package com.example.shoes_store.module.order;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.notification.NotificationRepository;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.payment.PaymentRepository;
import com.example.shoes_store.module.product.ProductMapper;
import com.example.shoes_store.module.profile.ProfileAddressModel;
import com.example.shoes_store.module.voucher.VoucherMapper;
import com.example.shoes_store.module.voucher.VoucherService;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Helper thực thi Firestore Transaction đặt hàng: đọc toàn bộ tồn kho/voucher trước khi ghi nguyên tử.
 */
public final class OrderPlaceHelper {

    private OrderPlaceHelper() {
    }

    static void executePlaceOrderTransaction(
            @NonNull FirebaseFirestore db,
            @NonNull String uid,
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
        String cleanVoucher = voucherCode != null ? voucherCode.trim().toUpperCase(Locale.ROOT) : "";

        db.runTransaction(transaction -> {
            PlaceOrderReadState readState = readPlaceOrderState(db, transaction, items, cleanVoucher);
            ValidatedVoucher validatedVoucher = validateAndComputeDiscount(readState, cleanVoucher, requestedDiscount);
            return writePlaceOrderAtomically(
                    db,
                    transaction,
                    uid,
                    items,
                    address,
                    shippingMethod,
                    shippingFee,
                    paymentMethod,
                    cleanVoucher,
                    removeFromCart,
                    readState,
                    validatedVoucher
            );
        }).addOnSuccessListener(callback::onSuccess)
          .addOnFailureListener(e -> callback.onError(
                  e.getMessage() != null ? e.getMessage() : "Không thể đặt hàng, vui lòng thử lại"
          ));
    }

    @NonNull
    private static PlaceOrderReadState readPlaceOrderState(
            @NonNull FirebaseFirestore db,
            @NonNull Transaction transaction,
            @NonNull List<CartItemModel> items,
            @NonNull String cleanVoucher
    ) throws FirebaseFirestoreException {
        Map<String, DocumentReference> variantRefs = new LinkedHashMap<>();
        Map<String, String> itemLabelByKey = new LinkedHashMap<>();
        List<OrderItemModel> orderItems = new ArrayList<>();
        long subtotal = OrderService.calculateSubtotal(items);

        for (CartItemModel item : items) {
            if (item.getVariantId().isEmpty()) {
                throw OrderTransactionHelper.abort(
                        "Không xác định được biến thể cho " + item.getProduct().getName() + " (EU " + item.getSize() + ")"
                );
            }
            String key = OrderService.buildVariantKey(item.getProduct().getId(), item.getVariantId());
            if (!variantRefs.containsKey(key)) {
                variantRefs.put(
                        key,
                        OrderTransactionHelper.variantDocRef(db, item.getProduct().getId(), item.getVariantId())
                );
                itemLabelByKey.put(key, item.getProduct().getName() + " (EU " + item.getSize() + ")");
            }
            orderItems.add(OrderItemModel.fromCartItem(item));
        }

        Map<String, Integer> requestedQtyByKey = OrderService.aggregateRequestedQuantities(items);
        Map<String, Integer> currentStockByKey = new LinkedHashMap<>();
        for (Map.Entry<String, DocumentReference> entry : variantRefs.entrySet()) {
            DocumentSnapshot snap = transaction.get(entry.getValue());
            if (snap.exists()) {
                currentStockByKey.put(entry.getKey(), FirestoreReadUtil.readInt(snap, ProductMapper.FIELD_STOCK_QTY, 0));
            }
        }

        DocumentReference voucherRef = null;
        DocumentSnapshot voucherSnap = null;
        if (!cleanVoucher.isEmpty()) {
            voucherRef = db.collection(FirebaseProviderHelper.COL_VOUCHERS).document(cleanVoucher);
            voucherSnap = transaction.get(voucherRef);
        }

        return new PlaceOrderReadState(
                variantRefs,
                itemLabelByKey,
                orderItems,
                requestedQtyByKey,
                currentStockByKey,
                subtotal,
                voucherRef,
                voucherSnap
        );
    }

    @NonNull
    private static ValidatedVoucher validateAndComputeDiscount(
            @NonNull PlaceOrderReadState state,
            @NonNull String cleanVoucher,
            long requestedDiscount
    ) throws FirebaseFirestoreException {
        String stockError = OrderService.validateStockSufficiency(
                state.requestedQtyByKey,
                state.currentStockByKey,
                state.itemLabelByKey
        );
        if (stockError != null) {
            throw OrderTransactionHelper.abort(stockError);
        }

        if (state.voucherRef == null) {
            return new ValidatedVoucher(0L, 0L);
        }
        if (state.voucherSnap == null || !state.voucherSnap.exists()) {
            throw OrderTransactionHelper.abort("Mã giảm giá " + cleanVoucher + " không tồn tại");
        }

        boolean active = FirestoreReadUtil.readBoolean(state.voucherSnap, VoucherMapper.FIELD_IS_ACTIVE, false);
        long minOrder = FirestoreReadUtil.readLong(state.voucherSnap, VoucherMapper.FIELD_MIN_ORDER_AMOUNT, 0L);
        long voucherDiscount = FirestoreReadUtil.readLong(
                state.voucherSnap, VoucherMapper.FIELD_DISCOUNT_AMOUNT, requestedDiscount
        );
        long usageLimit = FirestoreReadUtil.readLong(state.voucherSnap, VoucherMapper.FIELD_USAGE_LIMIT, 0L);
        long usedCount = FirestoreReadUtil.readLong(state.voucherSnap, VoucherMapper.FIELD_USED_COUNT, 0L);
        long expiryMillis = FirestoreReadUtil.readMillis(state.voucherSnap, VoucherMapper.FIELD_EXPIRY_DATE);

        String voucherError = VoucherService.validateVoucherSnapshotForTransaction(
                active, expiryMillis, System.currentTimeMillis(), minOrder, usageLimit, usedCount, state.subtotal, cleanVoucher
        );
        if (voucherError != null) {
            throw OrderTransactionHelper.abort(voucherError);
        }

        return new ValidatedVoucher(Math.min(voucherDiscount, state.subtotal), usedCount + 1L);
    }

    @NonNull
    private static OrderModel writePlaceOrderAtomically(
            @NonNull FirebaseFirestore db,
            @NonNull Transaction transaction,
            @NonNull String uid,
            @NonNull List<CartItemModel> items,
            @NonNull ProfileAddressModel address,
            @NonNull String shippingMethod,
            long shippingFee,
            @NonNull PaymentModel.Method paymentMethod,
            @NonNull String cleanVoucher,
            boolean removeFromCart,
            @NonNull PlaceOrderReadState state,
            @NonNull ValidatedVoucher voucher
    ) {
        Timestamp now = Timestamp.now();
        long nowMillis = now.toDate().getTime();

        for (Map.Entry<String, DocumentReference> entry : state.variantRefs.entrySet()) {
            int currentStock = state.currentStockByKey.getOrDefault(entry.getKey(), 0);
            int needed = state.requestedQtyByKey.getOrDefault(entry.getKey(), 0);
            transaction.update(entry.getValue(), ProductMapper.FIELD_STOCK_QTY, Math.max(0, currentStock - needed));
        }
        if (state.voucherRef != null) {
            transaction.update(state.voucherRef, VoucherMapper.FIELD_USED_COUNT, voucher.nextUsedCount);
        }

        long total = OrderService.calculateTotal(state.subtotal, shippingFee, voucher.effectiveDiscount);
        DocumentReference orderRef = db.collection(FirebaseProviderHelper.COL_ORDERS).document();
        String orderId = orderRef.getId();
        String orderCode = OrderService.generateOrderCode(nowMillis);

        transaction.set(orderRef, OrderMapper.orderToMap(
                orderId, orderCode, uid, paymentMethod, address, shippingMethod, cleanVoucher,
                state.orderItems, state.subtotal, shippingFee, voucher.effectiveDiscount, total, now
        ));

        DocumentReference paymentRef = PaymentRepository.getInstance().newPaymentDocumentRef(db);
        transaction.set(paymentRef, PaymentRepository.buildPaymentData(
                paymentRef.getId(), orderId, uid, paymentMethod, total, orderCode, now
        ));

        if (removeFromCart) {
            CollectionReference cartRef = db.collection(FirebaseProviderHelper.COL_USERS)
                    .document(uid)
                    .collection(FirebaseProviderHelper.SUB_CART);
            for (CartItemModel item : items) {
                if (!item.getId().isEmpty() && !item.getId().startsWith("direct_")) {
                    transaction.delete(cartRef.document(item.getId()));
                }
            }
        }

        DocumentReference notifRef = db.collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_NOTIFICATIONS)
                .document();
        transaction.set(notifRef, NotificationRepository.buildOrderNotificationData(
                notifRef.getId(),
                uid,
                "Đặt hàng #" + orderCode + " thành công",
                "Đơn hàng đang chờ xác nhận (" + paymentMethod.getLabel() + ").",
                orderCode,
                now
        ));

        return new OrderModel(
                orderId,
                orderCode,
                uid,
                OrderModel.Status.PENDING,
                paymentMethod,
                state.orderItems,
                address.getAddressDetail(),
                address.getReceiverName() + " · " + address.getPhone(),
                shippingMethod,
                cleanVoucher,
                state.subtotal,
                shippingFee,
                voucher.effectiveDiscount,
                total,
                nowMillis
        );
    }

    private static final class PlaceOrderReadState {
        final Map<String, DocumentReference> variantRefs;
        final Map<String, String> itemLabelByKey;
        final List<OrderItemModel> orderItems;
        final Map<String, Integer> requestedQtyByKey;
        final Map<String, Integer> currentStockByKey;
        final long subtotal;
        @Nullable
        final DocumentReference voucherRef;
        @Nullable
        final DocumentSnapshot voucherSnap;

        PlaceOrderReadState(
                @NonNull Map<String, DocumentReference> variantRefs,
                @NonNull Map<String, String> itemLabelByKey,
                @NonNull List<OrderItemModel> orderItems,
                @NonNull Map<String, Integer> requestedQtyByKey,
                @NonNull Map<String, Integer> currentStockByKey,
                long subtotal,
                @Nullable DocumentReference voucherRef,
                @Nullable DocumentSnapshot voucherSnap
        ) {
            this.variantRefs = variantRefs;
            this.itemLabelByKey = itemLabelByKey;
            this.orderItems = orderItems;
            this.requestedQtyByKey = requestedQtyByKey;
            this.currentStockByKey = currentStockByKey;
            this.subtotal = subtotal;
            this.voucherRef = voucherRef;
            this.voucherSnap = voucherSnap;
        }
    }

    private static final class ValidatedVoucher {
        final long effectiveDiscount;
        final long nextUsedCount;

        ValidatedVoucher(long effectiveDiscount, long nextUsedCount) {
            this.effectiveDiscount = effectiveDiscount;
            this.nextUsedCount = nextUsedCount;
        }
    }
}
