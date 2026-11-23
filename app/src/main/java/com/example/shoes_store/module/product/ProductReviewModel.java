package com.example.shoes_store.module.product;

/**
 * Mẫu đánh giá từ người chạy bộ cho sản phẩm giày.
 */
public class ProductReviewModel {

    private final String reviewerName;
    private final float rating;
    private final String date;
    private final String comment;

    public ProductReviewModel(String reviewerName, float rating, String date, String comment) {
        this.reviewerName = reviewerName;
        this.rating = rating;
        this.date = date;
        this.comment = comment;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public float getRating() {
        return rating;
    }

    public String getDate() {
        return date;
    }

    public String getComment() {
        return comment;
    }
}
