package com.example.shoes_store.module.admin;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.BaseFirestoreRepository;
import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.notification.NotificationRepository;
import com.example.shoes_store.module.order.OrderMapper;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.order.OrderTransactionHelper;
import com.example.shoes_store.module.payment.PaymentMapper;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.payment.PaymentRepository;
import com.example.shoes_store.module.payment.PaymentService;
import com.example.shoes_store.module.product.ProductMapper;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.voucher.VoucherMapper;
import com.example.shoes_store.module.voucher.VoucherModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Repository cho Kênh Quản trị (Admin): lắng nghe toàn bộ Đơn hàng, Sản phẩm (kể cả đang ẩn),
 * Voucher (kể cả đang tắt) và thực thi các Transaction duyệt đơn / Batch quản lý kho & ưu đãi.
 */
public class AdminRepository extends BaseFirestoreRepository {

    private static final String TAG = "AdminRepository";
    private static final String LISTENER_ADMIN_ORDERS = "admin_orders";
    private static final String LISTENER_ADMIN_PRODUCTS = "admin_products";
    private static final String LISTENER_ADMIN_VOUCHERS = "admin_vouchers";

    private final MutableLiveData<List<OrderModel>> allOrdersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ProductModel>> allProductsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<VoucherModel>> allVouchersLiveData = new MutableLiveData<>(new ArrayList<>());

    // ===== 1. REALTIME =====

    public void startListening() {
        stopListening();

        replaceUserListener(LISTENER_ADMIN_ORDERS, listenCollection(
                collection(FirebaseProviderHelper.COL_ORDERS),
                TAG,
                OrderMapper::fromSnapshot,
                (a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()),
                allOrdersLiveData::setValue
        ));

        replaceUserListener(LISTENER_ADMIN_PRODUCTS, listenCollection(
                collection(FirebaseProviderHelper.COL_PRODUCTS),
                TAG,
                ProductMapper::productFromSnapshot,
                (a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()),
                allProductsLiveData::setValue
        ));

        replaceUserListener(LISTENER_ADMIN_VOUCHERS, listenCollection(
                collection(FirebaseProviderHelper.COL_VOUCHERS),
                TAG,
                VoucherMapper::fromSnapshot,
                Comparator.comparingLong(VoucherModel::getMinOrderAmount),
                allVouchersLiveData::setValue
        ));
    }

    public void stopListening() {
        clearAllListeners();
    }

    // ===== 2. ĐỌC =====

    public LiveData<List<OrderModel>> getAllOrdersLiveData() {
        return allOrdersLiveData;
    }

    public LiveData<List<ProductModel>> getAllProductsLiveData() {
        return allProductsLiveData;
    }

    public LiveData<List<VoucherModel>> getAllVouchersLiveData() {
        return allVouchersLiveData;
    }

    // ===== 3. GHI / TRANSACTION QUẢN TRỊ =====

    public void updateOrderStatus(
            @NonNull String orderId,
            @NonNull OrderModel.Status targetStatus,
            @NonNull ResourceCallback<Void> callback
    ) {
        FirebaseFirestore db = provider.getFirestore();
        PaymentRepository.getInstance().findLatestPaymentRefForOrder(orderId, new ResourceCallback<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference latestPaymentRef) {
                dispatchOrderStatusUpdate(db, orderId, targetStatus, latestPaymentRef, callback);
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Find latest payment failed for order " + orderId + ": " + message);
                dispatchOrderStatusUpdate(db, orderId, targetStatus, null, callback);
            }
        });
    }

    public void saveProductWithVariants(
            @Nullable String existingProductId,
            @NonNull String name,
            @NonNull String brand,
            @NonNull String category,
            long price,
            long originalPrice,
            @NonNull String thumbnailUrl,
            @NonNull String weightGrams,
            @NonNull String heelDropMm,
            @NonNull String cushionLevel,
            @NonNull String surfaceType,
            @NonNull String description,
            @NonNull List<String> colorNames,
            @NonNull List<String> colorHexes,
            @NonNull List<String> sizes,
            @Nullable Integer stockOverride,
            boolean isActive,
            @NonNull ResourceCallback<Void> callback
    ) {
        AdminCatalogHelper.saveProductWithVariants(
                provider.getFirestore(),
                existingProductId, name, brand, category, price, originalPrice, thumbnailUrl,
                weightGrams, heelDropMm, cushionLevel, surfaceType, description,
                colorNames, colorHexes, sizes, stockOverride, isActive, callback
        );
    }

    public void toggleProductActive(
            @NonNull String productId,
            boolean isActive,
            @NonNull ResourceCallback<Void> callback
    ) {
        provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_PRODUCTS)
                .document(productId)
                .update(ProductMapper.FIELD_IS_ACTIVE, isActive)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError("Không thể cập nhật trạng thái sản phẩm"));
    }

    public void saveVoucher(
            @NonNull String code,
            @NonNull String shortBadge,
            @NonNull String title,
            @NonNull String description,
            long discountAmount,
            long minOrderAmount,
            int usageLimit,
            int validDays,
            boolean isActive,
            @NonNull ResourceCallback<Void> callback
    ) {
        AdminCatalogHelper.saveVoucher(
                provider.getFirestore(),
                code, shortBadge, title, description, discountAmount,
                minOrderAmount, usageLimit, validDays, isActive, callback
        );
    }

    public void toggleVoucherActive(
            @NonNull String code,
            boolean isActive,
            @NonNull ResourceCallback<Void> callback
    ) {
        provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_VOUCHERS)
                .document(code)
                .update(VoucherMapper.FIELD_IS_ACTIVE, isActive)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError("Không thể cập nhật trạng thái voucher"));
    }

    // ===== 4. PRIVATE HELPERS =====

    private void dispatchOrderStatusUpdate(
            @NonNull FirebaseFirestore db,
            @NonNull String orderId,
            @NonNull OrderModel.Status targetStatus,
            @Nullable DocumentReference paymentRef,
            @NonNull ResourceCallback<Void> callback
    ) {
        if (targetStatus == OrderModel.Status.CANCELLED) {
            OrderTransactionHelper.runCancelTransaction(db, orderId, paymentRef, buildAdminCancelConfig(), callback);
            return;
        }
        runAdminOrderStatusTransaction(db, orderId, targetStatus, paymentRef, callback);
    }

    @NonNull
    private static OrderTransactionHelper.CancelConfig buildAdminCancelConfig() {
        return new OrderTransactionHelper.CancelConfig(
                statusStr -> {
                    OrderModel.Status currentStatus = OrderModel.Status.fromStorage(statusStr);
                    if (!AdminService.canAdvanceOrderStatus(currentStatus, OrderModel.Status.CANCELLED)) {
                        return "Không thể chuyển trạng thái từ "
                                + currentStatus.getLabel()
                                + " sang "
                                + OrderModel.Status.CANCELLED.getLabel();
                    }
                    return null;
                },
                null,
                orderCode -> AdminService.buildOrderStatusNotificationTitle(orderCode, OrderModel.Status.CANCELLED),
                orderCode -> AdminService.buildOrderStatusNotificationMessage(OrderModel.Status.CANCELLED),
                "Không thể cập nhật trạng thái đơn hàng"
        );
    }

    private void runAdminOrderStatusTransaction(
            @NonNull FirebaseFirestore db,
            @NonNull String orderId,
            @NonNull OrderModel.Status targetStatus,
            @Nullable DocumentReference paymentRef,
            @NonNull ResourceCallback<Void> callback
    ) {
        DocumentReference orderRef = db.collection(FirebaseProviderHelper.COL_ORDERS).document(orderId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // B1. ĐỌC toàn bộ trước khi ghi
            DocumentSnapshot orderSnap = transaction.get(orderRef);
            if (!orderSnap.exists()) {
                throw new FirebaseFirestoreException("Không tìm thấy đơn hàng", FirebaseFirestoreException.Code.ABORTED);
            }
            OrderModel.Status currentStatus = OrderModel.Status.fromStorage(
                    FirestoreReadUtil.readString(orderSnap, OrderMapper.FIELD_STATUS, OrderModel.Status.PENDING.name())
            );
            if (!AdminService.canAdvanceOrderStatus(currentStatus, targetStatus)) {
                throw new FirebaseFirestoreException(
                        "Không thể chuyển trạng thái từ " + currentStatus.getLabel() + " sang " + targetStatus.getLabel(),
                        FirebaseFirestoreException.Code.ABORTED
                );
            }

            String customerUid = FirestoreReadUtil.readString(orderSnap, OrderMapper.FIELD_USER_ID, "");
            String orderCode = FirestoreReadUtil.readString(orderSnap, OrderMapper.FIELD_ORDER_CODE, orderId);
            DocumentSnapshot paymentSnap = paymentRef != null ? transaction.get(paymentRef) : null;

            // B2. GHI đồng bộ (SHIPPING / COMPLETED)
            Timestamp now = Timestamp.now();
            transaction.update(
                    orderRef,
                    OrderMapper.FIELD_STATUS, targetStatus.name(),
                    OrderMapper.FIELD_UPDATED_AT, now
            );

            if (targetStatus == OrderModel.Status.COMPLETED && paymentSnap != null && paymentSnap.exists()) {
                PaymentModel.Status paidStatus = AdminService.resolvePaymentStatusOnOrderComplete();
                String existingTxn = FirestoreReadUtil.readString(paymentSnap, PaymentMapper.FIELD_TRANSACTION_CODE, "");
                String nextTxn = existingTxn.isEmpty()
                        ? PaymentService.buildTransactionCode(paidStatus, orderCode)
                        : existingTxn;
                transaction.update(
                        paymentSnap.getReference(),
                        PaymentMapper.FIELD_STATUS, paidStatus.name(),
                        PaymentMapper.FIELD_PAID_AT, now,
                        PaymentMapper.FIELD_TRANSACTION_CODE, nextTxn
                );
            }

            if (!customerUid.isEmpty()) {
                DocumentReference notifRef = db.collection(FirebaseProviderHelper.COL_USERS)
                        .document(customerUid)
                        .collection(FirebaseProviderHelper.SUB_NOTIFICATIONS)
                        .document();
                transaction.set(notifRef, NotificationRepository.buildOrderNotificationData(
                        notifRef.getId(),
                        customerUid,
                        AdminService.buildOrderStatusNotificationTitle(orderCode, targetStatus),
                        AdminService.buildOrderStatusNotificationMessage(targetStatus),
                        orderCode,
                        now
                ));
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess(null))
          .addOnFailureListener(e -> callback.onError(
                  e.getMessage() != null ? e.getMessage() : "Không thể cập nhật trạng thái đơn hàng"
          ));
    }
}
