package com.example.shoes_store.module.cart;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.module.product.ProductVariantModel;

import java.util.List;

/**
 * Nghiệp vụ thuần Java cho Giỏ hàng: khớp biến thể, kiểm tồn kho khi thêm/tăng và chọn biến thể thêm nhanh.
 */
public final class CartService {

    private CartService() {
    }

    /**
     * Kết quả tổng hợp số lượng, tạm tính và trạng thái chọn toàn bộ trong Giỏ hàng.
     */
    public static final class CartSummary {
        private final int totalQuantity;
        private final int selectedQuantity;
        private final long selectedSubtotal;
        private final boolean allSelected;
        private final boolean empty;

        public CartSummary(
                int totalQuantity,
                int selectedQuantity,
                long selectedSubtotal,
                boolean allSelected,
                boolean empty
        ) {
            this.totalQuantity = totalQuantity;
            this.selectedQuantity = selectedQuantity;
            this.selectedSubtotal = selectedSubtotal;
            this.allSelected = allSelected;
            this.empty = empty;
        }

        public int getTotalQuantity() {
            return totalQuantity;
        }

        public int getSelectedQuantity() {
            return selectedQuantity;
        }

        public long getSelectedSubtotal() {
            return selectedSubtotal;
        }

        public boolean isAllSelected() {
            return allSelected;
        }

        public boolean isEmpty() {
            return empty;
        }
    }

    @NonNull
    public static CartSummary summarize(@Nullable List<CartItemModel> items) {
        if (items == null || items.isEmpty()) {
            return new CartSummary(0, 0, 0L, false, true);
        }
        int totalQtyAll = 0;
        int selectedQty = 0;
        long selectedSubtotal = 0L;
        boolean allSelected = true;
        for (CartItemModel item : items) {
            totalQtyAll += item.getQuantity();
            if (item.isSelected()) {
                selectedQty += item.getQuantity();
                selectedSubtotal += item.getLineTotal();
            } else {
                allSelected = false;
            }
        }
        return new CartSummary(totalQtyAll, selectedQty, selectedSubtotal, allSelected, false);
    }

    public static long calculateFinalTotal(long selectedSubtotal, long discount) {
        return Math.max(0L, Math.max(0L, selectedSubtotal) - Math.max(0L, discount));
    }

    @Nullable
    public static CartItemModel findMatchingItem(
            @Nullable List<CartItemModel> currentItems,
            @NonNull String productId,
            @Nullable String variantId,
            @NonNull String size,
            @NonNull String colorName
    ) {
        if (currentItems == null || currentItems.isEmpty()) {
            return null;
        }
        String cleanVariantId = variantId != null ? variantId.trim() : "";
        for (CartItemModel existing : currentItems) {
            boolean sameVariant = !cleanVariantId.isEmpty()
                    ? cleanVariantId.equals(existing.getVariantId()) && productId.equals(existing.getProduct().getId())
                    : productId.equals(existing.getProduct().getId())
                            && size.equalsIgnoreCase(existing.getSize())
                            && colorName.equalsIgnoreCase(existing.getColorName());
            if (sameVariant) {
                return existing;
            }
        }
        return null;
    }

    public static int getQuantityForVariant(
            @Nullable List<CartItemModel> currentItems,
            @NonNull String productId,
            @Nullable String variantId,
            @NonNull String size,
            @NonNull String colorName
    ) {
        CartItemModel match = findMatchingItem(currentItems, productId, variantId, size, colorName);
        return match != null ? match.getQuantity() : 0;
    }

    @Nullable
    public static String validateStockForAdd(int existingQty, int addQty, int maxStock) {
        if (maxStock <= 0) {
            return "Sản phẩm này hiện đã hết hàng";
        }
        int requested = Math.max(1, addQty);
        if (Math.max(0, existingQty) + requested > maxStock) {
            return "Chỉ còn " + maxStock + " đôi trong kho";
        }
        return null;
    }

    @Nullable
    public static String validateStockForNextQuantity(int nextQty, int stockQty) {
        if (stockQty <= 0) {
            return "Sản phẩm này hiện đã hết hàng";
        }
        if (nextQty > stockQty) {
            return "Chỉ còn " + stockQty + " đôi trong kho";
        }
        return null;
    }

    @Nullable
    public static ProductVariantModel chooseQuickAddVariant(
            @Nullable List<ProductVariantModel> variants,
            @NonNull String preferredSize
    ) {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        for (ProductVariantModel v : variants) {
            if (preferredSize.equalsIgnoreCase(v.getSize()) && v.isInStock()) {
                return v;
            }
        }
        for (ProductVariantModel v : variants) {
            if (v.isInStock()) {
                return v;
            }
        }
        return null;
    }
}
