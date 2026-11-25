package com.example.shoes_store.module.review;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.core.DateFormatUtil;

/**
 * Mẫu đánh giá từ người chạy bộ cho sản phẩm giày.
 */
public class ReviewModel {

    private final String productId;
    private final String userId;
    private final String orderId;
    private final String reviewerName;
    private final String reviewerAvatarUrl;
    private final float rating;
    private final String date;
    private final String comment;
    private final long createdAtMillis;

    public ReviewModel(
            @Nullable String productId,
            @Nullable String userId,
            @Nullable String orderId,
            @Nullable String reviewerName,
            @Nullable String reviewerAvatarUrl,
            float rating,
            @Nullable String date,
            @Nullable String comment,
            long createdAtMillis
    ) {
        this.productId = productId != null ? productId : "";
        this.userId = userId != null ? userId : "";
        this.orderId = orderId != null ? orderId : "";
        this.reviewerName = reviewerName != null ? reviewerName : "Runner";
        this.reviewerAvatarUrl = reviewerAvatarUrl != null ? reviewerAvatarUrl : "";
        this.rating = rating;
        this.createdAtMillis = createdAtMillis;
        if (date != null && !date.trim().isEmpty()) {
            this.date = date;
        } else if (createdAtMillis > 0L) {
            this.date = DateFormatUtil.formatDate(createdAtMillis);
        } else {
            this.date = "";
        }
        this.comment = comment != null ? comment : "";
    }

    @NonNull
    public String getProductId() {
        return productId;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    @NonNull
    public String getOrderId() {
        return orderId;
    }

    @NonNull
    public String getReviewerName() {
        return reviewerName;
    }

    @NonNull
    public String getReviewerAvatarUrl() {
        return reviewerAvatarUrl;
    }

    public float getRating() {
        return rating;
    }

    @NonNull
    public String getDate() {
        return date;
    }

    @NonNull
    public String getComment() {
        return comment;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }
}
