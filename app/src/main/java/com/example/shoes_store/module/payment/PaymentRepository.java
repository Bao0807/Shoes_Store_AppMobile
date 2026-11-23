package com.example.shoes_store.module.payment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

/**
 * Repository sở hữu truy xuất collection `payments` trên Cloud Firestore.
 */
public class PaymentRepository {

    private static volatile PaymentRepository instance;

    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();

    private PaymentRepository() {
    }

    public static PaymentRepository getInstance() {
        if (instance == null) {
            synchronized (PaymentRepository.class) {
                if (instance == null) {
                    instance = new PaymentRepository();
                }
            }
        }
        return instance;
    }

    // ===== 1. ĐỌC =====

    public void findLatestPaymentRef(
            @NonNull String orderId,
            @NonNull String uid,
            @NonNull ResourceCallback<DocumentReference> callback
    ) {
        provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_PAYMENTS)
                .whereEqualTo(PaymentMapper.FIELD_ORDER_ID, orderId)
                .whereEqualTo(PaymentMapper.FIELD_USER_ID, uid)
                .get()
                .addOnSuccessListener(snap -> {
                    DocumentReference latestRef = null;
                    long newestMillis = -1L;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        long created = FirestoreReadUtil.readMillis(doc, PaymentMapper.FIELD_CREATED_AT);
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
                .whereEqualTo(PaymentMapper.FIELD_ORDER_ID, orderId)
                .whereEqualTo(PaymentMapper.FIELD_USER_ID, uid)
                .get()
                .addOnSuccessListener(snap -> {
                    PaymentModel latest = null;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        PaymentModel candidate = PaymentMapper.fromSnapshot(doc);
                        if (latest == null || candidate.getCreatedAtMillis() >= latest.getCreatedAtMillis()) {
                            latest = candidate;
                        }
                    }
                    callback.onSuccess(latest);
                })
                .addOnFailureListener(e -> callback.onError("Không thể tải chi tiết thanh toán"));
    }

    // ===== 2. HỖ TRỢ TRANSACTION ĐƠN HÀNG =====

    @NonNull
    public DocumentReference newPaymentDocumentRef(@NonNull FirebaseFirestore db) {
        return db.collection(FirebaseProviderHelper.COL_PAYMENTS).document();
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
        PaymentModel.Status status = PaymentService.resolveInitialStatus(method);
        String transactionCode = PaymentService.buildTransactionCode(status, orderCode);
        return PaymentMapper.toMap(paymentId, orderId, userId, method, status, amount, transactionCode, now);
    }

    @Nullable
    public static PaymentModel.Status resolveInitialStatus(@NonNull PaymentModel.Method method) {
        return PaymentService.resolveInitialStatus(method);
    }
}
