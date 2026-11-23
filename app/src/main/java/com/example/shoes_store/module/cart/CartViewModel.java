package com.example.shoes_store.module.cart;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

/**
 * ViewModel cho tab Giỏ hàng.
 */
public class CartViewModel extends ViewModel {

    public static final long VOUCHER_DISCOUNT_AMOUNT = 150_000L;

    private final CartRepository repository = new CartRepository();
    private boolean voucherApplied = false;

    public LiveData<List<CartItemModel>> getCartItemsLiveData() {
        return repository.getCartItemsLiveData();
    }

    public boolean isVoucherApplied() {
        return voucherApplied;
    }

    public boolean toggleVoucher() {
        voucherApplied = !voucherApplied;
        return voucherApplied;
    }

    public void updateQuantity(@NonNull String cartItemId, int delta) {
        repository.updateQuantity(cartItemId, delta);
    }

    public void toggleItemSelected(@NonNull String cartItemId, boolean selected) {
        repository.toggleItemSelected(cartItemId, selected);
    }

    public void selectAll(boolean selected) {
        repository.selectAll(selected);
    }

    public void removeItem(@NonNull String cartItemId) {
        repository.removeItem(cartItemId);
    }
}
