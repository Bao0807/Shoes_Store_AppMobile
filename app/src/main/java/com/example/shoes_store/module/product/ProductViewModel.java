package com.example.shoes_store.module.product;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.Set;

/**
 * ViewModel cho màn Chi tiết sản phẩm.
 */
public class ProductViewModel extends ViewModel {

    private final ProductRepository repository = new ProductRepository();

    @Nullable
    public ProductModel getProduct(@Nullable String productId) {
        return repository.getProductById(productId);
    }

    public LiveData<Set<String>> getFavoriteIdsLiveData() {
        return repository.getFavoriteIdsLiveData();
    }

    public boolean toggleFavorite(@NonNull String productId) {
        return repository.toggleFavorite(productId);
    }

    public void addToCart(@NonNull ProductModel product, @NonNull String size, @NonNull String colorName, int qty) {
        repository.addToCart(product, size, colorName, qty);
    }
}
