package com.example.shoes_store;

import com.example.shoes_store.module.voucher.VoucherModel;
import com.example.shoes_store.module.voucher.VoucherService;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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

    @Test
    public void resolveDiscountAmountHandlesNullIneligibleAndCapsAtSubtotal() {
        long futureMillis = System.currentTimeMillis() + 86_400_000L;
        long pastMillis = System.currentTimeMillis() - 86_400_000L;

        assertEquals(0L, VoucherService.resolveDiscountAmount(null, 1_000_000L));

        VoucherModel ineligibleByMinOrder = new VoucherModel(
                "MIN2M", "-150K", "Title", "Desc", 150_000L, 2_000_000L, "", 10, 0, futureMillis, true
        );
        assertEquals(0L, VoucherService.resolveDiscountAmount(ineligibleByMinOrder, 1_500_000L));
        assertEquals(0L, VoucherService.resolveDiscountAmount(ineligibleByMinOrder, 0L));

        VoucherModel expired = new VoucherModel(
                "EXP", "-100K", "Title", "Desc", 100_000L, 0L, "", 10, 0, pastMillis, true
        );
        assertEquals(0L, VoucherService.resolveDiscountAmount(expired, 500_000L));

        VoucherModel inactive = new VoucherModel(
                "OFF", "-100K", "Title", "Desc", 100_000L, 0L, "", 10, 0, futureMillis, false
        );
        assertEquals(0L, VoucherService.resolveDiscountAmount(inactive, 500_000L));

        VoucherModel hugeDiscount = new VoucherModel(
                "BIG500K", "-500K", "Title", "Desc", 500_000L, 0L, "", 10, 0, futureMillis, true
        );
        assertEquals(300_000L, VoucherService.resolveDiscountAmount(hugeDiscount, 300_000L));
    }

    @Test
    public void findByCodeTrimsAndIgnoresCase() {
        long futureMillis = System.currentTimeMillis() + 86_400_000L;
        VoucherModel run150 = new VoucherModel(
                "RUN150K", "-150K", "Giảm 150K", "Đơn từ 2M", 150_000L, 2_000_000L, "31/12/2026", 10, 2, futureMillis, true
        );
        VoucherModel freeShip = new VoucherModel(
                "FREESHIP", "-30K", "Miễn phí ship", "Mọi đơn", 30_000L, 0L, "", 0, 0, futureMillis, true
        );
        List<VoucherModel> vouchers = Arrays.asList(run150, null, freeShip);

        assertNull(VoucherService.findByCode(null, "RUN150K"));
        assertNull(VoucherService.findByCode(Collections.emptyList(), "RUN150K"));
        assertNull(VoucherService.findByCode(vouchers, null));
        assertNull(VoucherService.findByCode(vouchers, "   "));
        assertNull(VoucherService.findByCode(vouchers, "NOTFOUND"));

        assertSame(run150, VoucherService.findByCode(vouchers, "RUN150K"));
        assertSame(freeShip, VoucherService.findByCode(vouchers, "  freeship  "));
    }

    @Test
    public void formatVoucherDescriptionsAndWalletHandleAllEdgeCases() {
        long futureMillis = System.currentTimeMillis() + 86_400_000L;
        VoucherModel fullMeta = new VoucherModel(
                "RUN150K", "-150K", "Giảm 150K", "Đơn từ 2M", 150_000L, 2_000_000L, "31/12/2026", 10, 3, futureMillis, true
        );
        assertEquals("HSD: 31/12/2026 · Còn 7 lượt", VoucherService.formatExpiryAndQuotaMeta(fullMeta));
        assertEquals("Giảm 150.000₫ · Đơn tối thiểu 2.000.000₫", VoucherService.formatAdminSummary(fullMeta));
        assertEquals("Đã dùng 3/10 lượt · HSD: 31/12/2026", VoucherService.formatAdminQuotaAndExpiry(fullMeta));

        VoucherModel exhaustedNoExpiry = new VoucherModel(
                "FULL", "-50K", "Hết lượt", "Mô tả", 50_000L, 0L, "", 5, 8, 0L, true
        );
        assertEquals("Còn 0 lượt", VoucherService.formatExpiryAndQuotaMeta(exhaustedNoExpiry));
        assertEquals("Đã dùng 8/5 lượt", VoucherService.formatAdminQuotaAndExpiry(exhaustedNoExpiry));

        VoucherModel unlimitedWithExpiry = new VoucherModel(
                "UNLIM", "-30K", "Không giới hạn", "Mọi đơn", 30_000L, 0L, "01/01/2027", 0, 12, 0L, true
        );
        assertEquals("HSD: 01/01/2027", VoucherService.formatExpiryAndQuotaMeta(unlimitedWithExpiry));
        assertEquals("Đã dùng 12 lượt (Không giới hạn) · HSD: 01/01/2027", VoucherService.formatAdminQuotaAndExpiry(unlimitedWithExpiry));

        VoucherModel unlimitedNoExpiry = new VoucherModel(
                "PLAIN", "-20K", "Cơ bản", "Mọi đơn", 20_000L, 0L, "", 0, 1, 0L, true
        );
        assertEquals("", VoucherService.formatExpiryAndQuotaMeta(unlimitedNoExpiry));
        assertEquals("Đã dùng 1 lượt (Không giới hạn)", VoucherService.formatAdminQuotaAndExpiry(unlimitedNoExpiry));

        assertEquals("Hiện chưa có mã giảm giá khả dụng", VoucherService.formatVoucherWalletMessage(null));
        assertEquals("Hiện chưa có mã giảm giá khả dụng", VoucherService.formatVoucherWalletMessage(Collections.emptyList()));
        String walletMsg = VoucherService.formatVoucherWalletMessage(Arrays.asList(fullMeta, unlimitedWithExpiry));
        assertEquals(
                "• RUN150K (-150K)\n  Giảm 150K · Đơn từ 2M\n  Còn 7/10 lượt · HSD: 31/12/2026\n\n"
                        + "• UNLIM (-30K)\n  Không giới hạn · Mọi đơn\n  HSD: 01/01/2027",
                walletMsg
        );
    }
}
