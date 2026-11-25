package com.example.shoes_store.module.cart;

import androidx.annotation.NonNull;

import com.example.shoes_store.module.product.ProductModel;

/**
 * Một dòng sản phẩm trong giỏ hàng (gắn với Product và ProductVariant).
 */
public class CartItemModel {

    private final String id;
    private final ProductModel product;
    private final String variantId;
    private final String size;
    private final String colorName;
    private final int quantity;
    private final boolean selected;
    private final long updatedAtMillis;

    public CartItemModel(
            String id,
            ProductModel product,
            String variantId,
            String size,
            String colorName,
            int quantity,
            boolean selected,
            long updatedAtMillis
    ) {
        this.id = id != null ? id : "";
        this.product = product;
        this.variantId = variantId != null ? variantId : "";
        this.size = size != null ? size : "42";
        this.colorName = colorName != null ? colorName : "Mặc định";
        this.quantity = Math.max(1, quantity);
        this.selected = selected;
        this.updatedAtMillis = updatedAtMillis;
    }

    public CartItemModel copy() {
        return new CartItemModel(id, product, variantId, size, colorName, quantity, selected, updatedAtMillis);
    }

    public String getId() {
        return id;
    }

    public ProductModel getProduct() {
        return product;
    }

    @NonNull
    public String getVariantId() {
        return variantId;
    }

    public String getSize() {
        return size;
    }

    public String getColorName() {
        return colorName;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean isSelected() {
        return selected;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public long getLineTotal() {
        return product.getPrice() * quantity;
    }
}
