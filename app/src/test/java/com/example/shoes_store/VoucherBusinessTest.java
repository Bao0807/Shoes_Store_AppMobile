package com.example.shoes_store;

import com.example.shoes_store.module.voucher.VoucherModel;
import com.example.shoes_store.module.voucher.VoucherService;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VoucherBusinessTest {

    @Test
    public void voucherServiceEvaluatesIneligibleReasons() {
        long futureMillis = System.currentTimeMillis() + 86_400_000L;
        long pastMillis = System.currentTimeMillis() - 86_400_000L;

        VoucherModel inactive = new VoucherModel(
                "OFF", "-100K", "Title", "Desc", 100_000L, 0L, "", 10, 0, futureMillis, false
        );
        assertEquals("Mã giảm giá hiện không còn áp dụng", VoucherService.getIneligibleReason(inactive, 1_000_000L));

        VoucherModel expired = new VoucherModel(
                "EXP", "-100K", "Title", "Desc", 100_000L, 0L, "", 10, 0, pastMillis, true
        );
        assertEquals("Mã giảm giá đã hết hạn sử dụng", VoucherService.getIneligibleReason(expired, 1_000_000L));

        VoucherModel exhausted = new VoucherModel(
                "FULL", "-100K", "Title", "Desc", 100_000L, 0L, "", 10, 10, futureMillis, true
        );
        assertEquals("Mã giảm giá đã hết lượt sử dụng", VoucherService.getIneligibleReason(exhausted, 1_000_000L));
        assertFalse(exhausted.hasRemainingQuota());
    }

    @Test
    public void voucherServiceResolvesDiscountAndValidatesTransactionSnapshot() {
        long futureMillis = System.currentTimeMillis() + 86_400_000L;
        VoucherModel valid = new VoucherModel(
                "RUN150K", "-150K", "Title", "Desc", 150_000L, 2_000_000L, "", 10, 1, futureMillis, true
        );
        assertTrue(valid.isEligible(2_500_000L));
        assertEquals(150_000L, VoucherService.resolveDiscountAmount(valid, 2_500_000L));
        assertEquals(0L, VoucherService.resolveDiscountAmount(valid, 1_500_000L));

        assertNull(VoucherService.validateVoucherSnapshotForTransaction(
                true, futureMillis, System.currentTimeMillis(), 2_000_000L, 10, 1, 2_500_000L, "RUN150K"
        ));
        assertNotNull(VoucherService.validateVoucherSnapshotForTransaction(
                true, futureMillis, System.currentTimeMillis(), 2_000_000L, 10, 10, 2_500_000L, "RUN150K"
        ));
    }
}
