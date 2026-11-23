package com.example.shoes_store.module.wishlist;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.shoes_store.core.MockStoreHelper;
import com.example.shoes_store.module.product.ProductModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Repository cho màn Yêu thích.
 */
public class WishlistRepository {

    private final MockStoreHelper store = MockStoreHelper.getInstance();

    public LiveData<Set<String>> getFavoriteIdsLiveData() {
        return store.getFavoriteIdsLiveData();
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

    public String addQuickToCart(@NonNull ProductModel product) {
        String preferred = store.getProfile().getPreferredSize();
        String chosenSize = preferred;
        if (product.getOutOfStockSizes().contains(preferred)) {
            for (String s : product.getAllSizes()) {
                if (!product.getOutOfStockSizes().contains(s)) {
                    chosenSize = s;
                    break;
                }
            }
        }
        String color = product.getColorNames().isEmpty() ? "Mặc định" : product.getColorNames().get(0);
        store.addToCart(product, chosenSize, color, 1);
        return chosenSize;
    }
}
