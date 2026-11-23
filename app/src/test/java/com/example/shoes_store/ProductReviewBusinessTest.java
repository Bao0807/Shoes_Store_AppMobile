package com.example.shoes_store;

import com.example.shoes_store.module.order.OrderItemModel;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.product.ProductReviewHelper;
import com.example.shoes_store.module.product.ProductReviewModel;
import com.example.shoes_store.module.product.ProductVariantModel;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProductReviewBusinessTest {

    @Test
    public void calculateAverageRatingHandlesEmptyNullAndRounding() {
        assertEquals(0.0, ProductReviewHelper.calculateAverageRating(null), 0.001);
        assertEquals(0.0, ProductReviewHelper.calculateAverageRating(Collections.emptyList()), 0.001);
        assertEquals(4.9, ProductReviewHelper.calculateAverageRating(Arrays.asList(5.0, 4.8, 4.9)), 0.001);
        assertEquals(4.8, ProductReviewHelper.calculateAverageRating(Arrays.asList(4.9, 4.7, 4.8)), 0.001);
    }

    @Test
    public void isVerifiedRealReviewRejectsSeedAndOrderlessReviews() {
        ProductReviewModel seedReview = new ProductReviewModel(
                "rev_1", "p_novablast_5", ProductReviewHelper.LEGACY_SEED_USER_ID, "",
                "Hoàng Nam", "", 5.0f, "20/11/2026", "Rất êm", 1000L
        );
        assertFalse(ProductReviewHelper.isVerifiedRealReview(seedReview));

        ProductReviewModel noOrderReview = new ProductReviewModel(
                "rev_2", "p_novablast_5", "real_uid_1", "",
                "Bảo", "", 5.0f, "20/11/2026", "Tốt", 1000L
        );
        assertFalse(ProductReviewHelper.isVerifiedRealReview(noOrderReview));

        ProductReviewModel realReview = new ProductReviewModel(
                "rev_3", "p_novablast_5", "real_uid_1", "ord_1",
                "Bảo", "", 5.0f, "23/11/2026", "Chạy rất êm", 2000L
        );
        assertTrue(ProductReviewHelper.isVerifiedRealReview(realReview));
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
                ProductReviewHelper.findEligibleOrderIdForReview("uid_1", "p_novablast_5", orders, Collections.emptyList())
        );

        ProductReviewModel alreadyReviewed = new ProductReviewModel(
                "rev_done", "p_novablast_5", "uid_1", "ord_done",
                "Bảo", "", 5.0f, "23/11/2026", "Đã nhận giày", 3000L
        );
        assertNull(ProductReviewHelper.findEligibleOrderIdForReview(
                "uid_1", "p_novablast_5", orders, Collections.singletonList(alreadyReviewed)
        ));
        assertTrue(ProductReviewHelper.hasUserReviewedProduct(
                "uid_1", Collections.singletonList(alreadyReviewed)
        ));
        assertNull(ProductReviewHelper.findEligibleOrderIdForReview(
                null, "p_novablast_5", orders, Collections.emptyList()
        ));
    }

    private OrderModel buildOrder(String orderId, String uid, OrderModel.Status status, OrderItemModel item) {
        return new OrderModel(
                orderId, orderId, uid, status, PaymentModel.Method.COD,
                Collections.singletonList(item), "Q1 TP.HCM", "Bảo · 0901", "Giao chuẩn", "",
                item.getLineTotal(), 30_000L, 0L, item.getLineTotal() + 30_000L, 1000L, 1000L
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
                Collections.singletonList("42"), Collections.emptySet(), Collections.emptyList(),
                true, 1000L
        );
        ProductVariantModel variant = new ProductVariantModel(
                "v_0_42", "p_novablast_5", "Cam", "#FF5A36", "", "42", 8, "SKU"
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
