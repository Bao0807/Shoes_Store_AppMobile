package com.example.shoes_store.module.order;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.module.cart.CartItemModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Nghiệp vụ thuần Java cho Đơn hàng: tính tiền, sinh mã đơn, kiểm tồn kho, hủy đơn & hoàn lượt voucher.
 */
public final class OrderService {

    private OrderService() {
    }

    public static long calculateSubtotal(@Nullable List<CartItemModel> items) {
        long subtotal = 0L;
        if (items == null) {
            return 0L;
        }
        for (CartItemModel item : items) {
            if (item != null) {
                subtotal += item.getLineTotal();
            }
        }
        return Math.max(0L, subtotal);
    }

    public static long calculateTotal(long subtotal, long shippingFee, long discount) {
        return Math.max(0L, Math.max(0L, subtotal) + Math.max(0L, shippingFee) - Math.max(0L, discount));
    }

    @NonNull
    public static String generateOrderCode(long nowMillis) {
        return "RN" + new SimpleDateFormat("yyMMddHHmmss", Locale.US).format(new Date(nowMillis));
    }

    @NonNull
    public static String buildVariantKey(@NonNull String productId, @NonNull String variantId) {
        return productId + "|" + variantId;
    }

    @NonNull
    public static Map<String, Integer> aggregateRequestedQuantities(@NonNull List<CartItemModel> items) {
        Map<String, Integer> requestedQtyByKey = new LinkedHashMap<>();
        for (CartItemModel item : items) {
            if (item.getVariantId().isEmpty()) {
                continue;
            }
            String key = buildVariantKey(item.getProduct().getId(), item.getVariantId());
            requestedQtyByKey.put(key, requestedQtyByKey.getOrDefault(key, 0) + Math.max(1, item.getQuantity()));
        }
        return requestedQtyByKey;
    }

    @Nullable
    public static String validateStockSufficiency(
            @NonNull Map<String, Integer> requestedByKey,
            @NonNull Map<String, Integer> currentStockByKey,
            @NonNull Map<String, String> labelByKey
    ) {
        for (Map.Entry<String, Integer> entry : requestedByKey.entrySet()) {
            String key = entry.getKey();
            int needed = entry.getValue() != null ? entry.getValue() : 0;
            String label = labelByKey.getOrDefault(key, "Sản phẩm");
            Integer currentStock = currentStockByKey.get(key);
            if (currentStock == null) {
                return "Sản phẩm " + label + " không còn tồn tại";
            }
            if (currentStock < needed) {
                return label + " chỉ còn " + currentStock + " đôi trong kho";
            }
        }
        return null;
    }

    public static boolean canCancelStatus(@Nullable String statusStr) {
        return OrderModel.Status.PENDING.name().equalsIgnoreCase(statusStr);
    }

    public static int computeRestoredStock(int currentStock, int restoreQty) {
        return Math.max(0, currentStock) + Math.max(0, restoreQty);
    }

    public static long computeNextVoucherUsedCountOnCancel(long currentUsedCount) {
        return Math.max(0L, currentUsedCount - 1L);
    }
}
