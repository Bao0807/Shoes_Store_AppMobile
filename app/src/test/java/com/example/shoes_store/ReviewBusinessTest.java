package com.example.shoes_store;

import com.example.shoes_store.module.order.OrderItemModel;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.product.ProductVariantModel;
import com.example.shoes_store.module.review.ReviewModel;
import com.example.shoes_store.module.review.ReviewService;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReviewBusinessTest {

    @Test
    public void calculateAverageRatingHandlesEmptyNullAndRounding() {
        assertEquals(0.0, ReviewService.calculateAverageRating(null), 0.001);
        assertEquals(0.0, ReviewService.calculateAverageRating(Collections.emptyList()), 0.001);
        assertEquals(4.9, ReviewService.calculateAverageRating(Arrays.asList(5.0, 4.8, 4.9)), 0.001);
        assertEquals(4.8, ReviewService.calculateAverageRating(Arrays.asList(4.9, 4.7, 4.8)), 0.001);
    }

    @Test
    public void calculateAverageRatingFromReviewsHandlesNullEmptyAndValidReviews() {
        assertEquals(0.0, ReviewService.calculateAverageRatingFromReviews(null), 0.001);
        assertEquals(0.0, ReviewService.calculateAverageRatingFromReviews(Collections.emptyList()), 0.001);

        ReviewModel r1 = new ReviewModel("p1", "u1", "o1", "Bảo", "", 5.0f, "20/11/2026", "Êm", 1000L);
        ReviewModel r2 = new ReviewModel("p1", "u2", "o2", "Nam", "", 4.0f, "21/11/2026", "Ổn", 2000L);
        ReviewModel r3 = new ReviewModel("p1", "u3", "o3", "An", "", 5.0f, "22/11/2026", "Tốt", 3000L);

        assertEquals(5.0, ReviewService.calculateAverageRatingFromReviews(Collections.singletonList(r1)), 0.001);
        assertEquals(4.7, ReviewService.calculateAverageRatingFromReviews(Arrays.asList(r1, r2, r3)), 0.001);
    }

    @Test
    public void isVerifiedRealReviewRejectsSeedAndOrderlessReviews() {
        ReviewModel seedReview = new ReviewModel(
                "p_novablast_5", ReviewService.LEGACY_SEED_USER_ID, "",
                "Hoàng Nam", "", 5.0f, "20/11/2026", "Rất êm", 1000L
        );
        assertFalse(ReviewService.isVerifiedRealReview(seedReview));

        ReviewModel noOrderReview = new ReviewModel(
                "p_novablast_5", "real_uid_1", "",
                "Bảo", "", 5.0f, "20/11/2026", "Tốt", 1000L
        );
        assertFalse(ReviewService.isVerifiedRealReview(noOrderReview));

        ReviewModel realReview = new ReviewModel(
                "p_novablast_5", "real_uid_1", "ord_1",
                "Bảo", "", 5.0f, "23/11/2026", "Chạy rất êm", 2000L
        );
        assertTrue(ReviewService.isVerifiedRealReview(realReview));
    }

    @Test
    public void validateReviewInputChecksUidOrderRatingAndComment() {
        assertNotNull(ReviewService.validateReviewInput(null, "ord_1", 5f, "Tốt"));
        assertNotNull(ReviewService.validateReviewInput("u1", " ", 5f, "Tốt"));
        assertNotNull(ReviewService.validateReviewInput("u1", "ord_1", 0f, "Tốt"));
        assertNotNull(ReviewService.validateReviewInput("u1", "ord_1", 6f, "Tốt"));
        assertNotNull(ReviewService.validateReviewInput("u1", "ord_1", 5f, "   "));
        assertNull(ReviewService.validateReviewInput("u1", "ord_1", 4.5f, "Giày nhẹ và êm"));
    }

    @Test
    public void findEligibleOrderIdAllowsOnlyCompletedUnreviewedOrderForProduct() {
        OrderItemModel novablast = new OrderItemModel(
                "p_novablast_5", "v_0_42", "ASICS Novablast 5", "", 0, "42", "Trắng", 3_490_000L, 1, 3_490_000L
        );
        OrderItemModel alphafly = new OrderItemModel(
                "p_alphafly_3", "v_0_42", "Nike Alphafly 3", "", 0, "42", "Volt", 7_490_000L, 1, 7_490_000L
        );

        OrderModel pending = buildOrder("ord_pending", "uid_1", OrderModel.Status.PENDING, novablast);
        OrderModel shipping = buildOrder("ord_shipping", "uid_1", OrderModel.Status.SHIPPING, novablast);
        OrderModel cancelled = buildOrder("ord_cancel", "uid_1", OrderModel.Status.CANCELLED, novablast);
        OrderModel wrongProduct = buildOrder("ord_other_shoe", "uid_1", OrderModel.Status.COMPLETED, alphafly);
        OrderModel wrongUser = buildOrder("ord_other_user", "uid_2", OrderModel.Status.COMPLETED, novablast);
        OrderModel completed = buildOrder("ord_done", "uid_1", OrderModel.Status.COMPLETED, novablast);

        List<OrderModel> orders = Arrays.asList(pending, shipping, cancelled, wrongProduct, wrongUser, completed);

        assertEquals(
                "ord_done",
                ReviewService.findEligibleOrderIdForReview("uid_1", "p_novablast_5", orders, Collections.emptyList())
        );

        ReviewModel alreadyReviewed = new ReviewModel(
                "p_novablast_5", "uid_1", "ord_done",
                "Bảo", "", 5.0f, "23/11/2026", "Đã nhận giày", 3000L
        );
        assertNull(ReviewService.findEligibleOrderIdForReview(
                "uid_1", "p_novablast_5", orders, Collections.singletonList(alreadyReviewed)
        ));
        assertTrue(ReviewService.hasUserReviewedProduct(
                "uid_1", Collections.singletonList(alreadyReviewed)
        ));
        assertNull(ReviewService.findEligibleOrderIdForReview(
                null, "p_novablast_5", orders, Collections.emptyList()
        ));
    }

    @Test
    public void secondCompletedOrderForSameProductIsEligibleAndGeneratesDistinctDocId() {
        OrderItemModel novablast = new OrderItemModel(
                "p_novablast_5", "v_0_42", "ASICS Novablast 5", "", 0, "42", "Trắng", 3_490_000L, 1, 3_490_000L
        );
        OrderModel order1 = buildOrder("ord_1", "uid_1", OrderModel.Status.COMPLETED, novablast);
        OrderModel order2 = buildOrder("ord_2", "uid_1", OrderModel.Status.COMPLETED, novablast);

        ReviewModel review1 = new ReviewModel(
                "p_novablast_5", "uid_1", "ord_1",
                "Bảo", "", 5.0f, "23/11/2026", "Đôi đầu chạy cực êm", 3000L
        );

        List<ReviewModel> afterFirstReview = Collections.singletonList(review1);
        assertNull(ReviewService.findEligibleOrderIdForReview(
                "uid_1", "p_novablast_5", Collections.singletonList(order1), afterFirstReview
        ));
        assertEquals(
                "ord_2",
                ReviewService.findEligibleOrderIdForReview(
                        "uid_1", "p_novablast_5", Arrays.asList(order2, order1), afterFirstReview
                )
        );
        assertNotEquals(
                ReviewService.buildReviewDocumentId("ord_1", "uid_1"),
                ReviewService.buildReviewDocumentId("ord_2", "uid_1")
        );

        ReviewModel review2 = new ReviewModel(
                "p_novablast_5", "uid_1", "ord_2",
                "Bảo", "", 4.0f, "24/11/2026", "Mua thêm đôi thứ 2 vẫn ưng", 4000L
        );
        List<ReviewModel> afterBothReviews = Arrays.asList(review2, review1);
        assertNull(ReviewService.findEligibleOrderIdForReview(
                "uid_1", "p_novablast_5", Arrays.asList(order2, order1), afterBothReviews
        ));
        assertEquals(4.5, ReviewService.calculateAverageRating(Arrays.asList(
                (double) review1.getRating(),
                (double) review2.getRating()
        )), 0.001);
    }

    private OrderModel buildOrder(String orderId, String uid, OrderModel.Status status, OrderItemModel item) {
        return new OrderModel(
                orderId, orderId, uid, status, PaymentModel.Method.COD,
                Collections.singletonList(item), "Q1 TP.HCM", "Bảo · 0901", "Giao chuẩn", "",
                item.getLineTotal(), 30_000L, 0L, item.getLineTotal() + 30_000L, 1000L
        );
    }

    @Test
    public void productWithReviewStatsPreservesFieldsAndVariants() {
        ProductModel original = new ProductModel(
                "p_novablast_5", "Novablast 5", "ASICS", "Daily",
                3_490_000L, 3_890_000L, 4.9, 128, 0, "https://example.com/a.jpg",
                Collections.singletonList("https://example.com/a.jpg"),
                "255g", "8 mm", "Cao", "Road", "Mô tả",
                Collections.singletonList("Cam"), Collections.singletonList("#FF5A36"),
                Collections.singletonList("42"), Collections.emptySet(),
                true, 1000L
        );
        ProductVariantModel variant = new ProductVariantModel(
                "v_0_42", "p_novablast_5", "Cam", "#FF5A36", "", "42", 8
        );
        original.setVariants(Collections.singletonList(variant));

        ProductModel updated = original.withReviewStats(0.0, 0);
        assertEquals("p_novablast_5", updated.getId());
        assertEquals("Novablast 5", updated.getName());
        assertEquals(0.0, updated.getRating(), 0.001);
        assertEquals(0, updated.getReviewCount());
        assertEquals(1, updated.getVariants().size());
        assertEquals("v_0_42", updated.getVariants().get(0).getVariantId());
    }
}
