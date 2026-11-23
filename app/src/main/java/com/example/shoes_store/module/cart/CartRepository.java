package com.example.shoes_store.module.cart;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.shoes_store.core.MockStoreHelper;

import java.util.List;

/**
 * Repository quản lý dữ liệu Giỏ hàng và danh sách Voucher.
 */
public class CartRepository {

    private final MockStoreHelper store = MockStoreHelper.getInstance();

    public LiveData<List<CartItemModel>> getCartItemsLiveData() {
        return store.getCartItemsLiveData();
    }

    public List<CartVoucherModel> getVouchers() {
        return store.getVouchers();
    }

    public void updateQuantity(@NonNull String cartItemId, int delta) {
        store.updateCartQuantity(cartItemId, delta);
    }

    public void toggleItemSelected(@NonNull String cartItemId, boolean selected) {
        store.toggleCartItemSelected(cartItemId, selected);
    }

    public void selectAll(boolean selected) {
        store.selectAllCartItems(selected);
    }

    public void removeItem(@NonNull String cartItemId) {
        store.removeCartItem(cartItemId);
    }
}
