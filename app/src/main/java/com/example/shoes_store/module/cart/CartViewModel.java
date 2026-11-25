package com.example.shoes_store.module.cart;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.voucher.VoucherModel;
import com.example.shoes_store.module.voucher.VoucherRepository;
import com.example.shoes_store.module.voucher.VoucherService;

import java.util.List;

/**
 * ViewModel cho tab Giỏ hàng và chọn Voucher.
 */
public class CartViewModel extends ViewModel {

    private final CartRepository repository = CartRepository.getInstance();
    private final VoucherRepository voucherRepository = VoucherRepository.getInstance();
    @Nullable
    private VoucherModel selectedVoucher;

    public LiveData<List<CartItemModel>> getCartItemsLiveData() {
        return repository.getCartItemsLiveData();
    }

    public LiveData<List<VoucherModel>> getVouchersLiveData() {
        return voucherRepository.getVouchersLiveData();
    }

    public List<VoucherModel> getAvailableVouchers() {
        return voucherRepository.getVouchers();
    }

    @Nullable
    public VoucherModel getSelectedVoucher() {
        return selectedVoucher;
    }

    public void setSelectedVoucher(@Nullable VoucherModel voucher) {
        this.selectedVoucher = voucher;
    }

    public void syncSelectedVoucherWithLatest(@Nullable List<VoucherModel> latestVouchers) {
        if (selectedVoucher == null) {
            return;
        }
        if (latestVouchers == null || latestVouchers.isEmpty()) {
            selectedVoucher = null;
            return;
        }
        String code = selectedVoucher.getCode();
        for (VoucherModel latest : latestVouchers) {
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
        long discount = VoucherService.resolveDiscountAmount(selectedVoucher, subtotal);
        if (selectedVoucher != null && !selectedVoucher.isEligible(subtotal)) {
            selectedVoucher = null;
        }
        return discount;
    }

    public void updateQuantity(
            @NonNull CartItemModel item,
            int delta,
            @Nullable ResourceCallback<Void> callback
    ) {
        repository.updateQuantity(item, delta, callback);
    }

    public void toggleItemSelected(
            @NonNull String cartItemId,
            boolean selected,
            @Nullable ResourceCallback<Void> callback
    ) {
        repository.toggleItemSelected(cartItemId, selected, callback);
    }

    public void selectAll(
            @Nullable List<CartItemModel> items,
            boolean selected,
            @Nullable ResourceCallback<Void> callback
    ) {
        repository.selectAll(items, selected, callback);
    }

    public void removeItem(@NonNull String cartItemId, @Nullable ResourceCallback<Void> callback) {
        repository.removeItem(cartItemId, callback);
    }
}
