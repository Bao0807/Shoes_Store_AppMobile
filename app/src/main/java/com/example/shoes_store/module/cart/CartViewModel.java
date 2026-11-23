package com.example.shoes_store.module.cart;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

/**
 * ViewModel cho tab Giỏ hàng và chọn Voucher.
 */
public class CartViewModel extends ViewModel {

    private final CartRepository repository = new CartRepository();
    @Nullable
    private CartVoucherModel selectedVoucher;

    public LiveData<List<CartItemModel>> getCartItemsLiveData() {
        return repository.getCartItemsLiveData();
    }

    public List<CartVoucherModel> getAvailableVouchers() {
        return repository.getVouchers();
    }

    @Nullable
    public CartVoucherModel getSelectedVoucher() {
        return selectedVoucher;
    }

    public void setSelectedVoucher(@Nullable CartVoucherModel voucher) {
        this.selectedVoucher = voucher;
    }

    public long calculateEffectiveDiscount(long subtotal) {
        if (selectedVoucher == null) {
            return 0L;
        }
        if (!selectedVoucher.isEligible(subtotal)) {
            selectedVoucher = null;
            return 0L;
        }
        return Math.min(selectedVoucher.getDiscountAmount(), subtotal);
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
