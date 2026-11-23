package com.example.shoes_store.module.order;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.cart.CartVoucherModel;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.profile.ProfileAddressModel;

import java.util.List;

/**
 * ViewModel cho Đặt hàng và Quản lý đơn hàng.
 */
public class OrderViewModel extends ViewModel {

    private final OrderRepository repository = new OrderRepository();

    @Nullable
    public ProductModel getProductById(@Nullable String productId) {
        return repository.getProductById(productId);
    }

    public List<CartItemModel> getSelectedCartItems() {
        return repository.getSelectedCartItems();
    }

    @Nullable
    public ProfileAddressModel getDefaultAddress() {
        return repository.getDefaultAddress();
    }

    @Nullable
    public ProfileAddressModel findAddressById(@Nullable String addressId) {
        return repository.findAddressById(addressId);
    }

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return repository.getOrdersLiveData();
    }

    @Nullable
    public OrderModel getOrderByCode(@Nullable String orderCode) {
        return repository.getOrderByCode(orderCode);
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
        return repository.placeOrder(items, address, shippingMethod, shippingFee, paymentMethod, discount, removeFromCart);
    }

    public void cancelOrder(@NonNull String orderCode) {
        repository.cancelOrder(orderCode);
    }

    public void reorder(@NonNull OrderModel order) {
        repository.reorder(order);
    }

    public List<CartVoucherModel> getVouchers() {
        return repository.getVouchers();
    }

    @Nullable
    public CartVoucherModel findVoucherByCode(@Nullable String code) {
        return repository.findVoucherByCode(code);
    }
}
