package com.example.shoes_store.module.order;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.cart.CartMapper;
import com.example.shoes_store.module.cart.CartRepository;
import com.example.shoes_store.module.cart.CartService;
import com.example.shoes_store.module.product.ProductMapper;
import com.example.shoes_store.module.product.ProductRepository;
import com.example.shoes_store.module.product.ProductVariantModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper xử lý mua lại đơn hàng và chuẩn hóa variantId trước khi đặt đơn với truy vấn productId được khử trùng.
 */
public final class OrderReorderHelper {

    private static final String TAG = "OrderReorderHelper";

    private OrderReorderHelper() {
    }

    static void reorder(
            @NonNull FirebaseFirestore db,
            @NonNull String uid,
            @NonNull OrderModel order,
            @NonNull ResourceCallback<Integer> callback
    ) {
        List<OrderItemModel> items = order.getOrderItems();
        if (items.isEmpty()) {
            callback.onError("Đơn hàng không có sản phẩm");
            return;
        }

        ProductRepository productRepo = ProductRepository.getInstance();
        List<OrderItemModel> activeItems = new ArrayList<>();
        Map<String, Task<QuerySnapshot>> variantTaskByProductId = new LinkedHashMap<>();
        for (OrderItemModel item : items) {
            String productId = item.getProductId();
            if (productRepo.getProductById(productId) == null) {
                continue;
            }
            activeItems.add(item);
            putVariantFetchTaskIfAbsent(db, variantTaskByProductId, productId);
        }
        if (activeItems.isEmpty()) {
            callback.onError("Sản phẩm trong đơn hiện không còn kinh doanh");
            return;
        }

        Tasks.whenAllComplete(variantTaskByProductId.values())
                .addOnCompleteListener(all -> commitReorderBatch(db, uid, activeItems, variantTaskByProductId, callback));
    }

    static void ensureItemVariantIds(
            @NonNull FirebaseFirestore db,
            @NonNull List<CartItemModel> items,
            @NonNull Runnable onReady
    ) {
        List<Integer> missingIndices = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            CartItemModel item = items.get(i);
            if (item.getVariantId().isEmpty()) {
                ProductVariantModel cached = item.getProduct().findVariant(item.getColorName(), item.getSize());
                if (cached != null) {
                    items.set(i, new CartItemModel(
                            item.getId(),
                            item.getProduct(),
                            cached.getVariantId(),
                            item.getSize(),
                            item.getColorName(),
                            item.getQuantity(),
                            item.isSelected(),
                            item.getUpdatedAtMillis()
                    ));
                } else {
                    missingIndices.add(i);
                }
            }
        }
        if (missingIndices.isEmpty()) {
            onReady.run();
            return;
        }

        Map<String, Task<QuerySnapshot>> variantTaskByProductId = new LinkedHashMap<>();
        for (int idx : missingIndices) {
            putVariantFetchTaskIfAbsent(db, variantTaskByProductId, items.get(idx).getProduct().getId());
        }

        Tasks.whenAllComplete(variantTaskByProductId.values()).addOnCompleteListener(all -> {
            Map<String, List<ProductVariantModel>> loadedByProductId = collectLoadedVariants(
                    variantTaskByProductId,
                    "Fetch variants failed for product "
            );

            for (int idx : missingIndices) {
                CartItemModel item = items.get(idx);
                List<ProductVariantModel> loaded = loadedByProductId.get(item.getProduct().getId());
                if (loaded == null) {
                    continue;
                }
                item.getProduct().setVariants(loaded);
                ProductVariantModel matched = item.getProduct().findVariant(item.getColorName(), item.getSize());
                if (matched != null) {
                    items.set(idx, new CartItemModel(
                            item.getId(),
                            item.getProduct(),
                            matched.getVariantId(),
                            item.getSize(),
                            item.getColorName(),
                            item.getQuantity(),
                            item.isSelected(),
                            item.getUpdatedAtMillis()
                    ));
                }
            }
            onReady.run();
        });
    }

    private static void commitReorderBatch(
            @NonNull FirebaseFirestore db,
            @NonNull String uid,
            @NonNull List<OrderItemModel> activeItems,
            @NonNull Map<String, Task<QuerySnapshot>> variantTaskByProductId,
            @NonNull ResourceCallback<Integer> callback
    ) {
        Map<String, List<ProductVariantModel>> variantsByProductId = collectLoadedVariants(
                variantTaskByProductId,
                "Fetch variants failed for reorder product "
        );

        CollectionReference cartRef = db.collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_CART);

        List<CartItemModel> existingCart = CartRepository.getInstance().getCurrentCartItems();
        Map<String, Integer> pendingQtyByCartDoc = new HashMap<>();
        WriteBatch batch = db.batch();
        Timestamp now = Timestamp.now();
        int addedCount = 0;
        boolean hitStockLimit = false;

        for (OrderItemModel item : activeItems) {
            List<ProductVariantModel> variants = variantsByProductId.get(item.getProductId());
            if (variants == null) {
                continue;
            }
            ProductVariantModel matched = findMatchedVariant(variants, item);
            if (matched == null || !matched.isInStock()) {
                hitStockLimit = true;
                continue;
            }

            CartItemModel existing = CartService.findMatchingItem(
                    existingCart, item.getProductId(), matched.getVariantId(), item.getSize(), item.getColorName()
            );
            int requested = Math.max(1, item.getQuantity());
            if (existing != null) {
                int currentInCart = pendingQtyByCartDoc.getOrDefault(existing.getId(), existing.getQuantity());
                int available = matched.getStockQty() - currentInCart;
                if (available <= 0) {
                    hitStockLimit = true;
                    continue;
                }
                int addQty = Math.min(requested, available);
                int nextQty = currentInCart + addQty;
                pendingQtyByCartDoc.put(existing.getId(), nextQty);
                batch.update(cartRef.document(existing.getId()), CartMapper.cartMergeUpdateMap(nextQty, now));
                addedCount++;
            } else {
                int addQty = Math.min(requested, matched.getStockQty());
                DocumentReference docRef = cartRef.document();
                batch.set(docRef, CartMapper.cartItemToMap(
                        docRef.getId(),
                        uid,
                        item.getProductId(),
                        matched.getVariantId(),
                        item.getSize(),
                        item.getColorName(),
                        addQty,
                        true,
                        now
                ));
                addedCount++;
            }
        }

        if (addedCount == 0) {
            callback.onError(hitStockLimit
                    ? "Sản phẩm trong đơn đã hết hàng hoặc giỏ đã đạt tối đa tồn kho"
                    : "Sản phẩm trong đơn hiện không còn kinh doanh");
            return;
        }
        final int finalAddedCount = addedCount;
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(finalAddedCount))
                .addOnFailureListener(e -> callback.onError("Không thể thêm lại vào giỏ hàng"));
    }

    private static void putVariantFetchTaskIfAbsent(
            @NonNull FirebaseFirestore db,
            @NonNull Map<String, Task<QuerySnapshot>> taskByProductId,
            @NonNull String productId
    ) {
        if (!taskByProductId.containsKey(productId)) {
            taskByProductId.put(
                    productId,
                    db.collection(FirebaseProviderHelper.COL_PRODUCTS)
                            .document(productId)
                            .collection(FirebaseProviderHelper.SUB_VARIANTS)
                            .get()
            );
        }
    }

    @NonNull
    private static Map<String, List<ProductVariantModel>> collectLoadedVariants(
            @NonNull Map<String, Task<QuerySnapshot>> variantTaskByProductId,
            @NonNull String failureLogPrefix
    ) {
        Map<String, List<ProductVariantModel>> loadedByProductId = new HashMap<>();
        for (Map.Entry<String, Task<QuerySnapshot>> entry : variantTaskByProductId.entrySet()) {
            String productId = entry.getKey();
            Task<QuerySnapshot> task = entry.getValue();
            if (task.isSuccessful() && task.getResult() != null) {
                List<ProductVariantModel> loaded = new ArrayList<>();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    loaded.add(ProductMapper.variantFromSnapshot(doc, productId));
                }
                loadedByProductId.put(productId, loaded);
            } else if (!task.isSuccessful()) {
                Log.w(TAG, failureLogPrefix + productId + ": "
                        + (task.getException() != null ? task.getException().getMessage() : "unknown"));
            }
        }
        return loadedByProductId;
    }

    private static ProductVariantModel findMatchedVariant(
            @NonNull List<ProductVariantModel> variants,
            @NonNull OrderItemModel item
    ) {
        for (ProductVariantModel variant : variants) {
            if ((!item.getVariantId().isEmpty() && item.getVariantId().equals(variant.getVariantId()))
                    || (variant.getSize().equalsIgnoreCase(item.getSize())
                    && variant.getColorName().equalsIgnoreCase(item.getColorName()))) {
                return variant;
            }
        }
        return null;
    }
}
