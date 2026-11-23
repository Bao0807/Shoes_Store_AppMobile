package com.example.shoes_store.module.wishlist;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreStoreHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.product.ProductVariantModel;
import com.example.shoes_store.module.profile.ProfileModel;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Repository cho màn Yêu thích đọc/ghi từ Cloud Firestore.
 */
public class WishlistRepository {

    private final FirestoreStoreHelper store = FirestoreStoreHelper.getInstance();
    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();

    public LiveData<Set<String>> getFavoriteIdsLiveData() {
        return store.getFavoriteIdsLiveData();
    }

    public LiveData<List<ProductModel>> getProductsLiveData() {
        return store.getProductsLiveData();
    }

    public List<ProductModel> getFavoriteProducts(Set<String> favoriteIds, String category) {
        List<ProductModel> result = new ArrayList<>();
        if (favoriteIds == null || favoriteIds.isEmpty()) {
            return result;
        }
        for (ProductModel p : store.getAllProducts()) {
            if (!favoriteIds.contains(p.getId())) {
                continue;
            }
            if (!"Tất cả".equals(category) && !p.getCategory().equalsIgnoreCase(category)) {
                continue;
            }
            result.add(p);
        }
        return result;
    }

    public void removeFavorite(@NonNull String productId) {
        if (store.isFavorite(productId)) {
            store.toggleFavorite(productId);
        }
    }

    public void addQuickToCart(@NonNull ProductModel product, @NonNull ResourceCallback<String> callback) {
        ProfileModel profile = store.getProfile();
        String preferred = profile != null ? profile.getPreferredSize() : "42";

        provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_PRODUCTS)
                .document(product.getId())
                .collection(FirebaseProviderHelper.SUB_VARIANTS)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ProductVariantModel> variants = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        variants.add(FirestoreStoreHelper.mapVariant(doc, product.getId()));
                    }
                    product.setVariants(variants);

                    ProductVariantModel chosen = null;
                    for (ProductVariantModel v : variants) {
                        if (preferred.equalsIgnoreCase(v.getSize()) && v.isInStock()) {
                            chosen = v;
                            break;
                        }
                    }
                    if (chosen == null) {
                        for (ProductVariantModel v : variants) {
                            if (v.isInStock()) {
                                chosen = v;
                                break;
                            }
                        }
                    }
                    if (chosen == null) {
                        callback.onError("Mẫu giày này hiện đang hết hàng");
                        return;
                    }
                    boolean added = store.addToCartWithinStock(
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
