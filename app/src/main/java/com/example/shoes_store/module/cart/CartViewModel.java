package com.example.shoes_store.module.cart;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.core.ResourceCallback;

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

    public LiveData<List<CartVoucherModel>> getVouchersLiveData() {
        return repository.getVouchersLiveData();
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

    public void syncSelectedVoucherWithLatest(@Nullable List<CartVoucherModel> latestVouchers) {
        if (selectedVoucher == null) {
            return;
        }
        if (latestVouchers == null || latestVouchers.isEmpty()) {
            selectedVoucher = null;
            return;
        }
        String code = selectedVoucher.getCode();
        for (CartVoucherModel latest : latestVouchers) {
            if (code.equalsIgnoreCase(latest.getCode())) {
                selectedVoucher = (latest.isActive() && latest.isNotExpired() && latest.hasRemainingQuota())
                        ? latest
                        : null;
                return;
            }
        }
        selectedVoucher = null;
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

    public void updateQuantity(
            @NonNull CartItemModel item,
            int delta,
            @Nullable ResourceCallback<Void> callback
    ) {
        repository.updateQuantity(item, delta, callback);
    }

    public void toggleItemSelected(@NonNull String cartItemId, boolean selected) {
        repository.toggleItemSelected(cartItemId, selected);
    }

    public void selectAll(@Nullable List<CartItemModel> items, boolean selected) {
        repository.selectAll(items, selected);
    }

    public void removeItem(@NonNull String cartItemId) {
        repository.removeItem(cartItemId);
    }
}
