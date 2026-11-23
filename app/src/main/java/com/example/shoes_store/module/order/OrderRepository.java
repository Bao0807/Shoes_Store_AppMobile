package com.example.shoes_store.module.order;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.shoes_store.core.MockStoreHelper;
import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.profile.ProfileAddressModel;

import java.util.List;

/**
 * Repository cho luồng Thanh toán, Lịch sử và Chi tiết đơn hàng.
 */
public class OrderRepository {

    private final MockStoreHelper store = MockStoreHelper.getInstance();

    @Nullable
    public ProductModel getProductById(@Nullable String productId) {
        return store.getProductById(productId);
    }

    public List<CartItemModel> getSelectedCartItems() {
        return store.getSelectedCartItems();
    }

    @Nullable
    public ProfileAddressModel getDefaultAddress() {
        return store.getDefaultAddress();
    }

    @Nullable
    public ProfileAddressModel findAddressById(@Nullable String addressId) {
        return store.findAddressById(addressId);
    }

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return store.getOrdersLiveData();
    }

    @Nullable
    public OrderModel getOrderByCode(@Nullable String orderCode) {
        return store.getOrderByCode(orderCode);
    }

    public OrderModel placeOrder(
            @NonNull List<CartItemModel> items,
            @NonNull ProfileAddressModel address,
            @NonNull String shippingMethod,
            long shippingFee,
            @NonNull String paymentMethod,
            long discount,
            boolean removeFromCart
    ) {
        return store.placeOrder(items, address, shippingMethod, shippingFee, paymentMethod, discount, removeFromCart);
    }

    public void cancelOrder(@NonNull String orderCode) {
        store.cancelOrder(orderCode);
    }

    public void reorder(@NonNull OrderModel order) {
        store.reorder(order);
    }
}
