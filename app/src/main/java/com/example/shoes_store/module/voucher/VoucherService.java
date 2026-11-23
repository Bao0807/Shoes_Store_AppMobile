package com.example.shoes_store.module.voucher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.core.CurrencyFormatUtil;

/**
 * Nghiệp vụ thuần Java cho kiểm tra điều kiện áp dụng Voucher và tính mức giảm giá.
 */
public final class VoucherService {

    private VoucherService() {
    }

    @Nullable
    public static String getIneligibleReason(@NonNull VoucherModel voucher, long subtotal) {
        if (!voucher.isActive()) {
            return "Mã giảm giá hiện không còn áp dụng";
        }
        if (!voucher.isNotExpired()) {
            return "Mã giảm giá đã hết hạn sử dụng";
        }
        if (!voucher.hasRemainingQuota()) {
            return "Mã giảm giá đã hết lượt sử dụng";
        }
        if (subtotal <= 0 || subtotal < voucher.getMinOrderAmount()) {
            long missing = Math.max(0L, voucher.getMinOrderAmount() - Math.max(0L, subtotal));
            return "Mua thêm " + CurrencyFormatUtil.formatVnd(missing) + " để dùng mã này";
        }
        return null;
    }

    public static long resolveDiscountAmount(@Nullable VoucherModel voucher, long subtotal) {
        if (voucher == null || subtotal <= 0L || !voucher.isEligible(subtotal)) {
            return 0L;
        }
        return Math.min(Math.max(0L, voucher.getDiscountAmount()), subtotal);
    }

    @Nullable
    public static String validateVoucherSnapshotForTransaction(
            boolean active,
            long expiryMillis,
            long nowMillis,
            long minOrderAmount,
            long usageLimit,
            long usedCount,
            long subtotal,
            @NonNull String code
    ) {
        if (!active || (expiryMillis > 0L && nowMillis > expiryMillis)) {
            return "Mã giảm giá " + code + " đã hết hạn";
        }
        if (subtotal < minOrderAmount) {
            return "Đơn hàng chưa đạt mức tối thiểu để dùng mã " + code;
        }
        if (usageLimit > 0L && usedCount >= usageLimit) {
            return "Mã giảm giá " + code + " đã hết lượt sử dụng";
        }
        return null;
    }
}
