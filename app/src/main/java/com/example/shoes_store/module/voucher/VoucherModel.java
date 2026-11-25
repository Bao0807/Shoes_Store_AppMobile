package com.example.shoes_store.module.voucher;

import androidx.annotation.Nullable;

import com.example.shoes_store.core.DateFormatUtil;

/**
 * Model mã giảm giá (Voucher) dùng chung cho Giỏ hàng, Đặt hàng, Thông báo và Kho Voucher.
 */
public class VoucherModel {

    private final String code;
    private final String shortBadge;
    private final String title;
    private final String description;
    private final long discountAmount;
    private final long minOrderAmount;
    private final String expiryDate;
    private final int usageLimit;
    private final int usedCount;
    private final long expiryTimestampMillis;
    private final boolean active;

    public VoucherModel(
            String code,
            String shortBadge,
            String title,
            String description,
            long discountAmount,
            long minOrderAmount,
            String expiryDate,
            int usageLimit,
            int usedCount,
            long expiryTimestampMillis,
            boolean active
    ) {
        this.code = code != null ? code : "";
        this.shortBadge = shortBadge != null ? shortBadge : "";
        this.title = title != null ? title : "";
        this.description = description != null ? description : "";
        this.discountAmount = discountAmount;
        this.minOrderAmount = minOrderAmount;
        this.usageLimit = usageLimit;
        this.usedCount = usedCount;
        this.expiryTimestampMillis = expiryTimestampMillis;
        this.active = active;
        if (expiryDate != null && !expiryDate.trim().isEmpty()) {
            this.expiryDate = expiryDate;
        } else if (expiryTimestampMillis > 0L) {
            this.expiryDate = DateFormatUtil.formatDate(expiryTimestampMillis);
        } else {
            this.expiryDate = "";
        }
    }

    public String getCode() {
        return code;
    }

    public String getShortBadge() {
        return shortBadge;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public long getMinOrderAmount() {
        return minOrderAmount;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public int getUsageLimit() {
        return usageLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public long getExpiryTimestampMillis() {
        return expiryTimestampMillis;
    }

    public boolean isActive() {
        return active;
    }

    public boolean hasRemainingQuota() {
        return usageLimit <= 0 || usedCount < usageLimit;
    }

    public boolean isNotExpired() {
        return expiryTimestampMillis <= 0L || System.currentTimeMillis() <= expiryTimestampMillis;
    }

    public boolean isEligible(long subtotal) {
        return active
                && isNotExpired()
                && hasRemainingQuota()
                && subtotal > 0
                && subtotal >= minOrderAmount;
    }

    @Nullable
    public String getIneligibleReason(long subtotal) {
        return VoucherService.getIneligibleReason(this, subtotal);
    }
}
