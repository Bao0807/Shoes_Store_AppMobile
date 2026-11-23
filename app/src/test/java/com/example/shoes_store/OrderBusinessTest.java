package com.example.shoes_store;

import com.example.shoes_store.module.cart.CartVoucherModel;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.product.ProductVariantModel;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OrderBusinessTest {

    @Test
    public void variantStockCheckMatchesQuantity() {
        ProductVariantModel variant = new ProductVariantModel(
                "v_0_42", "p_novablast_5", "Trắng / Cam", "#FF5A36", "", "42", 2, "SKU-42"
        );
        assertTrue(variant.isInStock());
        assertTrue(variant.hasEnoughStock(2));
        assertFalse(variant.hasEnoughStock(3));
        assertFalse(variant.hasEnoughStock(0));
    }

    @Test
    public void voucherChecksQuotaExpiryAndMinSubtotal() {
        long futureMillis = System.currentTimeMillis() + 86_400_000L;
        CartVoucherModel voucher = new CartVoucherModel(
                "RUN150K", "-150K", "Title", "Desc", 150_000L, 2_000_000L, "", 10, 9, futureMillis, true
        );
        assertTrue(voucher.hasRemainingQuota());
        assertTrue(voucher.isEligible(2_500_000L));
        assertFalse(voucher.isEligible(1_500_000L));

        CartVoucherModel exhausted = new CartVoucherModel(
                "RUN150K", "-150K", "Title", "Desc", 150_000L, 2_000_000L, "", 10, 10, futureMillis, true
        );
        assertFalse(exhausted.hasRemainingQuota());
        assertFalse(exhausted.isEligible(3_000_000L));
    }

    @Test
    public void orderCancelAndPaymentStatusHelpersWork() {
        OrderModel pendingOrder = new OrderModel(
                "o1", "RN1", "u1", OrderModel.Status.PENDING, PaymentModel.Method.COD,
                Collections.emptyList(), "Addr", "Receiver", "Giao chuẩn", "",
                1_000_000L, 30_000L, 0L, 1_030_000L, 1000L, 1000L
        );
        assertTrue(pendingOrder.canCancel());
        pendingOrder.setStatus(OrderModel.Status.SHIPPING);
        assertFalse(pendingOrder.canCancel());

        PaymentModel paidPayment = new PaymentModel(
                "pay1", "o1", "u1", PaymentModel.Method.BANK_QR, PaymentModel.Status.PAID,
                1_030_000L, "TXN-1", 1000L, 1000L
        );
        assertTrue(paidPayment.isPaid());
    }

    @Test
    public void voucherIneligibleReasonDistinguishesAllCauses() {
        long futureMillis = System.currentTimeMillis() + 86_400_000L;
        long pastMillis = System.currentTimeMillis() - 86_400_000L;

        CartVoucherModel inactive = new CartVoucherModel(
                "OFF", "-100K", "Title", "Desc", 100_000L, 0L, "", 10, 0, futureMillis, false
        );
        assertEquals("Mã giảm giá hiện không còn áp dụng", inactive.getIneligibleReason(1_000_000L));

        CartVoucherModel expired = new CartVoucherModel(
                "EXP", "-100K", "Title", "Desc", 100_000L, 0L, "", 10, 0, pastMillis, true
        );
        assertEquals("Mã giảm giá đã hết hạn sử dụng", expired.getIneligibleReason(1_000_000L));

        CartVoucherModel exhausted = new CartVoucherModel(
                "FULL", "-100K", "Title", "Desc", 100_000L, 0L, "", 10, 10, futureMillis, true
        );
        assertEquals("Mã giảm giá đã hết lượt sử dụng", exhausted.getIneligibleReason(1_000_000L));

        CartVoucherModel minOrder = new CartVoucherModel(
                "MIN", "-150K", "Title", "Desc", 150_000L, 2_000_000L, "", 10, 1, futureMillis, true
        );
        String reason = minOrder.getIneligibleReason(1_500_000L);
        assertNotNull(reason);
        assertTrue(reason.contains("Mua thêm"));

        assertNull(minOrder.getIneligibleReason(2_500_000L));
    }
}
