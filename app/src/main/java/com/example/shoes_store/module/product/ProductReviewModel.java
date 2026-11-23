package com.example.shoes_store.module.product;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Mẫu đánh giá từ người chạy bộ cho sản phẩm giày.
 */
public class ProductReviewModel {

    private final String reviewId;
    private final String productId;
    private final String userId;
    private final String orderId;
    private final String reviewerName;
    private final String reviewerAvatarUrl;
    private final float rating;
    private final String date;
    private final String comment;
    private final long createdAtMillis;

    public ProductReviewModel(String reviewerName, float rating, String date, String comment) {
        this("", "", "", "", reviewerName, "", rating, date, comment, 0L);
    }

    public ProductReviewModel(
            @Nullable String reviewId,
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
        this.reviewId = reviewId != null ? reviewId : "";
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
            this.date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(createdAtMillis));
        } else {
            this.date = "";
        }
        this.comment = comment != null ? comment : "";
    }

    @NonNull
    public String getReviewId() {
        return reviewId;
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
