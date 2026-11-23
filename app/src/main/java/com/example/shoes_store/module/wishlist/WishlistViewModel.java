package com.example.shoes_store.module.wishlist;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.product.ProductModel;

import java.util.List;
import java.util.Set;

/**
 * ViewModel cho tab Yêu thích.
 */
public class WishlistViewModel extends ViewModel {

    private final WishlistRepository repository = WishlistRepository.getInstance();
    private String selectedCategory = "Tất cả";

    public LiveData<Set<String>> getFavoriteIdsLiveData() {
        return repository.getFavoriteIdsLiveData();
    }

    public LiveData<List<ProductModel>> getProductsLiveData() {
        return repository.getProductsLiveData();
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(@NonNull String category) {
        this.selectedCategory = category;
    }

    public List<ProductModel> getFilteredFavorites(Set<String> favoriteIds) {
        return repository.getFavoriteProducts(favoriteIds, selectedCategory);
    }

    public void removeFavorite(@NonNull String productId) {
        repository.removeFavorite(productId);
    }

    public void addQuickToCart(@NonNull ProductModel product, @NonNull ResourceCallback<String> callback) {
        repository.addQuickToCart(product, callback);
    }
}
