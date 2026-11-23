package com.example.shoes_store.module.product;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Biến thể giày theo từng phối màu và size EU kèm tồn kho thực tế.
 */
public class ProductVariantModel {

    private final String variantId;
    private final String productId;
    private final String colorName;
    private final String colorHex;
    private final String imageUrl;
    private final String size;
    private final int stockQty;
    private final String sku;

    public ProductVariantModel(
            @NonNull String variantId,
            @NonNull String productId,
            @NonNull String colorName,
            @NonNull String colorHex,
            @Nullable String imageUrl,
            @NonNull String size,
            int stockQty,
            @NonNull String sku
    ) {
        this.variantId = variantId;
        this.productId = productId;
        this.colorName = colorName;
        this.colorHex = colorHex;
        this.imageUrl = imageUrl != null ? imageUrl : "";
        this.size = size;
        this.stockQty = Math.max(0, stockQty);
        this.sku = sku;
    }

    @NonNull
    public String getVariantId() {
        return variantId;
    }

    @NonNull
    public String getProductId() {
        return productId;
    }

    @NonNull
    public String getColorName() {
        return colorName;
    }

    @NonNull
    public String getColorHex() {
        return colorHex;
    }

    @NonNull
    public String getImageUrl() {
        return imageUrl;
    }

    @NonNull
    public String getSize() {
        return size;
    }

    public int getStockQty() {
        return stockQty;
    }

    @NonNull
    public String getSku() {
        return sku;
    }

    public boolean isInStock() {
        return stockQty > 0;
    }

    public boolean hasEnoughStock(int quantity) {
        return quantity > 0 && stockQty >= quantity;
    }
}
