package com.example.shoes_store.module.cart;

import androidx.annotation.Nullable;

import com.example.shoes_store.core.CurrencyFormatUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Model mã giảm giá (Voucher) áp dụng trong giỏ hàng và thanh toán.
 */
public class CartVoucherModel {

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

    public CartVoucherModel(
            String code,
            String shortBadge,
            String title,
            String description,
            long discountAmount,
            long minOrderAmount,
            String expiryDate
    ) {
        this(code, shortBadge, title, description, discountAmount, minOrderAmount, expiryDate, 0, 0, 0L, true);
    }

    public CartVoucherModel(
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
            this.expiryDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(new Date(expiryTimestampMillis));
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
        if (!active) {
            return "Mã giảm giá hiện không còn áp dụng";
        }
        if (!isNotExpired()) {
            return "Mã giảm giá đã hết hạn sử dụng";
        }
        if (!hasRemainingQuota()) {
            return "Mã giảm giá đã hết lượt sử dụng";
        }
        if (subtotal <= 0 || subtotal < minOrderAmount) {
            long missing = Math.max(0L, minOrderAmount - Math.max(0L, subtotal));
            return "Mua thêm " + CurrencyFormatUtil.formatVnd(missing) + " để dùng mã này";
        }
        return null;
    }
}
