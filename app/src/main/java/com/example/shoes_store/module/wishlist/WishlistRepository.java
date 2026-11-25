package com.example.shoes_store.module.wishlist;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.BaseFirestoreRepository;
import com.example.shoes_store.core.CategoryStyleHelper;
import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.cart.CartRepository;
import com.example.shoes_store.module.cart.CartService;
import com.example.shoes_store.module.product.ProductMapper;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.product.ProductRepository;
import com.example.shoes_store.module.product.ProductVariantModel;
import com.example.shoes_store.module.profile.ProfileModel;
import com.example.shoes_store.module.profile.ProfileRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Repository sở hữu danh sách Yêu thích (`users/{uid}/wishlist`) và thao tác thêm nhanh vào giỏ.
 */
public class WishlistRepository extends BaseFirestoreRepository {

    private static final String TAG = "WishlistRepository";
    private static final String LISTENER_WISHLIST = "wishlist";
    private static volatile WishlistRepository instance;

    private final MutableLiveData<Set<String>> favoriteIdsLiveData = new MutableLiveData<>(new LinkedHashSet<>());

    private WishlistRepository() {
    }

    public static WishlistRepository getInstance() {
        if (instance == null) {
            synchronized (WishlistRepository.class) {
                if (instance == null) {
                    instance = new WishlistRepository();
                }
            }
        }
        return instance;
    }

    @Override
    public void onSessionStart(@NonNull String uid, @NonNull FirebaseUser authUser) {
        startUserListening(uid);
    }

    @Override
    public void onSessionEnd() {
        stopUserListening();
    }

    // ===== 1. REALTIME =====

    public void startUserListening(@NonNull String uid) {
        stopUserListening();
        replaceUserListener(LISTENER_WISHLIST, listenCollection(
                userSubCollection(uid, FirebaseProviderHelper.SUB_WISHLIST),
                TAG,
                doc -> doc,
                (a, b) -> Long.compare(
                        FirestoreReadUtil.readMillis(b, WishlistMapper.FIELD_ADDED_AT),
                        FirestoreReadUtil.readMillis(a, WishlistMapper.FIELD_ADDED_AT)
                ),
                docs -> {
                    Set<String> ids = new LinkedHashSet<>();
                    for (DocumentSnapshot doc : docs) {
                        String pid = FirestoreReadUtil.readString(doc, WishlistMapper.FIELD_PRODUCT_ID, doc.getId());
                        if (!pid.isEmpty()) {
                            ids.add(pid);
                        }
                    }
                    favoriteIdsLiveData.setValue(ids);
                }
        ));
    }

    public void stopUserListening() {
        clearUserListeners();
        favoriteIdsLiveData.setValue(new LinkedHashSet<>());
    }

    // ===== 2. ĐỌC =====

    public LiveData<Set<String>> getFavoriteIdsLiveData() {
        return favoriteIdsLiveData;
    }

    public boolean isFavorite(@Nullable String productId) {
        Set<String> ids = favoriteIdsLiveData.getValue();
        return productId != null && ids != null && ids.contains(productId);
    }

    @NonNull
    public List<ProductModel> getFavoriteProducts(@Nullable Set<String> favoriteIds, @NonNull String category) {
        List<ProductModel> result = new ArrayList<>();
        if (favoriteIds == null || favoriteIds.isEmpty()) {
            return result;
        }
        for (ProductModel p : ProductRepository.getInstance().getAllProducts()) {
            if (!favoriteIds.contains(p.getId())) {
                continue;
            }
            if (!CategoryStyleHelper.CAT_ALL.equals(category) && !p.getCategory().equalsIgnoreCase(category)) {
                continue;
            }
            result.add(p);
        }
        return result;
    }

    // ===== 3. GHI =====

    public boolean toggleFavorite(@NonNull String productId) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            return false;
        }
        CollectionReference ref = userSubCollection(uid, FirebaseProviderHelper.SUB_WISHLIST);
        boolean currentlyFav = isFavorite(productId);
        if (currentlyFav) {
            ref.document(productId).delete()
                    .addOnFailureListener(e -> Log.w(TAG, "Remove favorite failed: " + e.getMessage()));
            return false;
        } else {
            ref.document(productId).set(WishlistMapper.toMap(productId, uid, Timestamp.now()))
                    .addOnFailureListener(e -> Log.w(TAG, "Add favorite failed: " + e.getMessage()));
            return true;
        }
    }

    public void removeFavorite(@NonNull String productId) {
        if (isFavorite(productId)) {
            toggleFavorite(productId);
        }
    }

    public void addQuickToCart(@NonNull ProductModel product, @NonNull ResourceCallback<String> callback) {
        ProfileModel profile = ProfileRepository.getInstance().getProfile();
        String preferred = profile != null ? profile.getPreferredSize() : "42";

        collection(FirebaseProviderHelper.COL_PRODUCTS)
                .document(product.getId())
                .collection(FirebaseProviderHelper.SUB_VARIANTS)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ProductVariantModel> variants = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        variants.add(ProductMapper.variantFromSnapshot(doc, product.getId()));
                    }
                    product.setVariants(variants);

                    ProductVariantModel chosen = CartService.chooseQuickAddVariant(variants, preferred);
                    if (chosen == null) {
                        callback.onError("Mẫu giày này hiện đang hết hàng");
                        return;
                    }
                    boolean added = CartRepository.getInstance().addToCartWithinStock(
                            product,
                            chosen.getVariantId(),
                            chosen.getSize(),
                            chosen.getColorName(),
                            1,
                            chosen.getStockQty()
                    );
                    if (!added) {
                        callback.onError("Giỏ hàng đã đạt tối đa tồn kho (" + chosen.getStockQty() + " đôi)");
                        return;
                    }
                    callback.onSuccess(chosen.getSize());
                })
                .addOnFailureListener(e -> callback.onError("Không thể kiểm tra tồn kho, vui lòng thử lại"));
    }
}
