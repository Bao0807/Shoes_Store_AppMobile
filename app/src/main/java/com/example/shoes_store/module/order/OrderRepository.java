package com.example.shoes_store.module.order;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.BaseFirestoreRepository;
import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.payment.PaymentRepository;
import com.example.shoes_store.module.profile.ProfileAddressModel;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository sở hữu Đơn hàng (`orders`): đồng bộ realtime, Transaction đặt/hủy đơn và mua lại có kiểm tồn kho.
 */
public class OrderRepository extends BaseFirestoreRepository {

    private static final String TAG = "OrderRepository";
    private static final String LISTENER_ORDERS = "orders";
    private static volatile OrderRepository instance;

    private final MutableLiveData<List<OrderModel>> ordersLiveData = new MutableLiveData<>(new ArrayList<>());

    private OrderRepository() {
    }

    public static OrderRepository getInstance() {
        if (instance == null) {
            synchronized (OrderRepository.class) {
                if (instance == null) {
                    instance = new OrderRepository();
                }
            }
        }
        return instance;
    }

    @Override
    public void onSessionStart(@NonNull String uid, @NonNull FirebaseUser authUser) {
        startUserListening(uid);
    }

    @Override
    public void onSessionEnd() {
        stopUserListening();
    }

    // ===== 1. REALTIME =====

    public void startUserListening(@NonNull String uid) {
        stopUserListening();
        replaceUserListener(LISTENER_ORDERS, listenCollection(
                collection(FirebaseProviderHelper.COL_ORDERS)
                        .whereEqualTo(OrderMapper.FIELD_USER_ID, uid),
                TAG,
                OrderMapper::fromSnapshot,
                (a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()),
                ordersLiveData::setValue
        ));
    }

    public void stopUserListening() {
        clearUserListeners();
        ordersLiveData.setValue(new ArrayList<>());
    }

    // ===== 2. ĐỌC =====

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return ordersLiveData;
    }

    @Nullable
    public OrderModel getOrderById(@Nullable String orderId) {
        if (orderId == null) {
            return null;
        }
        List<OrderModel> list = ordersLiveData.getValue();
        if (list == null) {
            return null;
        }
        for (OrderModel order : list) {
            if (orderId.equals(order.getOrderId())) {
                return order;
            }
        }
        return null;
    }

    @Nullable
    public OrderModel getOrderByCode(@Nullable String orderCode) {
        if (orderCode == null) {
            return null;
        }
        List<OrderModel> list = ordersLiveData.getValue();
        if (list == null) {
            return null;
        }
        for (OrderModel order : list) {
            if (orderCode.equals(order.getOrderCode()) || orderCode.equals(order.getOrderId())) {
                return order;
            }
        }
        return null;
    }

    @Nullable
    public OrderModel getOrderByIdOrCode(@Nullable String orderIdOrCode) {
        if (orderIdOrCode == null) {
            return null;
        }
        OrderModel byId = getOrderById(orderIdOrCode);
        return byId != null ? byId : getOrderByCode(orderIdOrCode);
    }

    // ===== 3. GHI / TRANSACTION =====

    public void placeOrder(
            @NonNull List<CartItemModel> items,
            @NonNull ProfileAddressModel address,
            @NonNull String shippingMethod,
            long shippingFee,
            @NonNull PaymentModel.Method paymentMethod,
            @Nullable String voucherCode,
            long requestedDiscount,
            boolean removeFromCart,
            @NonNull ResourceCallback<OrderModel> callback
    ) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            callback.onError("Vui lòng đăng nhập để đặt hàng");
            return;
        }
        if (items.isEmpty()) {
            callback.onError("Không có sản phẩm để thanh toán");
            return;
        }

        OrderReorderHelper.ensureItemVariantIds(
                provider.getFirestore(),
                items,
                () -> OrderPlaceHelper.executePlaceOrderTransaction(
                        provider.getFirestore(),
                        uid,
                        items,
                        address,
                        shippingMethod,
                        shippingFee,
                        paymentMethod,
                        voucherCode,
                        requestedDiscount,
                        removeFromCart,
                        callback
                )
        );
    }

    public void cancelOrder(@NonNull String orderId, @NonNull ResourceCallback<Void> callback) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            callback.onError("Vui lòng đăng nhập");
            return;
        }
        FirebaseFirestore db = provider.getFirestore();

        // Truy vấn Payment Reference qua PaymentRepository trước khi vào Transaction (Firestore không hỗ trợ Query trong Transaction)
        PaymentRepository.getInstance().findLatestPaymentRef(orderId, uid, new ResourceCallback<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference latestPaymentRef) {
                runCancelOrderTransaction(db, uid, orderId, latestPaymentRef, callback);
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Find latest payment failed for order " + orderId + ": " + message);
                runCancelOrderTransaction(db, uid, orderId, null, callback);
            }
        });
    }

    public void reorder(@NonNull OrderModel order, @NonNull ResourceCallback<Integer> callback) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            callback.onError("Vui lòng đăng nhập");
            return;
        }
        OrderReorderHelper.reorder(provider.getFirestore(), uid, order, callback);
    }

    // ===== 4. PRIVATE HELPERS =====

    private void runCancelOrderTransaction(
            @NonNull FirebaseFirestore db,
            @NonNull String uid,
            @NonNull String orderId,
            @Nullable DocumentReference paymentRef,
            @NonNull ResourceCallback<Void> callback
    ) {
        OrderTransactionHelper.CancelConfig config = new OrderTransactionHelper.CancelConfig(
                statusStr -> OrderService.canCancelStatus(statusStr)
                        ? null
                        : "Chỉ có thể hủy đơn đang chờ xác nhận",
                uid,
                orderCode -> "Đơn #" + orderCode + " đã được hủy",
                orderCode -> "Số lượng sản phẩm đã được hoàn lại vào kho của cửa hàng.",
                "Không thể hủy đơn hàng"
        );
        OrderTransactionHelper.runCancelTransaction(db, orderId, paymentRef, config, callback);
    }
}
