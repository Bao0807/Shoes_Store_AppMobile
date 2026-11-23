package com.example.shoes_store.module.cart;

/**
 * Model mã giảm giá (Voucher) áp dụng trong giỏ hàng.
 */
public class CartVoucherModel {

    private final String code;
    private final String shortBadge;
    private final String title;
    private final String description;
    private final long discountAmount;
    private final long minOrderAmount;
    private final String expiryDate;

    public CartVoucherModel(
            String code,
            String shortBadge,
            String title,
            String description,
            long discountAmount,
            long minOrderAmount,
            String expiryDate
    ) {
        this.code = code;
        this.shortBadge = shortBadge;
        this.title = title;
        this.description = description;
        this.discountAmount = discountAmount;
        this.minOrderAmount = minOrderAmount;
        this.expiryDate = expiryDate;
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

    public boolean isEligible(long subtotal) {
        return subtotal > 0 && subtotal >= minOrderAmount;
    }
}
