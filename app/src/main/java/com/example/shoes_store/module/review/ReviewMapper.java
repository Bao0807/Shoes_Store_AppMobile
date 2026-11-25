package com.example.shoes_store.module.review;

import androidx.annotation.NonNull;

import com.example.shoes_store.core.FirestoreReadUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Chuyển đổi dữ liệu Firestore cho Đánh giá sản phẩm (`reviews`).
 */
public final class ReviewMapper {

    public static final String FIELD_REVIEW_ID = "reviewId";
    public static final String FIELD_PRODUCT_ID = "productId";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_ORDER_ID = "orderId";
    public static final String FIELD_REVIEWER_NAME = "reviewerName";
    public static final String FIELD_REVIEWER_AVATAR_URL = "reviewerAvatarUrl";
    public static final String FIELD_RATING = "rating";
    public static final String FIELD_COMMENT = "comment";
    public static final String FIELD_CREATED_AT = "createdAt";

    private ReviewMapper() {
    }

    @NonNull
    public static ReviewModel fromSnapshot(@NonNull DocumentSnapshot doc, @NonNull String fallbackProductId) {
        return new ReviewModel(
                FirestoreReadUtil.readString(doc, FIELD_PRODUCT_ID, fallbackProductId),
                FirestoreReadUtil.readString(doc, FIELD_USER_ID, ""),
                FirestoreReadUtil.readString(doc, FIELD_ORDER_ID, ""),
                FirestoreReadUtil.readString(doc, FIELD_REVIEWER_NAME, "Runner"),
                FirestoreReadUtil.readString(doc, FIELD_REVIEWER_AVATAR_URL, ""),
                (float) FirestoreReadUtil.readDouble(doc, FIELD_RATING, 5.0),
                "",
                FirestoreReadUtil.readString(doc, FIELD_COMMENT, ""),
                FirestoreReadUtil.readMillis(doc, FIELD_CREATED_AT)
        );
    }

    @NonNull
    public static Map<String, Object> toMap(
            @NonNull String reviewId,
            @NonNull String productId,
            @NonNull String uid,
            @NonNull String orderId,
            @NonNull String reviewerName,
            @NonNull String reviewerAvatarUrl,
            float rating,
            @NonNull String comment,
            @NonNull Timestamp now
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_REVIEW_ID, reviewId);
        data.put(FIELD_PRODUCT_ID, productId);
        data.put(FIELD_USER_ID, uid);
        data.put(FIELD_ORDER_ID, orderId);
        data.put(FIELD_REVIEWER_NAME, reviewerName);
        data.put(FIELD_REVIEWER_AVATAR_URL, reviewerAvatarUrl);
        data.put(FIELD_RATING, (double) rating);
        data.put(FIELD_COMMENT, comment);
        data.put(FIELD_CREATED_AT, now);
        return data;
    }
}
