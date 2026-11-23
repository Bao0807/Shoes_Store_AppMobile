package com.example.shoes_store.module.product;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.module.order.OrderItemModel;
import com.example.shoes_store.module.order.OrderModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tiện ích kiểm tra điều kiện đánh giá hợp lệ (chỉ người dùng thực có đơn COMPLETED chứa sản phẩm)
 * và tính điểm trung bình đánh giá.
 */
public final class ProductReviewHelper {

    public static final String LEGACY_SEED_USER_ID = "seed_runner";

    private ProductReviewHelper() {
    }

    public static double calculateAverageRating(@Nullable List<Double> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Double r : ratings) {
            if (r != null) {
                sum += r;
            }
        }
        return Math.round((sum / ratings.size()) * 10.0) / 10.0;
    }

    public static boolean isVerifiedRealReview(@Nullable ProductReviewModel review) {
        if (review == null) {
            return false;
        }
        String userId = review.getUserId().trim();
        String orderId = review.getOrderId().trim();
        return !userId.isEmpty()
                && !LEGACY_SEED_USER_ID.equalsIgnoreCase(userId)
                && !orderId.isEmpty();
    }

    @Nullable
    public static String findEligibleOrderIdForReview(
            @Nullable String uid,
            @NonNull String productId,
            @Nullable List<OrderModel> orders,
            @Nullable List<ProductReviewModel> existingReviews
    ) {
        if (uid == null || uid.trim().isEmpty() || productId.trim().isEmpty() || orders == null) {
            return null;
        }
        Set<String> reviewedOrderIds = new HashSet<>();
        if (existingReviews != null) {
            for (ProductReviewModel rev : existingReviews) {
                if (isVerifiedRealReview(rev) && uid.equals(rev.getUserId())) {
                    reviewedOrderIds.add(rev.getOrderId());
                }
            }
        }

        for (OrderModel order : orders) {
            if (order == null) {
                continue;
            }
            if (!uid.equals(order.getUserId())
                    || order.getStatus() != OrderModel.Status.COMPLETED
                    || reviewedOrderIds.contains(order.getOrderId())) {
                continue;
            }
            for (OrderItemModel item : order.getOrderItems()) {
                if (productId.equals(item.getProductId())) {
                    return order.getOrderId();
                }
            }
        }
        return null;
    }

    public static boolean hasUserReviewedProduct(
            @Nullable String uid,
            @Nullable List<ProductReviewModel> existingReviews
    ) {
        if (uid == null || uid.trim().isEmpty() || existingReviews == null) {
            return false;
        }
        for (ProductReviewModel rev : existingReviews) {
            if (isVerifiedRealReview(rev) && uid.equals(rev.getUserId())) {
                return true;
            }
        }
        return false;
    }
}
