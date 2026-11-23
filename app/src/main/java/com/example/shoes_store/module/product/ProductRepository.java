package com.example.shoes_store.module.product;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.cart.CartRepository;
import com.example.shoes_store.module.order.OrderMapper;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.order.OrderRepository;
import com.example.shoes_store.module.profile.ProfileMapper;
import com.example.shoes_store.module.wishlist.WishlistRepository;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Repository sở hữu đồng bộ realtime Sản phẩm (`products`), Biến thể (`variants`) và Đánh giá (`reviews`).
 */
public class ProductRepository {

    private static final String TAG = "ProductRepository";
    private static volatile ProductRepository instance;

    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();
    private final MutableLiveData<List<ProductModel>> productsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final List<ProductModel> rawProducts = new ArrayList<>();
    private final Map<String, ReviewStats> liveReviewStatsByProduct = new HashMap<>();
    private final Map<String, ListenerRegistration> productReviewRegistrations = new HashMap<>();
    private final Set<String> syncedRatingKeys = new HashSet<>();
    @Nullable
    private ListenerRegistration productsRegistration;

    private ProductRepository() {
    }

    public static ProductRepository getInstance() {
        if (instance == null) {
            synchronized (ProductRepository.class) {
                if (instance == null) {
                    instance = new ProductRepository();
                }
            }
        }
        return instance;
    }

    // ===== 1. REALTIME =====

    public void startPublicListening() {
        if (productsRegistration != null) {
            productsRegistration.remove();
        }
        productsRegistration = provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_PRODUCTS)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Products listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    rawProducts.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ProductModel product = ProductMapper.productFromSnapshot(doc);
                        if (product.isActive()) {
                            rawProducts.add(product);
                        }
                    }
                    syncProductReviewListeners();
                    publishProductsWithLiveReviewStats();
                });
    }

    public void refreshPublicCatalog() {
        provider.getFirestore().collection(FirebaseProviderHelper.COL_PRODUCTS).get();
    }

    public void reconcileAllRatingsOnSignIn() {
        syncedRatingKeys.clear();
        for (Map.Entry<String, ReviewStats> entry : liveReviewStatsByProduct.entrySet()) {
            ReviewStats stats = entry.getValue();
            reconcileProductRatingIfSignedIn(entry.getKey(), stats.ratingAvg, stats.reviewCount);
        }
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
                        list.add(ProductMapper.variantFromSnapshot(doc, productId));
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
                        ProductReviewModel review = ProductMapper.reviewFromSnapshot(doc, productId);
                        if (ProductReviewService.isVerifiedRealReview(review)) {
                            list.add(review);
                        }
                    }
                    Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()));
                    target.setValue(list);
                });
    }

    // ===== 2. ĐỌC =====

    public LiveData<List<ProductModel>> getProductsLiveData() {
        return productsLiveData;
    }

    @NonNull
    public List<ProductModel> getAllProducts() {
        List<ProductModel> list = productsLiveData.getValue();
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    @Nullable
    public ProductModel getProductById(@Nullable String productId) {
        if (productId == null) {
            return null;
        }
        List<ProductModel> list = productsLiveData.getValue();
        if (list == null) {
            return null;
        }
        for (ProductModel p : list) {
            if (productId.equals(p.getId())) {
                return p;
            }
        }
        return null;
    }

    public LiveData<Set<String>> getFavoriteIdsLiveData() {
        return WishlistRepository.getInstance().getFavoriteIdsLiveData();
    }

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return OrderRepository.getInstance().getOrdersLiveData();
    }

    // ===== 3. GHI =====

    public boolean toggleFavorite(@NonNull String productId) {
        return WishlistRepository.getInstance().toggleFavorite(productId);
    }

    public boolean addToCart(
            @NonNull ProductModel product,
            @Nullable ProductVariantModel variant,
            @NonNull String size,
            @NonNull String colorName,
            int qty
    ) {
        CartRepository cartRepository = CartRepository.getInstance();
        if (variant != null) {
            return cartRepository.addToCartWithinStock(
                    product,
                    variant.getVariantId(),
                    size,
                    colorName,
                    qty,
                    variant.getStockQty()
            );
        }
        cartRepository.addToCart(product, "", size, colorName, qty);
        return true;
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
        String validationError = ProductReviewService.validateReviewInput(uid, orderId, rating, comment);
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
                .whereEqualTo(ProductMapper.FIELD_USER_ID, uid)
                .whereEqualTo(ProductMapper.FIELD_ORDER_ID, cleanOrderId)
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

                    boolean containsProduct = false;
                    Object rawItems = orderSnap.get(OrderMapper.FIELD_ITEMS);
                    if (rawItems instanceof List<?>) {
                        for (Object obj : (List<?>) rawItems) {
                            if (obj instanceof Map<?, ?>) {
                                Object pidObj = ((Map<String, Object>) obj).get(OrderMapper.ITEM_PRODUCT_ID);
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

                    String reviewerName = FirestoreReadUtil.readString(userSnap, ProfileMapper.FIELD_FULL_NAME, "Runner").trim();
                    if (reviewerName.isEmpty()) {
                        reviewerName = "Runner";
                    }
                    String reviewerAvatarUrl = FirestoreReadUtil.readString(userSnap, ProfileMapper.FIELD_AVATAR_URL, "");

                    DocumentReference reviewRef = productRef.collection(FirebaseProviderHelper.SUB_REVIEWS).document();
                    Map<String, Object> data = ProductMapper.reviewToMap(
                            reviewRef.getId(),
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

    // ===== 4. PRIVATE HELPERS =====

    private void syncProductReviewListeners() {
        Set<String> activeIds = new HashSet<>();
        for (ProductModel p : rawProducts) {
            activeIds.add(p.getId());
        }

        Iterator<Map.Entry<String, ListenerRegistration>> it = productReviewRegistrations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ListenerRegistration> entry = it.next();
            if (!activeIds.contains(entry.getKey())) {
                entry.getValue().remove();
                liveReviewStatsByProduct.remove(entry.getKey());
                it.remove();
            }
        }

        FirebaseFirestore db = provider.getFirestore();
        for (ProductModel raw : rawProducts) {
            String productId = raw.getId();
            if (productId.isEmpty() || productReviewRegistrations.containsKey(productId)) {
                continue;
            }
            ListenerRegistration reg = db.collection(FirebaseProviderHelper.COL_PRODUCTS)
                    .document(productId)
                    .collection(FirebaseProviderHelper.SUB_REVIEWS)
                    .addSnapshotListener((snap, error) -> {
                        if (error != null) {
                            Log.w(TAG, "Product reviews sync error (" + productId + "): " + error.getMessage());
                        }
                        if (snap == null) {
                            return;
                        }
                        List<Double> ratings = new ArrayList<>();
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            ProductReviewModel rev = ProductMapper.reviewFromSnapshot(doc, productId);
                            if (ProductReviewService.isVerifiedRealReview(rev)) {
                                ratings.add((double) rev.getRating());
                            }
                        }
                        double liveAvg = ProductReviewService.calculateAverageRating(ratings);
                        int liveCount = ratings.size();
                        liveReviewStatsByProduct.put(productId, new ReviewStats(liveAvg, liveCount));
                        publishProductsWithLiveReviewStats();
                        reconcileProductRatingIfSignedIn(productId, liveAvg, liveCount);
                    });
            productReviewRegistrations.put(productId, reg);
        }
    }

    private void publishProductsWithLiveReviewStats() {
        List<ProductModel> merged = new ArrayList<>(rawProducts.size());
        for (ProductModel raw : rawProducts) {
            ReviewStats stats = liveReviewStatsByProduct.get(raw.getId());
            if (stats != null) {
                merged.add(raw.withReviewStats(stats.ratingAvg, stats.reviewCount));
            } else {
                merged.add(raw);
            }
        }
        productsLiveData.setValue(merged);
        CartRepository.getInstance().rebuildCartLiveData();
    }

    private void reconcileProductRatingIfSignedIn(@NonNull String productId, double liveAvg, int liveCount) {
        if (!provider.isSignedIn()) {
            return;
        }
        ProductModel matchingRaw = null;
        for (ProductModel raw : rawProducts) {
            if (productId.equals(raw.getId())) {
                matchingRaw = raw;
                break;
            }
        }
        if (matchingRaw == null) {
            return;
        }
        if (Math.abs(matchingRaw.getRating() - liveAvg) <= 0.01 && matchingRaw.getReviewCount() == liveCount) {
            return;
        }
        String syncKey = productId + "|" + liveAvg + "|" + liveCount;
        if (!syncedRatingKeys.add(syncKey)) {
            return;
        }
        provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_PRODUCTS)
                .document(productId)
                .update(ProductMapper.FIELD_RATING_AVG, liveAvg, ProductMapper.FIELD_REVIEW_COUNT, liveCount)
                .addOnFailureListener(e -> Log.d(TAG, "Product rating reconcile skipped: " + e.getMessage()));
    }

    private void syncProductRatingStats(@NonNull DocumentReference productRef) {
        String productId = productRef.getId();
        productRef.collection(FirebaseProviderHelper.SUB_REVIEWS)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Double> ratings = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ProductReviewModel review = ProductMapper.reviewFromSnapshot(doc, productId);
                        if (ProductReviewService.isVerifiedRealReview(review)) {
                            ratings.add((double) review.getRating());
                        }
                    }
                    double avg = ProductReviewService.calculateAverageRating(ratings);
                    productRef.update(ProductMapper.FIELD_RATING_AVG, avg, ProductMapper.FIELD_REVIEW_COUNT, ratings.size())
                            .addOnFailureListener(e -> Log.d(TAG, "Product rating sync skipped: " + e.getMessage()));
                });
    }

    private static final class ReviewStats {
        final double ratingAvg;
        final int reviewCount;

        ReviewStats(double ratingAvg, int reviewCount) {
            this.ratingAvg = ratingAvg;
            this.reviewCount = reviewCount;
        }
    }
}
