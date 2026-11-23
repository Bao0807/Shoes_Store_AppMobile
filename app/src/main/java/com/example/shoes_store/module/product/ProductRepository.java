package com.example.shoes_store.module.product;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.shoes_store.core.MockStoreHelper;

import java.util.Set;

/**
 * Repository cho màn Chi tiết sản phẩm.
 */
public class ProductRepository {

    private final MockStoreHelper store = MockStoreHelper.getInstance();

    @Nullable
    public ProductModel getProductById(@Nullable String productId) {
        return store.getProductById(productId);
    }

    public LiveData<Set<String>> getFavoriteIdsLiveData() {
        return store.getFavoriteIdsLiveData();
    }

    public boolean toggleFavorite(@NonNull String productId) {
        return store.toggleFavorite(productId);
    }

    public void addToCart(@NonNull ProductModel product, @NonNull String size, @NonNull String colorName, int qty) {
        store.addToCart(product, size, colorName, qty);
    }
}
