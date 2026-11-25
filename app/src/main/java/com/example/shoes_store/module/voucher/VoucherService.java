package com.example.shoes_store.module.voucher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.core.CurrencyFormatUtil;

import java.util.List;

/**
 * Nghiệp vụ thuần Java cho tìm mã, định dạng mô tả, kiểm tra điều kiện và tính mức giảm giá Voucher.
 */
public final class VoucherService {

    private VoucherService() {
    }

    @Nullable
    public static VoucherModel findByCode(@Nullable List<VoucherModel> vouchers, @Nullable String code) {
        if (vouchers == null || code == null || code.trim().isEmpty()) {
            return null;
        }
        String cleanCode = code.trim();
        for (VoucherModel voucher : vouchers) {
            if (voucher != null && cleanCode.equalsIgnoreCase(voucher.getCode())) {
                return voucher;
            }
        }
        return null;
    }

    @NonNull
    public static String formatExpiryAndQuotaMeta(@NonNull VoucherModel voucher) {
        StringBuilder meta = new StringBuilder();
        if (!voucher.getExpiryDate().isEmpty()) {
            meta.append("HSD: ").append(voucher.getExpiryDate());
        }
        if (voucher.getUsageLimit() > 0) {
            int remaining = Math.max(0, voucher.getUsageLimit() - voucher.getUsedCount());
            if (meta.length() > 0) {
                meta.append(" · ");
            }
            meta.append("Còn ").append(remaining).append(" lượt");
        }
        return meta.toString();
    }

    @NonNull
    public static String formatVoucherWalletMessage(@Nullable List<VoucherModel> vouchers) {
        if (vouchers == null || vouchers.isEmpty()) {
            return "Hiện chưa có mã giảm giá khả dụng";
        }
        StringBuilder sb = new StringBuilder();
        for (VoucherModel voucher : vouchers) {
            if (voucher == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("• ").append(voucher.getCode()).append(" (").append(voucher.getShortBadge()).append(")\n  ")
                    .append(voucher.getTitle()).append(" · ").append(voucher.getDescription());
            if (voucher.getUsageLimit() > 0) {
                int remaining = Math.max(0, voucher.getUsageLimit() - voucher.getUsedCount());
                sb.append("\n  Còn ").append(remaining).append("/").append(voucher.getUsageLimit()).append(" lượt");
            }
            if (!voucher.getExpiryDate().isEmpty()) {
                sb.append(voucher.getUsageLimit() > 0 ? " · " : "\n  ")
                        .append("HSD: ").append(voucher.getExpiryDate());
            }
        }
        return sb.length() > 0 ? sb.toString() : "Hiện chưa có mã giảm giá khả dụng";
    }

    @NonNull
    public static String formatAdminSummary(@NonNull VoucherModel voucher) {
        return "Giảm " + CurrencyFormatUtil.formatVnd(voucher.getDiscountAmount())
                + " · Đơn tối thiểu " + CurrencyFormatUtil.formatVnd(voucher.getMinOrderAmount());
    }

    @NonNull
    public static String formatAdminQuotaAndExpiry(@NonNull VoucherModel voucher) {
        String quotaText = voucher.getUsageLimit() > 0
                ? "Đã dùng " + voucher.getUsedCount() + "/" + voucher.getUsageLimit() + " lượt"
                : "Đã dùng " + voucher.getUsedCount() + " lượt (Không giới hạn)";
        if (!voucher.getExpiryDate().isEmpty()) {
            quotaText += " · HSD: " + voucher.getExpiryDate();
        }
        return quotaText;
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
