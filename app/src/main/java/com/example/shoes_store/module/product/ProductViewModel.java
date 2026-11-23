package com.example.shoes_store.module.product;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.order.OrderModel;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ViewModel cho màn Chi tiết sản phẩm (quản lý listener realtime variants & reviews).
 */
public class ProductViewModel extends ViewModel {

    private final ProductRepository repository = new ProductRepository();
    private final MutableLiveData<List<ProductVariantModel>> variantsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ProductReviewModel>> reviewsLiveData = new MutableLiveData<>(new ArrayList<>());

    @Nullable
    private String observedProductId;
    @Nullable
    private ListenerRegistration variantsRegistration;
    @Nullable
    private ListenerRegistration reviewsRegistration;

    @Nullable
    public ProductModel getProduct(@Nullable String productId) {
        return repository.getProductById(productId);
    }

    public LiveData<List<ProductModel>> getProductsLiveData() {
        return repository.getProductsLiveData();
    }

    public LiveData<List<ProductVariantModel>> loadVariants(@NonNull String productId) {
        ensureProductListeners(productId);
        return variantsLiveData;
    }

    public LiveData<List<ProductReviewModel>> loadReviews(@NonNull String productId) {
        ensureProductListeners(productId);
        return reviewsLiveData;
    }

    private void ensureProductListeners(@NonNull String productId) {
        if (productId.equals(observedProductId)) {
            return;
        }
        detachListeners();
        observedProductId = productId;
        variantsRegistration = repository.observeVariants(productId, variantsLiveData);
        reviewsRegistration = repository.observeReviews(productId, reviewsLiveData);
    }

    private void detachListeners() {
        if (variantsRegistration != null) {
            variantsRegistration.remove();
            variantsRegistration = null;
        }
        if (reviewsRegistration != null) {
            reviewsRegistration.remove();
            reviewsRegistration = null;
        }
    }

    public LiveData<Set<String>> getFavoriteIdsLiveData() {
        return repository.getFavoriteIdsLiveData();
    }

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return repository.getOrdersLiveData();
    }

    public boolean toggleFavorite(@NonNull String productId) {
        return repository.toggleFavorite(productId);
    }

    public boolean addToCart(
            @NonNull ProductModel product,
            @Nullable ProductVariantModel variant,
            @NonNull String size,
            @NonNull String colorName,
            int qty
    ) {
        return repository.addToCart(product, variant, size, colorName, qty);
    }

    public void submitReview(
            @NonNull String productId,
            @NonNull String orderId,
            float rating,
            @NonNull String comment,
            @NonNull ResourceCallback<Void> callback
    ) {
        repository.submitReview(productId, orderId, rating, comment, callback);
    }

    @Override
    protected void onCleared() {
        detachListeners();
        super.onCleared();
    }
}
