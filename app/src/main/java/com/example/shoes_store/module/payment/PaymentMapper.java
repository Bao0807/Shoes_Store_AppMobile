package com.example.shoes_store.module.payment;

import androidx.annotation.NonNull;

import com.example.shoes_store.core.FirestoreReadUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Chuyển đổi dữ liệu Firestore cho Giao dịch thanh toán (`payments`).
 */
public final class PaymentMapper {

    public static final String FIELD_PAYMENT_ID = "paymentId";
    public static final String FIELD_ORDER_ID = "orderId";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_METHOD = "method";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_AMOUNT = "amount";
    public static final String FIELD_TRANSACTION_CODE = "transactionCode";
    public static final String FIELD_PAID_AT = "paidAt";
    public static final String FIELD_CREATED_AT = "createdAt";

    private PaymentMapper() {
    }

    @NonNull
    public static PaymentModel fromSnapshot(@NonNull DocumentSnapshot doc) {
        return new PaymentModel(
                FirestoreReadUtil.readString(doc, FIELD_ORDER_ID, ""),
                PaymentModel.Status.fromStorage(FirestoreReadUtil.readString(doc, FIELD_STATUS, PaymentModel.Status.UNPAID.name())),
                FirestoreReadUtil.readString(doc, FIELD_TRANSACTION_CODE, ""),
                FirestoreReadUtil.readMillis(doc, FIELD_CREATED_AT)
        );
    }

    @NonNull
    public static Map<String, Object> toMap(
            @NonNull String paymentId,
            @NonNull String orderId,
            @NonNull String userId,
            @NonNull PaymentModel.Method method,
            @NonNull PaymentModel.Status status,
            long amount,
            @NonNull String transactionCode,
            @NonNull Timestamp now
    ) {
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put(FIELD_PAYMENT_ID, paymentId);
        paymentData.put(FIELD_ORDER_ID, orderId);
        paymentData.put(FIELD_USER_ID, userId);
        paymentData.put(FIELD_METHOD, method.name());
        paymentData.put(FIELD_STATUS, status.name());
        paymentData.put(FIELD_AMOUNT, amount);
        paymentData.put(FIELD_TRANSACTION_CODE, transactionCode);
        paymentData.put(FIELD_PAID_AT, status == PaymentModel.Status.PAID ? now : null);
        paymentData.put(FIELD_CREATED_AT, now);
        return paymentData;
    }
}
