package com.example.shoes_store.module.cart;

import com.example.shoes_store.module.product.ProductModel;

/**
 * Một dòng sản phẩm trong giỏ hàng hoặc đơn hàng.
 */
public class CartItemModel {

    private final String id;
    private final ProductModel product;
    private final String size;
    private final String colorName;
    private int quantity;
    private boolean selected;

    public CartItemModel(
            String id,
            ProductModel product,
            String size,
            String colorName,
            int quantity,
            boolean selected
    ) {
        this.id = id;
        this.product = product;
        this.size = size;
        this.colorName = colorName;
        this.quantity = quantity;
        this.selected = selected;
    }

    public CartItemModel copy() {
        return new CartItemModel(id, product, size, colorName, quantity, selected);
    }

    public String getId() {
        return id;
    }

    public ProductModel getProduct() {
        return product;
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

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public long getLineTotal() {
        return product.getPrice() * quantity;
    }
}
