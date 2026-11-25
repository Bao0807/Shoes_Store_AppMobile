package com.example.shoes_store.module.cart;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

/**
 * Chuyển đổi dữ liệu Firestore cho Giỏ hàng (`users/{uid}/cart`).
 */
public final class CartMapper {

    public static final String FIELD_CART_ITEM_ID = "cartItemId";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_PRODUCT_ID = "productId";
    public static final String FIELD_VARIANT_ID = "variantId";
    public static final String FIELD_SIZE = "size";
    public static final String FIELD_COLOR_NAME = "colorName";
    public static final String FIELD_QUANTITY = "quantity";
    public static final String FIELD_SELECTED = "selected";
    public static final String FIELD_UPDATED_AT = "updatedAt";

    private CartMapper() {
    }

    @NonNull
    public static Map<String, Object> cartItemToMap(
            @NonNull String cartItemId,
            @NonNull String uid,
            @NonNull String productId,
            @NonNull String variantId,
            @NonNull String size,
            @NonNull String colorName,
            int quantity,
            boolean selected,
            @NonNull Timestamp now
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_CART_ITEM_ID, cartItemId);
        data.put(FIELD_USER_ID, uid);
        data.put(FIELD_PRODUCT_ID, productId);
        data.put(FIELD_VARIANT_ID, variantId);
        data.put(FIELD_SIZE, size);
        data.put(FIELD_COLOR_NAME, colorName);
        data.put(FIELD_QUANTITY, Math.max(1, quantity));
        data.put(FIELD_SELECTED, selected);
        data.put(FIELD_UPDATED_AT, now);
        return data;
    }

    @NonNull
    public static Map<String, Object> cartQuantityUpdateMap(int quantity, @NonNull Timestamp now) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_QUANTITY, Math.max(1, quantity));
        updates.put(FIELD_UPDATED_AT, now);
        return updates;
    }

    @NonNull
    public static Map<String, Object> cartMergeUpdateMap(int quantity, @NonNull Timestamp now) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_QUANTITY, Math.max(1, quantity));
        updates.put(FIELD_SELECTED, true);
        updates.put(FIELD_UPDATED_AT, now);
        return updates;
    }
}
