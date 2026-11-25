package com.example.shoes_store.module.review;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.BaseFirestoreRepository;
import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.order.OrderMapper;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.product.ProductMapper;
import com.example.shoes_store.module.profile.ProfileMapper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Repository sở hữu đồng bộ realtime và thao tác ghi cho Đánh giá sản phẩm (`products/{productId}/reviews`).
 */
public class ReviewRepository extends BaseFirestoreRepository {

    private static final String TAG = "ReviewRepository";
    private static volatile ReviewRepository instance;

    private ReviewRepository() {
    }

    public static ReviewRepository getInstance() {
        if (instance == null) {
            synchronized (ReviewRepository.class) {
                if (instance == null) {
                    instance = new ReviewRepository();
                }
            }
        }
        return instance;
    }

    public ListenerRegistration observeReviews(
            @NonNull String productId,
            @NonNull MutableLiveData<List<ReviewModel>> target
    ) {
        return listenCollection(
                collection(FirebaseProviderHelper.COL_PRODUCTS)
                        .document(productId)
                        .collection(FirebaseProviderHelper.SUB_REVIEWS),
                TAG,
                doc -> {
                    ReviewModel review = ReviewMapper.fromSnapshot(doc, productId);
                    return ReviewService.isVerifiedRealReview(review) ? review : null;
                },
                (a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()),
                target::setValue
        );
    }

    public void loadReviewedProductIdsForOrder(
            @NonNull String orderId,
            @NonNull List<String> productIds,
            @NonNull ResourceCallback<Set<String>> callback
    ) {
        String uid = provider.getCurrentUid();
        String cleanOrderId = orderId.trim();
        if (uid == null || cleanOrderId.isEmpty() || productIds.isEmpty()) {
            callback.onSuccess(new HashSet<>());
            return;
        }

        Set<String> distinctIds = new LinkedHashSet<>();
        for (String pid : productIds) {
            if (pid != null && !pid.trim().isEmpty()) {
                distinctIds.add(pid.trim());
            }
        }
        if (distinctIds.isEmpty()) {
            callback.onSuccess(new HashSet<>());
            return;
        }

        FirebaseFirestore db = provider.getFirestore();
        List<String> orderedPids = new ArrayList<>(distinctIds);
        List<Task<QuerySnapshot>> tasks = new ArrayList<>(orderedPids.size());
        for (String pid : orderedPids) {
            tasks.add(db.collection(FirebaseProviderHelper.COL_PRODUCTS)
                    .document(pid)
                    .collection(FirebaseProviderHelper.SUB_REVIEWS)
                    .whereEqualTo(ReviewMapper.FIELD_USER_ID, uid)
                    .whereEqualTo(ReviewMapper.FIELD_ORDER_ID, cleanOrderId)
                    .limit(1)
                    .get());
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(all -> {
            Set<String> reviewedIds = new HashSet<>();
            for (int i = 0; i < orderedPids.size(); i++) {
                Task<QuerySnapshot> task = tasks.get(i);
                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    reviewedIds.add(orderedPids.get(i));
                } else if (!task.isSuccessful()) {
                    Log.w(TAG, "Check review status failed for product " + orderedPids.get(i) + ": "
                            + (task.getException() != null ? task.getException().getMessage() : "unknown"));
                }
            }
            callback.onSuccess(reviewedIds);
        });
    }

    public void submitReview(
            @NonNull String productId,
            @NonNull String orderId,
            float rating,
            @NonNull String comment,
            @NonNull ResourceCallback<Void> callback
    ) {
        String uid = provider.getCurrentUid();
        String validationError = ReviewService.validateReviewInput(uid, orderId, rating, comment);
        if (validationError != null) {
            callback.onError(validationError);
            return;
        }
        String cleanOrderId = orderId.trim();
        String cleanComment = comment.trim();

        FirebaseFirestore db = provider.getFirestore();
        DocumentReference userRef = db.collection(FirebaseProviderHelper.COL_USERS).document(uid);
        DocumentReference orderRef = db.collection(FirebaseProviderHelper.COL_ORDERS).document(cleanOrderId);
        DocumentReference productRef = db.collection(FirebaseProviderHelper.COL_PRODUCTS).document(productId);

        Task<DocumentSnapshot> userTask = userRef.get();
        Task<DocumentSnapshot> orderTask = orderRef.get();
        Task<QuerySnapshot> dupTask = productRef.collection(FirebaseProviderHelper.SUB_REVIEWS)
                .whereEqualTo(ReviewMapper.FIELD_USER_ID, uid)
                .whereEqualTo(ReviewMapper.FIELD_ORDER_ID, cleanOrderId)
                .limit(1)
                .get();

        Tasks.whenAllComplete(userTask, orderTask, dupTask)
                .addOnCompleteListener(all -> {
                    if (!userTask.isSuccessful() || !orderTask.isSuccessful() || !dupTask.isSuccessful()) {
                        callback.onError("Không thể xác minh thông tin mua hàng, vui lòng thử lại");
                        return;
                    }

                    DocumentSnapshot userSnap = userTask.getResult();
                    if (userSnap == null || !userSnap.exists()) {
                        callback.onError("Tài khoản chưa tồn tại trong cơ sở dữ liệu");
                        return;
                    }

                    DocumentSnapshot orderSnap = orderTask.getResult();
                    if (orderSnap == null || !orderSnap.exists()) {
                        callback.onError("Không tìm thấy đơn hàng hợp lệ");
                        return;
                    }
                    String orderUserId = FirestoreReadUtil.readString(orderSnap, OrderMapper.FIELD_USER_ID, "");
                    String statusStr = FirestoreReadUtil.readString(orderSnap, OrderMapper.FIELD_STATUS, "");
                    if (!uid.equals(orderUserId) || !OrderModel.Status.COMPLETED.name().equalsIgnoreCase(statusStr)) {
                        callback.onError("Chỉ có thể đánh giá sau khi đơn hàng đã hoàn thành");
                        return;
                    }

                    if (!OrderMapper.snapshotContainsProduct(orderSnap, productId)) {
                        callback.onError("Đơn hàng này không chứa sản phẩm đang xem");
                        return;
                    }

                    QuerySnapshot dupSnap = dupTask.getResult();
                    if (dupSnap != null && !dupSnap.isEmpty()) {
                        callback.onError("Bạn đã đánh giá sản phẩm này cho đơn hàng đó rồi");
                        return;
                    }

                    String reviewerName = FirestoreReadUtil.readString(userSnap, ProfileMapper.FIELD_FULL_NAME, "Runner").trim();
                    if (reviewerName.isEmpty()) {
                        reviewerName = "Runner";
                    }
                    String reviewerAvatarUrl = FirestoreReadUtil.readString(userSnap, ProfileMapper.FIELD_AVATAR_URL, "");

                    String reviewId = ReviewService.buildReviewDocumentId(cleanOrderId, uid);
                    DocumentReference reviewRef = productRef.collection(FirebaseProviderHelper.SUB_REVIEWS).document(reviewId);
                    Map<String, Object> data = ReviewMapper.toMap(
                            reviewId,
                            productId,
                            uid,
                            cleanOrderId,
                            reviewerName,
                            reviewerAvatarUrl,
                            rating,
                            cleanComment,
                            Timestamp.now()
                    );

                    reviewRef.set(data)
                            .addOnSuccessListener(unused -> {
                                syncProductRatingStats(productRef);
                                callback.onSuccess(null);
                            })
                            .addOnFailureListener(e -> callback.onError(
                                    e.getMessage() != null ? e.getMessage() : "Không thể gửi đánh giá"
                            ));
                });
    }

    private void syncProductRatingStats(@NonNull DocumentReference productRef) {
        String productId = productRef.getId();
        productRef.collection(FirebaseProviderHelper.SUB_REVIEWS)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Double> ratings = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ReviewModel review = ReviewMapper.fromSnapshot(doc, productId);
                        if (ReviewService.isVerifiedRealReview(review)) {
                            ratings.add((double) review.getRating());
                        }
                    }
                    double avg = ReviewService.calculateAverageRating(ratings);
                    productRef.update(ProductMapper.FIELD_RATING_AVG, avg, ProductMapper.FIELD_REVIEW_COUNT, ratings.size())
                            .addOnFailureListener(e -> Log.w(TAG, "Product rating sync skipped: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.w(TAG, "Load reviews for rating sync failed: " + e.getMessage()));
    }
}
