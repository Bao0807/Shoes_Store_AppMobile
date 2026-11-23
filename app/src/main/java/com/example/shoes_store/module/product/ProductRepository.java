package com.example.shoes_store.module.product;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreStoreHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.order.OrderModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Repository cho màn Chi tiết sản phẩm (lắng nghe realtime variants tồn kho, reviews từ người mua thực).
 */
public class ProductRepository {

    private static final String TAG = "ProductRepository";

    private final FirestoreStoreHelper store = FirestoreStoreHelper.getInstance();
    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();

    @Nullable
    public ProductModel getProductById(@Nullable String productId) {
        return store.getProductById(productId);
    }

    public LiveData<List<ProductModel>> getProductsLiveData() {
        return store.getProductsLiveData();
    }

    public LiveData<Set<String>> getFavoriteIdsLiveData() {
        return store.getFavoriteIdsLiveData();
    }

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return store.getOrdersLiveData();
    }

    public ListenerRegistration observeVariants(
            @NonNull String productId,
            @NonNull MutableLiveData<List<ProductVariantModel>> target
    ) {
        return provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_PRODUCTS)
                .document(productId)
                .collection(FirebaseProviderHelper.SUB_VARIANTS)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Variants listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<ProductVariantModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        list.add(FirestoreStoreHelper.mapVariant(doc, productId));
                    }
                    Collections.sort(list, (a, b) -> {
                        int c = a.getVariantId().compareTo(b.getVariantId());
                        return c != 0 ? c : a.getSize().compareTo(b.getSize());
                    });
                    target.setValue(list);
                });
    }

    public ListenerRegistration observeReviews(
            @NonNull String productId,
            @NonNull MutableLiveData<List<ProductReviewModel>> target
    ) {
        return provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_PRODUCTS)
                .document(productId)
                .collection(FirebaseProviderHelper.SUB_REVIEWS)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Reviews listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<ProductReviewModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ProductReviewModel review = FirestoreStoreHelper.mapReview(doc, productId);
                        if (ProductReviewHelper.isVerifiedRealReview(review)) {
                            list.add(review);
                        }
                    }
                    Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()));
                    target.setValue(list);
                });
    }

    @SuppressWarnings("unchecked")
    public void submitReview(
            @NonNull String productId,
            @NonNull String orderId,
            float rating,
            @NonNull String comment,
            @NonNull ResourceCallback<Void> callback
    ) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            callback.onError("Vui lòng đăng nhập để gửi đánh giá");
            return;
        }
        String cleanOrderId = orderId.trim();
        if (cleanOrderId.isEmpty()) {
            callback.onError("Chỉ khách hàng có đơn hàng hoàn tất mới được đánh giá");
            return;
        }
        if (rating < 1.0f || rating > 5.0f) {
            callback.onError("Vui lòng chọn số sao từ 1 đến 5");
            return;
        }
        String cleanComment = comment.trim();
        if (cleanComment.isEmpty()) {
            callback.onError("Vui lòng nhập nội dung đánh giá");
            return;
        }

        FirebaseFirestore db = provider.getFirestore();
        DocumentReference userRef = db.collection(FirebaseProviderHelper.COL_USERS).document(uid);
        DocumentReference orderRef = db.collection(FirebaseProviderHelper.COL_ORDERS).document(cleanOrderId);
        DocumentReference productRef = db.collection(FirebaseProviderHelper.COL_PRODUCTS).document(productId);

        Task<DocumentSnapshot> userTask = userRef.get();
        Task<DocumentSnapshot> orderTask = orderRef.get();
        Task<QuerySnapshot> dupTask = productRef.collection(FirebaseProviderHelper.SUB_REVIEWS)
                .whereEqualTo("userId", uid)
                .whereEqualTo("orderId", cleanOrderId)
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
                    String orderUserId = FirestoreStoreHelper.readString(orderSnap, "userId", "");
                    String statusStr = FirestoreStoreHelper.readString(orderSnap, "status", "");
                    if (!uid.equals(orderUserId) || !OrderModel.Status.COMPLETED.name().equalsIgnoreCase(statusStr)) {
                        callback.onError("Chỉ có thể đánh giá sau khi đơn hàng đã hoàn thành");
                        return;
                    }

                    boolean containsProduct = false;
                    Object rawItems = orderSnap.get("items");
                    if (rawItems instanceof List<?>) {
                        for (Object obj : (List<?>) rawItems) {
                            if (obj instanceof Map<?, ?>) {
                                Map<String, Object> m = (Map<String, Object>) obj;
                                Object pidObj = m.get("productId");
                                if (pidObj != null && productId.equals(String.valueOf(pidObj))) {
                                    containsProduct = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!containsProduct) {
                        callback.onError("Đơn hàng này không chứa sản phẩm đang xem");
                        return;
                    }

                    QuerySnapshot dupSnap = dupTask.getResult();
                    if (dupSnap != null && !dupSnap.isEmpty()) {
                        callback.onError("Bạn đã đánh giá sản phẩm này cho đơn hàng đó rồi");
                        return;
                    }

                    String reviewerName = FirestoreStoreHelper.readString(userSnap, "fullName", "Runner").trim();
                    if (reviewerName.isEmpty()) {
                        reviewerName = "Runner";
                    }
                    String reviewerAvatarUrl = FirestoreStoreHelper.readString(userSnap, "avatarUrl", "");

                    DocumentReference reviewRef = productRef.collection(FirebaseProviderHelper.SUB_REVIEWS).document();
                    Map<String, Object> data = new HashMap<>();
                    data.put("reviewId", reviewRef.getId());
                    data.put("productId", productId);
                    data.put("userId", uid);
                    data.put("orderId", cleanOrderId);
                    data.put("reviewerName", reviewerName);
                    data.put("reviewerAvatarUrl", reviewerAvatarUrl);
                    data.put("rating", (double) rating);
                    data.put("comment", cleanComment);
                    data.put("createdAt", Timestamp.now());

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
                        ProductReviewModel review = FirestoreStoreHelper.mapReview(doc, productId);
                        if (ProductReviewHelper.isVerifiedRealReview(review)) {
                            ratings.add((double) review.getRating());
                        }
                    }
                    double avg = ProductReviewHelper.calculateAverageRating(ratings);
                    productRef.update("ratingAvg", avg, "reviewCount", ratings.size())
                            .addOnFailureListener(e -> Log.d(TAG, "Product rating sync skipped: " + e.getMessage()));
                });
    }

    public boolean toggleFavorite(@NonNull String productId) {
        return store.toggleFavorite(productId);
    }

    public boolean addToCart(
            @NonNull ProductModel product,
            @Nullable ProductVariantModel variant,
            @NonNull String size,
            @NonNull String colorName,
            int qty
    ) {
        if (variant != null) {
            return store.addToCartWithinStock(
                    product,
                    variant.getVariantId(),
                    size,
                    colorName,
                    qty,
                    variant.getStockQty()
            );
        }
        store.addToCart(product, "", size, colorName, qty);
        return true;
    }
}
