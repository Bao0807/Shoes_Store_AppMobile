package com.example.shoes_store.module.order;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.cart.CartRepository;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.product.ProductRepository;
import com.example.shoes_store.module.profile.ProfileAddressModel;
import com.example.shoes_store.module.profile.ProfileRepository;
import com.example.shoes_store.module.voucher.VoucherModel;
import com.example.shoes_store.module.voucher.VoucherRepository;

import java.util.List;

/**
 * ViewModel cho Đặt hàng và Quản lý đơn hàng.
 */
public class OrderViewModel extends ViewModel {

    private final OrderRepository repository = OrderRepository.getInstance();
    private final ProductRepository productRepository = ProductRepository.getInstance();
    private final CartRepository cartRepository = CartRepository.getInstance();
    private final ProfileRepository profileRepository = ProfileRepository.getInstance();
    private final VoucherRepository voucherRepository = VoucherRepository.getInstance();

    @Nullable
    public ProductModel getProductById(@Nullable String productId) {
        return productRepository.getProductById(productId);
    }

    public List<CartItemModel> getSelectedCartItems() {
        return cartRepository.getSelectedCartItems();
    }

    @Nullable
    public ProfileAddressModel getDefaultAddress() {
        return profileRepository.getDefaultAddress();
    }

    @Nullable
    public ProfileAddressModel findAddressById(@Nullable String addressId) {
        return profileRepository.findAddressById(addressId);
    }

    public LiveData<List<ProfileAddressModel>> getAddressesLiveData() {
        return profileRepository.getAddressesLiveData();
    }

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return repository.getOrdersLiveData();
    }

    @Nullable
    public OrderModel getOrderByCode(@Nullable String orderIdOrCode) {
        return repository.getOrderByIdOrCode(orderIdOrCode);
    }

    public void placeOrder(
            @NonNull List<CartItemModel> items,
            @NonNull ProfileAddressModel address,
            @NonNull String shippingMethod,
            long shippingFee,
            @NonNull PaymentModel.Method paymentMethod,
            @Nullable String voucherCode,
            long discount,
            boolean removeFromCart,
            @NonNull ResourceCallback<OrderModel> callback
    ) {
        repository.placeOrder(
                items,
                address,
                shippingMethod,
                shippingFee,
                paymentMethod,
                voucherCode,
                discount,
                removeFromCart,
                callback
        );
    }

    public void cancelOrder(@NonNull String orderId, @NonNull ResourceCallback<Void> callback) {
        repository.cancelOrder(orderId, callback);
    }

    public void reorder(@NonNull OrderModel order, @NonNull ResourceCallback<Integer> callback) {
        repository.reorder(order, callback);
    }

    public List<VoucherModel> getVouchers() {
        return voucherRepository.getVouchers();
    }

    public LiveData<List<VoucherModel>> getVouchersLiveData() {
        return voucherRepository.getVouchersLiveData();
    }

    @Nullable
    public VoucherModel findVoucherByCode(@Nullable String code) {
        return voucherRepository.findByCode(code);
    }
}
