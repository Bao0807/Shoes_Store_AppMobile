package com.example.shoes_store.module.wishlist;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

/**
 * Chuyển đổi dữ liệu Firestore cho Danh sách yêu thích (`users/{uid}/wishlist`).
 */
public final class WishlistMapper {

    public static final String FIELD_PRODUCT_ID = "productId";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_ADDED_AT = "addedAt";

    private WishlistMapper() {
    }

    @NonNull
    public static Map<String, Object> toMap(
            @NonNull String productId,
            @NonNull String userId,
            @NonNull Timestamp addedAt
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_PRODUCT_ID, productId);
        data.put(FIELD_USER_ID, userId);
        data.put(FIELD_ADDED_AT, addedAt);
        return data;
    }
}
