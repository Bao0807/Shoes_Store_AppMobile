package com.example.shoes_store.module.payment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreStoreHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository sở hữu toàn bộ nghiệp vụ thanh toán và truy xuất collection `payments` trên Firestore.
 */
public class PaymentRepository {

    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();

    @NonNull
    public static PaymentModel.Status resolveInitialStatus(@NonNull PaymentModel.Method method) {
        return method == PaymentModel.Method.COD
                ? PaymentModel.Status.UNPAID
                : PaymentModel.Status.PAID;
    }

    @NonNull
    public static PaymentModel.Status resolveStatusOnOrderCancel(@Nullable String currentStatus) {
        return PaymentModel.Status.PAID.name().equalsIgnoreCase(currentStatus)
                ? PaymentModel.Status.REFUNDED
                : PaymentModel.Status.FAILED;
    }

    @NonNull
    public static String buildTransactionCode(@NonNull PaymentModel.Status status, @NonNull String orderCode) {
        return status == PaymentModel.Status.PAID ? "TXN-" + orderCode : "";
    }

    @NonNull
    public static Map<String, Object> buildPaymentData(
            @NonNull String paymentId,
            @NonNull String orderId,
            @NonNull String userId,
            @NonNull PaymentModel.Method method,
            long amount,
            @NonNull String orderCode,
            @NonNull Timestamp now
    ) {
        PaymentModel.Status payStatus = resolveInitialStatus(method);
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("orderId", orderId);
        paymentData.put("userId", userId);
        paymentData.put("method", method.name());
        paymentData.put("status", payStatus.name());
        paymentData.put("amount", amount);
        paymentData.put("transactionCode", buildTransactionCode(payStatus, orderCode));
        paymentData.put("paidAt", payStatus == PaymentModel.Status.PAID ? now : null);
        paymentData.put("createdAt", now);
        return paymentData;
    }

    @NonNull
    public static PaymentModel mapPayment(@NonNull DocumentSnapshot doc) {
        return new PaymentModel(
                FirestoreStoreHelper.readString(doc, "paymentId", doc.getId()),
                FirestoreStoreHelper.readString(doc, "orderId", ""),
                FirestoreStoreHelper.readString(doc, "userId", ""),
                PaymentModel.Method.fromStorage(FirestoreStoreHelper.readString(doc, "method", "COD")),
                PaymentModel.Status.fromStorage(FirestoreStoreHelper.readString(doc, "status", "UNPAID")),
                FirestoreStoreHelper.readLong(doc, "amount", 0L),
                FirestoreStoreHelper.readString(doc, "transactionCode", ""),
                FirestoreStoreHelper.readMillis(doc, "paidAt"),
                FirestoreStoreHelper.readMillis(doc, "createdAt")
        );
    }

    @NonNull
    public DocumentReference newPaymentDocumentRef(@NonNull FirebaseFirestore db) {
        return db.collection(FirebaseProviderHelper.COL_PAYMENTS).document();
    }

    public void findLatestPaymentRef(
            @NonNull String orderId,
            @NonNull String uid,
            @NonNull ResourceCallback<DocumentReference> callback
    ) {
        provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_PAYMENTS)
                .whereEqualTo("orderId", orderId)
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    DocumentReference latestRef = null;
                    long newestMillis = -1L;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        long created = FirestoreStoreHelper.readMillis(doc, "createdAt");
                        if (latestRef == null || created >= newestMillis) {
                            newestMillis = created;
                            latestRef = doc.getReference();
                        }
                    }
                    callback.onSuccess(latestRef);
                })
                .addOnFailureListener(e -> callback.onError("Không thể tìm giao dịch thanh toán"));
    }

    public void loadLatestPayment(@NonNull String orderId, @NonNull ResourceCallback<PaymentModel> callback) {
        String uid = provider.getCurrentUid();
        if (uid == null || orderId.isEmpty()) {
            callback.onSuccess(null);
            return;
        }
        provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_PAYMENTS)
                .whereEqualTo("orderId", orderId)
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    PaymentModel latest = null;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        PaymentModel candidate = mapPayment(doc);
                        if (latest == null || candidate.getCreatedAtMillis() >= latest.getCreatedAtMillis()) {
                            latest = candidate;
                        }
                    }
                    callback.onSuccess(latest);
                })
                .addOnFailureListener(e -> callback.onError("Không thể tải chi tiết thanh toán"));
    }
}
