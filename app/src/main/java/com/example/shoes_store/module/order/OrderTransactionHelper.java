package com.example.shoes_store.module.order;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.notification.NotificationRepository;
import com.example.shoes_store.module.payment.PaymentMapper;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.payment.PaymentService;
import com.example.shoes_store.module.product.ProductMapper;
import com.example.shoes_store.module.voucher.VoucherMapper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Helper thực thi Firestore Transaction hủy đơn hàng dùng chung cho Người dùng và Quản trị viên,
 * đảm bảo toàn bộ thao tác đọc hoàn tất trước khi ghi.
 */
public final class OrderTransactionHelper {

    @FunctionalInterface
    public interface StatusValidator {
        @Nullable
        String validate(@NonNull String currentStatusStr);
    }

    @FunctionalInterface
    public interface NotificationTextBuilder {
        @NonNull
        String build(@NonNull String orderCode);
    }

    public static final class CancelConfig {
        @NonNull
        final StatusValidator statusValidator;
        @Nullable
        final String fixedRecipientUid;
        @NonNull
        final NotificationTextBuilder notificationTitleBuilder;
        @NonNull
        final NotificationTextBuilder notificationMessageBuilder;
        @NonNull
        final String fallbackErrorMessage;

        public CancelConfig(
                @NonNull StatusValidator statusValidator,
                @Nullable String fixedRecipientUid,
                @NonNull NotificationTextBuilder notificationTitleBuilder,
                @NonNull NotificationTextBuilder notificationMessageBuilder,
                @NonNull String fallbackErrorMessage
        ) {
            this.statusValidator = statusValidator;
            this.fixedRecipientUid = fixedRecipientUid;
            this.notificationTitleBuilder = notificationTitleBuilder;
            this.notificationMessageBuilder = notificationMessageBuilder;
            this.fallbackErrorMessage = fallbackErrorMessage;
        }
    }

    private OrderTransactionHelper() {
    }

    public static void runCancelTransaction(
            @NonNull FirebaseFirestore db,
            @NonNull String orderId,
            @Nullable DocumentReference paymentRef,
            @NonNull CancelConfig config,
            @NonNull ResourceCallback<Void> callback
    ) {
        DocumentReference orderRef = db.collection(FirebaseProviderHelper.COL_ORDERS).document(orderId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // B1. ĐỌC toàn bộ trước khi ghi
            DocumentSnapshot orderSnap = transaction.get(orderRef);
            if (!orderSnap.exists()) {
                throw abort("Không tìm thấy đơn hàng");
            }
            String statusStr = FirestoreReadUtil.readString(
                    orderSnap, OrderMapper.FIELD_STATUS, OrderModel.Status.PENDING.name()
            );
            String statusError = config.statusValidator.validate(statusStr);
            if (statusError != null) {
                throw abort(statusError);
            }

            String orderCode = FirestoreReadUtil.readString(orderSnap, OrderMapper.FIELD_ORDER_CODE, orderId);
            String voucherCode = FirestoreReadUtil.readString(orderSnap, OrderMapper.FIELD_VOUCHER_CODE, "")
                    .trim().toUpperCase(Locale.ROOT);
            String recipientUid = config.fixedRecipientUid != null
                    ? config.fixedRecipientUid
                    : FirestoreReadUtil.readString(orderSnap, OrderMapper.FIELD_USER_ID, "");

            Map<String, DocumentReference> variantRefs = new LinkedHashMap<>();
            Map<String, Integer> restoreQtyByKey = new LinkedHashMap<>();
            for (OrderItemModel item : OrderMapper.parseItemsFromSnapshot(orderSnap)) {
                String productId = item.getProductId();
                String variantId = item.getVariantId();
                if (!productId.isEmpty() && !variantId.isEmpty()) {
                    String key = OrderService.buildVariantKey(productId, variantId);
                    variantRefs.put(key, variantDocRef(db, productId, variantId));
                    restoreQtyByKey.put(key, restoreQtyByKey.getOrDefault(key, 0) + item.getQuantity());
                }
            }

            Map<String, Integer> currentStockByKey = new LinkedHashMap<>();
            for (Map.Entry<String, DocumentReference> entry : variantRefs.entrySet()) {
                DocumentSnapshot vSnap = transaction.get(entry.getValue());
                if (vSnap.exists()) {
                    currentStockByKey.put(
                            entry.getKey(),
                            FirestoreReadUtil.readInt(vSnap, ProductMapper.FIELD_STOCK_QTY, 0)
                    );
                }
            }

            DocumentSnapshot paymentSnap = paymentRef != null ? transaction.get(paymentRef) : null;
            DocumentReference voucherRef = !voucherCode.isEmpty()
                    ? db.collection(FirebaseProviderHelper.COL_VOUCHERS).document(voucherCode)
                    : null;
            DocumentSnapshot voucherSnap = voucherRef != null ? transaction.get(voucherRef) : null;

            // B2. GHI đồng bộ (hủy đơn, hoàn kho, hoàn lượt voucher, cập nhật thanh toán, tạo thông báo)
            Timestamp now = Timestamp.now();
            transaction.update(
                    orderRef,
                    OrderMapper.FIELD_STATUS, OrderModel.Status.CANCELLED.name(),
                    OrderMapper.FIELD_UPDATED_AT, now
            );

            for (Map.Entry<String, Integer> stockEntry : currentStockByKey.entrySet()) {
                DocumentReference vRef = variantRefs.get(stockEntry.getKey());
                int addQty = restoreQtyByKey.getOrDefault(stockEntry.getKey(), 0);
                if (vRef != null && addQty > 0) {
                    transaction.update(
                            vRef,
                            ProductMapper.FIELD_STOCK_QTY,
                            OrderService.computeRestoredStock(stockEntry.getValue(), addQty)
                    );
                }
            }

            if (voucherSnap != null && voucherSnap.exists()) {
                long currentUsed = FirestoreReadUtil.readLong(voucherSnap, VoucherMapper.FIELD_USED_COUNT, 0L);
                transaction.update(
                        voucherSnap.getReference(),
                        VoucherMapper.FIELD_USED_COUNT,
                        OrderService.computeNextVoucherUsedCountOnCancel(currentUsed)
                );
            }

            if (paymentSnap != null && paymentSnap.exists()) {
                String currentPayStatus = FirestoreReadUtil.readString(
                        paymentSnap, PaymentMapper.FIELD_STATUS, PaymentModel.Status.UNPAID.name()
                );
                PaymentModel.Status nextPayStatus = PaymentService.resolveStatusOnOrderCancel(currentPayStatus);
                transaction.update(paymentSnap.getReference(), PaymentMapper.FIELD_STATUS, nextPayStatus.name());
            }

            if (!recipientUid.isEmpty()) {
                DocumentReference notifRef = db.collection(FirebaseProviderHelper.COL_USERS)
                        .document(recipientUid)
                        .collection(FirebaseProviderHelper.SUB_NOTIFICATIONS)
                        .document();
                transaction.set(notifRef, NotificationRepository.buildOrderNotificationData(
                        notifRef.getId(),
                        recipientUid,
                        config.notificationTitleBuilder.build(orderCode),
                        config.notificationMessageBuilder.build(orderCode),
                        orderCode,
                        now
                ));
            }

            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess(null))
          .addOnFailureListener(e -> callback.onError(
                  e.getMessage() != null ? e.getMessage() : config.fallbackErrorMessage
          ));
    }

    @NonNull
    static DocumentReference variantDocRef(
            @NonNull FirebaseFirestore db,
            @NonNull String productId,
            @NonNull String variantId
    ) {
        return db.collection(FirebaseProviderHelper.COL_PRODUCTS)
                .document(productId)
                .collection(FirebaseProviderHelper.SUB_VARIANTS)
                .document(variantId);
    }

    @NonNull
    static FirebaseFirestoreException abort(@NonNull String message) {
        return new FirebaseFirestoreException(message, FirebaseFirestoreException.Code.ABORTED);
    }
}
