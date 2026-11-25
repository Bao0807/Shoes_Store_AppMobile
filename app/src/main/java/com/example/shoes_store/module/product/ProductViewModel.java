package com.example.shoes_store.module.product;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.module.cart.CartRepository;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.order.OrderRepository;
import com.example.shoes_store.module.wishlist.WishlistRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Set;

/**
 * ViewModel cho màn Chi tiết sản phẩm (quản lý listener realtime biến thể tồn kho và giỏ/yêu thích).
 */
public class ProductViewModel extends ViewModel {

    private final ProductRepository repository = ProductRepository.getInstance();
    private final WishlistRepository wishlistRepository = WishlistRepository.getInstance();
    private final OrderRepository orderRepository = OrderRepository.getInstance();
    private final CartRepository cartRepository = CartRepository.getInstance();
    private final FirebaseProviderHelper firebaseProvider = FirebaseProviderHelper.getInstance();
    private final MutableLiveData<List<ProductVariantModel>> variantsLiveData = new MutableLiveData<>();

    @Nullable
    private String observedProductId;
    @Nullable
    private ListenerRegistration variantsRegistration;

    @Nullable
    public ProductModel getProduct(@Nullable String productId) {
        return repository.getProductById(productId);
    }

    public LiveData<List<ProductModel>> getProductsLiveData() {
        return repository.getProductsLiveData();
    }

    public LiveData<List<ProductVariantModel>> loadVariants(@NonNull String productId) {
        if (!productId.equals(observedProductId)) {
            detachListeners();
            observedProductId = productId;
            variantsRegistration = repository.observeVariants(productId, variantsLiveData);
        }
        return variantsLiveData;
    }

    private void detachListeners() {
        if (variantsRegistration != null) {
            variantsRegistration.remove();
            variantsRegistration = null;
        }
    }

    public LiveData<Set<String>> getFavoriteIdsLiveData() {
        return wishlistRepository.getFavoriteIdsLiveData();
    }

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return orderRepository.getOrdersLiveData();
    }

    public boolean toggleFavorite(@NonNull String productId) {
        return wishlistRepository.toggleFavorite(productId);
    }

    public boolean isSignedIn() {
        return firebaseProvider.isSignedIn();
    }

    @Nullable
    public String getCurrentUid() {
        return firebaseProvider.getCurrentUid();
    }

    public boolean addToCart(
            @NonNull ProductModel product,
            @NonNull ProductVariantModel variant,
            @NonNull String size,
            @NonNull String colorName,
            int qty
    ) {
        return cartRepository.addToCartWithinStock(
                product,
                variant.getVariantId(),
                size,
                colorName,
                qty,
                variant.getStockQty()
        );
    }

    @Override
    protected void onCleared() {
        detachListeners();
        super.onCleared();
    }
}
