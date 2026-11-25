package com.example.shoes_store.module.order;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.R;
import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.product.ProductModel;

/**
 * Snapshot một dòng sản phẩm cố định tại thời điểm đặt đơn hàng.
 */
public class OrderItemModel {

    private final String productId;
    private final String variantId;
    private final String productName;
    private final String thumbnailUrl;
    @DrawableRes
    private final int fallbackImageResId;
    private final String size;
    private final String colorName;
    private final long unitPrice;
    private final int quantity;
    private final long lineTotal;

    public OrderItemModel(
            @NonNull String productId,
            @NonNull String variantId,
            @NonNull String productName,
            @Nullable String thumbnailUrl,
            @DrawableRes int fallbackImageResId,
            @NonNull String size,
            @NonNull String colorName,
            long unitPrice,
            int quantity,
            long lineTotal
    ) {
        this.productId = productId;
        this.variantId = variantId;
        this.productName = productName;
        this.thumbnailUrl = thumbnailUrl != null ? thumbnailUrl : "";
        this.fallbackImageResId = fallbackImageResId != 0 ? fallbackImageResId : R.drawable.img_shoe_asics_novablast;
        this.size = size;
        this.colorName = colorName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
    }

    @NonNull
    public static OrderItemModel fromCartItem(@NonNull CartItemModel item) {
        ProductModel p = item.getProduct();
        return new OrderItemModel(
                p.getId(),
                item.getVariantId(),
                p.getBrand() + " " + p.getName(),
                p.getThumbnailUrl(),
                p.getImageResId(),
                item.getSize(),
                item.getColorName(),
                p.getPrice(),
                item.getQuantity(),
                item.getLineTotal()
        );
    }

    @NonNull
    public String getProductId() {
        return productId;
    }

    @NonNull
    public String getVariantId() {
        return variantId;
    }

    @NonNull
    public String getProductName() {
        return productName;
    }

    @NonNull
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    @DrawableRes
    public int getFallbackImageResId() {
        return fallbackImageResId;
    }

    @NonNull
    public String getSize() {
        return size;
    }

    @NonNull
    public String getColorName() {
        return colorName;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getLineTotal() {
        return lineTotal;
    }
}
