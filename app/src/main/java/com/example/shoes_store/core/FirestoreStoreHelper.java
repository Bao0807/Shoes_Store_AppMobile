package com.example.shoes_store.core;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.R;
import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.cart.CartVoucherModel;
import com.example.shoes_store.module.home.HomeBannerModel;
import com.example.shoes_store.module.notification.NotificationModel;
import com.example.shoes_store.module.order.OrderItemModel;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.product.ProductReviewHelper;
import com.example.shoes_store.module.product.ProductReviewModel;
import com.example.shoes_store.module.product.ProductVariantModel;
import com.example.shoes_store.module.profile.ProfileAddressModel;
import com.example.shoes_store.module.profile.ProfileModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Trung tâm đồng bộ realtime Cloud Firestore và phát LiveData cho toàn bộ các module.
 */
public final class FirestoreStoreHelper {

    private static final String TAG = "FirestoreStoreHelper";
    private static volatile FirestoreStoreHelper instance;

    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();
    private final MutableLiveData<Boolean> signedInLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<List<HomeBannerModel>> bannersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ProductModel>> productsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<CartVoucherModel>> vouchersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ProfileModel> profileLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ProfileAddressModel>> addressesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Set<String>> favoriteIdsLiveData = new MutableLiveData<>(new LinkedHashSet<>());
    private final MutableLiveData<List<CartItemModel>> cartItemsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> cartTotalCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<List<NotificationModel>> notificationsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> unreadNotificationCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<List<OrderModel>> ordersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> recentSearchesLiveData = new MutableLiveData<>(new ArrayList<>());

    private final List<RawCartDoc> rawCartDocs = new ArrayList<>();
    private final List<ProductModel> rawProducts = new ArrayList<>();
    private final Map<String, ReviewStats> liveReviewStatsByProduct = new HashMap<>();
    private final Map<String, ListenerRegistration> productReviewRegistrations = new HashMap<>();
    private final Set<String> syncedRatingKeys = new HashSet<>();
    private final List<ListenerRegistration> userRegistrations = new ArrayList<>();
    private ListenerRegistration bannersRegistration;
    private ListenerRegistration productsRegistration;
    private ListenerRegistration vouchersRegistration;
    private boolean initialized = false;
    @Nullable
    private String activeUid;

    private FirestoreStoreHelper() {
    }

    public static FirestoreStoreHelper getInstance() {
        if (instance == null) {
            synchronized (FirestoreStoreHelper.class) {
                if (instance == null) {
                    instance = new FirestoreStoreHelper();
                }
            }
        }
        return instance;
    }

    public synchronized void init(@NonNull Context context) {
        if (initialized) {
            return;
        }
        initialized = true;
        AppPrefsHelper.init(context);
        recentSearchesLiveData.setValue(AppPrefsHelper.getRecentSearches());

        attachPublicCatalogListeners();

        provider.getAuth().addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            String uid = user != null ? user.getUid() : null;
            if (uid == null) {
                activeUid = null;
                signedInLiveData.setValue(false);
                detachUserListenersAndReset();
            } else if (!uid.equals(activeUid)) {
                activeUid = uid;
                signedInLiveData.setValue(true);
                attachUserListeners(uid, user);
                ensureUserDocumentExists(uid, user);
                syncedRatingKeys.clear();
                for (Map.Entry<String, ReviewStats> entry : liveReviewStatsByProduct.entrySet()) {
                    ReviewStats stats = entry.getValue();
                    reconcileProductRatingIfSignedIn(entry.getKey(), stats.ratingAvg, stats.reviewCount);
                }
            }
        });
    }

    private void ensureUserDocumentExists(@NonNull String uid, @NonNull FirebaseUser authUser) {
        FirebaseFirestore db = provider.getFirestore();
        db.collection(FirebaseProviderHelper.COL_USERS).document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        return;
                    }
                    String display = authUser.getDisplayName();
                    String fullName;
                    if (display != null && !display.trim().isEmpty()) {
                        fullName = display.trim();
                    } else {
                        String email = authUser.getEmail();
                        if (email != null && email.contains("@")) {
                            String prefix = email.substring(0, email.indexOf('@')).trim();
                            fullName = !prefix.isEmpty()
                                    ? prefix.substring(0, 1).toUpperCase(Locale.ROOT) + prefix.substring(1)
                                    : "Runner";
                        } else {
                            fullName = "Runner";
                        }
                    }
                    Timestamp now = Timestamp.now();
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", uid);
                    userData.put("fullName", fullName);
                    userData.put("email", authUser.getEmail() != null ? authUser.getEmail() : "");
                    userData.put("phone", "");
                    userData.put("avatarUrl", "");
                    userData.put("preferredSize", "42");
                    userData.put("runningGoal", "Daily");
                    userData.put("role", "CUSTOMER");
                    userData.put("recentSearches", new ArrayList<>(AppPrefsHelper.getRecentSearches()));
                    userData.put("createdAt", now);
                    userData.put("updatedAt", now);
                    db.collection(FirebaseProviderHelper.COL_USERS).document(uid)
                            .set(userData)
                            .addOnFailureListener(e -> Log.w(TAG, "Ensure user doc failed: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.w(TAG, "Check user doc failed: " + e.getMessage()));
    }

    private void attachPublicCatalogListeners() {
        FirebaseFirestore db = provider.getFirestore();

        if (bannersRegistration != null) {
            bannersRegistration.remove();
        }
        bannersRegistration = db.collection(FirebaseProviderHelper.COL_BANNERS)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Banners listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<HomeBannerModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        HomeBannerModel banner = mapBanner(doc);
                        if (banner != null && banner.isActive()) {
                            list.add(banner);
                        }
                    }
                    Collections.sort(list, Comparator.comparingInt(HomeBannerModel::getDisplayOrder));
                    bannersLiveData.setValue(list);
                });

        if (productsRegistration != null) {
            productsRegistration.remove();
        }
        productsRegistration = db.collection(FirebaseProviderHelper.COL_PRODUCTS)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Products listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    rawProducts.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ProductModel product = mapProduct(doc);
                        if (product != null && product.isActive()) {
                            rawProducts.add(product);
                        }
                    }
                    syncProductReviewListeners();
                    publishProductsWithLiveReviewStats();
                });

        if (vouchersRegistration != null) {
            vouchersRegistration.remove();
        }
        vouchersRegistration = db.collection(FirebaseProviderHelper.COL_VOUCHERS)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Vouchers listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<CartVoucherModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        CartVoucherModel voucher = mapVoucher(doc);
                        if (voucher != null && voucher.isActive()) {
                            list.add(voucher);
                        }
                    }
                    Collections.sort(list, Comparator.comparingLong(CartVoucherModel::getMinOrderAmount));
                    vouchersLiveData.setValue(list);
                });
    }

    public void refreshPublicCatalog() {
        FirebaseFirestore db = provider.getFirestore();
        db.collection(FirebaseProviderHelper.COL_BANNERS).get();
        db.collection(FirebaseProviderHelper.COL_PRODUCTS).get();
        db.collection(FirebaseProviderHelper.COL_VOUCHERS).get();
    }

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
                            ProductReviewModel rev = mapReview(doc, productId);
                            if (ProductReviewHelper.isVerifiedRealReview(rev)) {
                                ratings.add((double) rev.getRating());
                            }
                        }
                        double liveAvg = ProductReviewHelper.calculateAverageRating(ratings);
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
        rebuildCartLiveData();
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
                .update("ratingAvg", liveAvg, "reviewCount", liveCount)
                .addOnFailureListener(e -> Log.d(TAG, "Product rating reconcile skipped: " + e.getMessage()));
    }

    private void attachUserListeners(@NonNull String uid, @NonNull FirebaseUser authUser) {
        detachUserListenersAndReset();
        FirebaseFirestore db = provider.getFirestore();

        userRegistrations.add(db.collection(FirebaseProviderHelper.COL_USERS).document(uid)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        Log.w(TAG, "User profile listener error: " + error.getMessage());
                    }
                    if (doc == null || !doc.exists()) {
                        profileLiveData.setValue(new ProfileModel(
                                uid,
                                authUser.getDisplayName() != null ? authUser.getDisplayName() : "Runner",
                                authUser.getEmail() != null ? authUser.getEmail() : "",
                                "",
                                "42",
                                "Daily",
                                "CUSTOMER",
                                R.drawable.img_avatar_runner,
                                null
                        ));
                        return;
                    }
                    ProfileModel profile = mapProfile(doc, uid);
                    profileLiveData.setValue(profile);

                    List<String> remoteSearches = readStringList(doc, "recentSearches");
                    if (!remoteSearches.isEmpty()) {
                        AppPrefsHelper.replaceRecentSearches(remoteSearches);
                        recentSearchesLiveData.setValue(remoteSearches);
                    }
                }));

        userRegistrations.add(db.collection(FirebaseProviderHelper.COL_USERS).document(uid)
                .collection(FirebaseProviderHelper.SUB_ADDRESSES)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Addresses listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<ProfileAddressModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        list.add(mapAddress(doc, uid));
                    }
                    Collections.sort(list, (a, b) -> Boolean.compare(b.isDefaultAddress(), a.isDefaultAddress()));
                    addressesLiveData.setValue(list);
                }));

        userRegistrations.add(db.collection(FirebaseProviderHelper.COL_USERS).document(uid)
                .collection(FirebaseProviderHelper.SUB_WISHLIST)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Wishlist listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<DocumentSnapshot> docs = new ArrayList<>(snap.getDocuments());
                    Collections.sort(docs, (a, b) -> Long.compare(readMillis(b, "addedAt"), readMillis(a, "addedAt")));
                    Set<String> ids = new LinkedHashSet<>();
                    for (DocumentSnapshot doc : docs) {
                        String pid = readString(doc, "productId", doc.getId());
                        if (!pid.isEmpty()) {
                            ids.add(pid);
                        }
                    }
                    favoriteIdsLiveData.setValue(ids);
                }));

        userRegistrations.add(db.collection(FirebaseProviderHelper.COL_USERS).document(uid)
                .collection(FirebaseProviderHelper.SUB_CART)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Cart listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    rawCartDocs.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        rawCartDocs.add(new RawCartDoc(
                                doc.getId(),
                                readString(doc, "productId", ""),
                                readString(doc, "variantId", ""),
                                readString(doc, "size", "42"),
                                readString(doc, "colorName", "Mặc định"),
                                Math.max(1, readInt(doc, "quantity", 1)),
                                readBoolean(doc, "selected", true),
                                readMillis(doc, "updatedAt")
                        ));
                    }
                    Collections.sort(rawCartDocs, (a, b) -> Long.compare(b.updatedAtMillis, a.updatedAtMillis));
                    rebuildCartLiveData();
                }));

        userRegistrations.add(db.collection(FirebaseProviderHelper.COL_USERS).document(uid)
                .collection(FirebaseProviderHelper.SUB_NOTIFICATIONS)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Notifications listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<NotificationModel> list = new ArrayList<>();
                    int unread = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        NotificationModel item = mapNotification(doc, uid);
                        list.add(item);
                        if (item.isUnread()) {
                            unread++;
                        }
                    }
                    Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()));
                    notificationsLiveData.setValue(list);
                    unreadNotificationCountLiveData.setValue(unread);
                }));

        userRegistrations.add(db.collection(FirebaseProviderHelper.COL_ORDERS)
                .whereEqualTo("userId", uid)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Orders listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<OrderModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        list.add(mapOrder(doc));
                    }
                    Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()));
                    ordersLiveData.setValue(list);
                }));
    }

    private void detachUserListenersAndReset() {
        for (ListenerRegistration reg : userRegistrations) {
            reg.remove();
        }
        userRegistrations.clear();
        rawCartDocs.clear();
        profileLiveData.setValue(null);
        addressesLiveData.setValue(new ArrayList<>());
        favoriteIdsLiveData.setValue(new LinkedHashSet<>());
        cartItemsLiveData.setValue(new ArrayList<>());
        cartTotalCountLiveData.setValue(0);
        notificationsLiveData.setValue(new ArrayList<>());
        unreadNotificationCountLiveData.setValue(0);
        ordersLiveData.setValue(new ArrayList<>());
        recentSearchesLiveData.setValue(AppPrefsHelper.getRecentSearches());
    }

    private void rebuildCartLiveData() {
        List<CartItemModel> resolved = new ArrayList<>();
        int totalQty = 0;
        for (RawCartDoc raw : rawCartDocs) {
            ProductModel product = getProductById(raw.productId);
            if (product == null) {
                continue;
            }
            resolved.add(new CartItemModel(
                    raw.id,
                    product,
                    raw.variantId,
                    raw.size,
                    raw.colorName,
                    raw.quantity,
                    raw.selected,
                    raw.updatedAtMillis
            ));
            totalQty += raw.quantity;
        }
        cartItemsLiveData.setValue(resolved);
        cartTotalCountLiveData.setValue(totalQty);
    }

    public LiveData<Boolean> getSignedInLiveData() {
        return signedInLiveData;
    }

    public LiveData<List<HomeBannerModel>> getBannersLiveData() {
        return bannersLiveData;
    }

    public LiveData<List<ProductModel>> getProductsLiveData() {
        return productsLiveData;
    }

    public LiveData<List<CartVoucherModel>> getVouchersLiveData() {
        return vouchersLiveData;
    }

    public LiveData<ProfileModel> getProfileLiveData() {
        return profileLiveData;
    }

    public LiveData<List<ProfileAddressModel>> getAddressesLiveData() {
        return addressesLiveData;
    }

    public LiveData<Set<String>> getFavoriteIdsLiveData() {
        return favoriteIdsLiveData;
    }

    public LiveData<List<CartItemModel>> getCartItemsLiveData() {
        return cartItemsLiveData;
    }

    public LiveData<Integer> getCartTotalCountLiveData() {
        return cartTotalCountLiveData;
    }

    public LiveData<List<NotificationModel>> getNotificationsLiveData() {
        return notificationsLiveData;
    }

    public LiveData<Integer> getUnreadNotificationCountLiveData() {
        return unreadNotificationCountLiveData;
    }

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return ordersLiveData;
    }

    public LiveData<List<String>> getRecentSearchesLiveData() {
        return recentSearchesLiveData;
    }

    @NonNull
    public List<HomeBannerModel> getBanners() {
        List<HomeBannerModel> list = bannersLiveData.getValue();
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
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

    @NonNull
    public List<CartVoucherModel> getVouchers() {
        List<CartVoucherModel> list = vouchersLiveData.getValue();
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    @Nullable
    public ProfileModel getProfile() {
        return profileLiveData.getValue();
    }

    @Nullable
    public ProfileAddressModel getDefaultAddress() {
        List<ProfileAddressModel> list = addressesLiveData.getValue();
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (ProfileAddressModel address : list) {
            if (address.isDefaultAddress()) {
                return address;
            }
        }
        return list.get(0);
    }

    @Nullable
    public ProfileAddressModel findAddressById(@Nullable String addressId) {
        List<ProfileAddressModel> list = addressesLiveData.getValue();
        if (addressId == null || list == null) {
            return null;
        }
        for (ProfileAddressModel address : list) {
            if (addressId.equals(address.getId())) {
                return address;
            }
        }
        return null;
    }

    public boolean isFavorite(@Nullable String productId) {
        Set<String> ids = favoriteIdsLiveData.getValue();
        return productId != null && ids != null && ids.contains(productId);
    }

    @NonNull
    public List<CartItemModel> getSelectedCartItems() {
        List<CartItemModel> list = cartItemsLiveData.getValue();
        List<CartItemModel> selected = new ArrayList<>();
        if (list != null) {
            for (CartItemModel item : list) {
                if (item.isSelected()) {
                    selected.add(item.copy());
                }
            }
        }
        return selected;
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

    public void addRecentSearch(@NonNull String query) {
        List<String> updated = AppPrefsHelper.addRecentSearch(query);
        recentSearchesLiveData.setValue(updated);
        syncRecentSearchesToFirestore(updated);
    }

    public void removeRecentSearch(@NonNull String query) {
        List<String> updated = AppPrefsHelper.removeRecentSearch(query);
        recentSearchesLiveData.setValue(updated);
        syncRecentSearchesToFirestore(updated);
    }

    private void syncRecentSearchesToFirestore(@NonNull List<String> updated) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            return;
        }
        provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .update("recentSearches", updated);
    }

    public boolean toggleFavorite(@NonNull String productId) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            return false;
        }
        CollectionReference ref = provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_WISHLIST);
        boolean currentlyFav = isFavorite(productId);
        if (currentlyFav) {
            ref.document(productId).delete();
            return false;
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("productId", productId);
            data.put("userId", uid);
            data.put("addedAt", Timestamp.now());
            ref.document(productId).set(data);
            return true;
        }
    }

    public int getCartQuantityForVariant(
            @NonNull String productId,
            @Nullable String variantId,
            @NonNull String size,
            @NonNull String colorName
    ) {
        List<CartItemModel> currentItems = cartItemsLiveData.getValue();
        if (currentItems == null) {
            return 0;
        }
        String cleanVariantId = variantId != null ? variantId : "";
        for (CartItemModel existing : currentItems) {
            boolean sameVariant = !cleanVariantId.isEmpty()
                    ? cleanVariantId.equals(existing.getVariantId()) && productId.equals(existing.getProduct().getId())
                    : productId.equals(existing.getProduct().getId())
                            && size.equals(existing.getSize())
                            && colorName.equals(existing.getColorName());
            if (sameVariant) {
                return existing.getQuantity();
            }
        }
        return 0;
    }

    public boolean addToCartWithinStock(
            @NonNull ProductModel product,
            @NonNull String variantId,
            @NonNull String size,
            @NonNull String colorName,
            int quantity,
            int maxStock
    ) {
        if (maxStock <= 0) {
            return false;
        }
        int existingQty = getCartQuantityForVariant(product.getId(), variantId, size, colorName);
        if (existingQty + Math.max(1, quantity) > maxStock) {
            return false;
        }
        addToCart(product, variantId, size, colorName, quantity);
        return true;
    }

    public void addToCart(
            @NonNull ProductModel product,
            @NonNull String variantId,
            @NonNull String size,
            @NonNull String colorName,
            int quantity
    ) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            return;
        }
        CollectionReference cartRef = provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_CART);

        List<CartItemModel> currentItems = cartItemsLiveData.getValue();
        if (currentItems != null) {
            for (CartItemModel existing : currentItems) {
                boolean sameVariant = !variantId.isEmpty()
                        ? variantId.equals(existing.getVariantId()) && product.getId().equals(existing.getProduct().getId())
                        : product.getId().equals(existing.getProduct().getId())
                                && size.equals(existing.getSize())
                                && colorName.equals(existing.getColorName());
                if (sameVariant) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("quantity", existing.getQuantity() + quantity);
                    updates.put("selected", true);
                    updates.put("updatedAt", Timestamp.now());
                    cartRef.document(existing.getId()).update(updates);
                    return;
                }
            }
        }

        String docId = cartRef.document().getId();
        Map<String, Object> data = new HashMap<>();
        data.put("cartItemId", docId);
        data.put("userId", uid);
        data.put("productId", product.getId());
        data.put("variantId", variantId);
        data.put("size", size);
        data.put("colorName", colorName);
        data.put("quantity", Math.max(1, quantity));
        data.put("selected", true);
        data.put("updatedAt", Timestamp.now());
        cartRef.document(docId).set(data);
    }

    @NonNull
    public static ProductVariantModel mapVariant(@NonNull DocumentSnapshot doc, @NonNull String fallbackProductId) {
        return new ProductVariantModel(
                readString(doc, "variantId", doc.getId()),
                readString(doc, "productId", fallbackProductId),
                readString(doc, "colorName", "Mặc định"),
                readString(doc, "colorHex", "#111827"),
                readString(doc, "imageUrl", ""),
                readString(doc, "size", "42"),
                readInt(doc, "stockQty", 0),
                readString(doc, "sku", "")
        );
    }

    @NonNull
    public static ProductReviewModel mapReview(@NonNull DocumentSnapshot doc, @NonNull String fallbackProductId) {
        return new ProductReviewModel(
                readString(doc, "reviewId", doc.getId()),
                readString(doc, "productId", fallbackProductId),
                readString(doc, "userId", ""),
                readString(doc, "orderId", ""),
                readString(doc, "reviewerName", "Runner"),
                readString(doc, "reviewerAvatarUrl", ""),
                (float) readDouble(doc, "rating", 5.0),
                "",
                readString(doc, "comment", ""),
                readMillis(doc, "createdAt")
        );
    }

    @Nullable
    private static HomeBannerModel mapBanner(@NonNull DocumentSnapshot doc) {
        return new HomeBannerModel(
                readString(doc, "bannerId", doc.getId()),
                readString(doc, "badge", ""),
                readString(doc, "title", ""),
                readString(doc, "subtitle", ""),
                readString(doc, "ctaLabel", ""),
                readString(doc, "targetCategory", "Daily"),
                readString(doc, "imageUrl", ""),
                R.drawable.img_banner_race,
                readInt(doc, "displayOrder", 0),
                readBoolean(doc, "isActive", true)
        );
    }

    @Nullable
    private static ProductModel mapProduct(@NonNull DocumentSnapshot doc) {
        List<String> galleryUrls = readStringList(doc, "galleryUrls");
        List<String> colorNames = readStringList(doc, "colorNames");
        List<String> colorHexes = readStringList(doc, "colorHexes");
        List<String> allSizes = readStringList(doc, "allSizes");
        Set<String> outSizes = new LinkedHashSet<>(readStringList(doc, "outOfStockSizes"));

        return new ProductModel(
                readString(doc, "productId", doc.getId()),
                readString(doc, "name", ""),
                readString(doc, "brand", ""),
                readString(doc, "category", "Daily"),
                readLong(doc, "price", 0L),
                readLong(doc, "originalPrice", 0L),
                readDouble(doc, "ratingAvg", 0.0),
                readInt(doc, "reviewCount", 0),
                R.drawable.img_shoe_asics_novablast,
                readString(doc, "thumbnailUrl", ""),
                galleryUrls,
                readString(doc, "weightGrams", ""),
                readString(doc, "heelDropMm", ""),
                readString(doc, "cushionLevel", ""),
                readString(doc, "surfaceType", "Road"),
                readString(doc, "description", ""),
                colorNames,
                colorHexes,
                allSizes,
                outSizes,
                new ArrayList<>(),
                readBoolean(doc, "isActive", true),
                readMillis(doc, "createdAt")
        );
    }

    @Nullable
    private static CartVoucherModel mapVoucher(@NonNull DocumentSnapshot doc) {
        return new CartVoucherModel(
                readString(doc, "code", doc.getId()),
                readString(doc, "shortBadge", ""),
                readString(doc, "title", ""),
                readString(doc, "description", ""),
                readLong(doc, "discountAmount", 0L),
                readLong(doc, "minOrderAmount", 0L),
                "",
                readInt(doc, "usageLimit", 0),
                readInt(doc, "usedCount", 0),
                readMillis(doc, "expiryDate"),
                readBoolean(doc, "isActive", true)
        );
    }

    @NonNull
    private static ProfileModel mapProfile(@NonNull DocumentSnapshot doc, @NonNull String uid) {
        String avatarUrl = readString(doc, "avatarUrl", "");
        return new ProfileModel(
                uid,
                readString(doc, "fullName", "Runner"),
                readString(doc, "email", ""),
                readString(doc, "phone", ""),
                readString(doc, "preferredSize", "42"),
                readString(doc, "runningGoal", "Daily"),
                readString(doc, "role", "CUSTOMER"),
                R.drawable.img_avatar_runner,
                avatarUrl.isEmpty() ? null : avatarUrl
        );
    }

    @NonNull
    private static ProfileAddressModel mapAddress(@NonNull DocumentSnapshot doc, @NonNull String uid) {
        return new ProfileAddressModel(
                readString(doc, "addressId", doc.getId()),
                readString(doc, "userId", uid),
                readString(doc, "receiverName", ""),
                readString(doc, "phone", ""),
                readString(doc, "addressDetail", ""),
                readString(doc, "label", "Nhà riêng"),
                readBoolean(doc, "isDefault", false)
        );
    }

    @NonNull
    private static NotificationModel mapNotification(@NonNull DocumentSnapshot doc, @NonNull String uid) {
        return new NotificationModel(
                readString(doc, "notificationId", doc.getId()),
                readString(doc, "userId", uid),
                readString(doc, "title", ""),
                readString(doc, "message", ""),
                null,
                NotificationModel.Type.fromStorage(readString(doc, "type", "ORDER")),
                readString(doc, "targetId", ""),
                readMillis(doc, "createdAt"),
                readBoolean(doc, "unread", false)
        );
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private static OrderModel mapOrder(@NonNull DocumentSnapshot doc) {
        List<OrderItemModel> orderItems = new ArrayList<>();
        Object rawItems = doc.get("items");
        if (rawItems instanceof List<?>) {
            for (Object entry : (List<?>) rawItems) {
                if (entry instanceof Map<?, ?>) {
                    Map<String, Object> map = (Map<String, Object>) entry;
                    orderItems.add(new OrderItemModel(
                            mapString(map, "productId", ""),
                            mapString(map, "variantId", ""),
                            mapString(map, "productName", ""),
                            mapString(map, "thumbnailUrl", ""),
                            R.drawable.img_shoe_asics_novablast,
                            mapString(map, "size", "42"),
                            mapString(map, "colorName", "Mặc định"),
                            mapLong(map, "unitPrice", 0L),
                            mapInt(map, "quantity", 1),
                            mapLong(map, "lineTotal", 0L)
                    ));
                }
            }
        }
        String receiverName = readString(doc, "receiverName", "");
        String receiverPhone = readString(doc, "receiverPhone", "");
        String receiverInfo = receiverPhone.isEmpty() ? receiverName : receiverName + " · " + receiverPhone;

        return new OrderModel(
                readString(doc, "orderId", doc.getId()),
                readString(doc, "orderCode", doc.getId()),
                readString(doc, "userId", ""),
                OrderModel.Status.fromStorage(readString(doc, "status", "PENDING")),
                PaymentModel.Method.fromStorage(readString(doc, "paymentMethod", "COD")),
                orderItems,
                readString(doc, "shippingAddress", ""),
                receiverInfo,
                readString(doc, "shippingMethod", "Giao tiêu chuẩn"),
                readString(doc, "voucherCode", ""),
                readLong(doc, "subtotal", 0L),
                readLong(doc, "shippingFee", 0L),
                readLong(doc, "discount", 0L),
                readLong(doc, "total", 0L),
                readMillis(doc, "createdAt"),
                readMillis(doc, "updatedAt")
        );
    }

    @NonNull
    public static String readString(@NonNull DocumentSnapshot doc, @NonNull String key, @NonNull String fallback) {
        String val = doc.getString(key);
        return val != null ? val : fallback;
    }

    public static long readLong(@NonNull DocumentSnapshot doc, @NonNull String key, long fallback) {
        Long val = doc.getLong(key);
        return val != null ? val : fallback;
    }

    public static int readInt(@NonNull DocumentSnapshot doc, @NonNull String key, int fallback) {
        Long val = doc.getLong(key);
        return val != null ? val.intValue() : fallback;
    }

    public static double readDouble(@NonNull DocumentSnapshot doc, @NonNull String key, double fallback) {
        Double val = doc.getDouble(key);
        return val != null ? val : fallback;
    }

    public static boolean readBoolean(@NonNull DocumentSnapshot doc, @NonNull String key, boolean fallback) {
        Boolean val = doc.getBoolean(key);
        return val != null ? val : fallback;
    }

    public static long readMillis(@NonNull DocumentSnapshot doc, @NonNull String key) {
        Timestamp ts = doc.getTimestamp(key);
        if (ts != null) {
            return ts.toDate().getTime();
        }
        Long num = doc.getLong(key);
        return num != null ? num : 0L;
    }

    @NonNull
    public static List<String> readStringList(@NonNull DocumentSnapshot doc, @NonNull String key) {
        List<String> result = new ArrayList<>();
        Object raw = doc.get(key);
        if (raw instanceof List<?>) {
            for (Object item : (List<?>) raw) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
        }
        return result;
    }

    @NonNull
    private static String mapString(@NonNull Map<String, Object> map, @NonNull String key, @NonNull String fallback) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : fallback;
    }

    private static long mapLong(@NonNull Map<String, Object> map, @NonNull String key, long fallback) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return fallback;
    }

    private static int mapInt(@NonNull Map<String, Object> map, @NonNull String key, int fallback) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return fallback;
    }

    private static final class ReviewStats {
        final double ratingAvg;
        final int reviewCount;

        ReviewStats(double ratingAvg, int reviewCount) {
            this.ratingAvg = ratingAvg;
            this.reviewCount = reviewCount;
        }
    }

    private static final class RawCartDoc {
        final String id;
        final String productId;
        final String variantId;
        final String size;
        final String colorName;
        final int quantity;
        final boolean selected;
        final long updatedAtMillis;

        RawCartDoc(String id, String productId, String variantId, String size, String colorName,
                   int quantity, boolean selected, long updatedAtMillis) {
            this.id = id;
            this.productId = productId;
            this.variantId = variantId;
            this.size = size;
            this.colorName = colorName;
            this.quantity = quantity;
            this.selected = selected;
            this.updatedAtMillis = updatedAtMillis;
        }
    }
}
