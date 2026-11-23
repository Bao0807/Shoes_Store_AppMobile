package com.example.shoes_store.module.cart;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreStoreHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.product.ProductVariantModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository quản lý Giỏ hàng (users/{uid}/cart) kiểm tra tồn kho thật và danh sách Voucher trên Cloud Firestore.
 */
public class CartRepository {

    private final FirestoreStoreHelper store = FirestoreStoreHelper.getInstance();
    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();

    public LiveData<List<CartItemModel>> getCartItemsLiveData() {
        return store.getCartItemsLiveData();
    }

    public LiveData<List<CartVoucherModel>> getVouchersLiveData() {
        return store.getVouchersLiveData();
    }

    public List<CartVoucherModel> getVouchers() {
        return store.getVouchers();
    }

    public void updateQuantity(
            @NonNull CartItemModel item,
            int delta,
            @Nullable ResourceCallback<Void> callback
    ) {
        CollectionReference cartRef = userCartRef();
        if (cartRef == null) {
            if (callback != null) {
                callback.onError("Vui lòng đăng nhập");
            }
            return;
        }
        int nextQty = item.getQuantity() + delta;
        if (delta <= 0) {
            if (nextQty <= 0) {
                cartRef.document(item.getId()).delete()
                        .addOnSuccessListener(unused -> {
                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (callback != null) {
                                callback.onError("Không thể cập nhật giỏ hàng");
                            }
                        });
            } else {
                applyQuantityUpdate(cartRef, item.getId(), nextQty, callback);
            }
            return;
        }

        String productId = item.getProduct().getId();
        String variantId = item.getVariantId();
        CollectionReference variantsRef = provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_PRODUCTS)
                .document(productId)
                .collection(FirebaseProviderHelper.SUB_VARIANTS);

        if (!variantId.isEmpty()) {
            variantsRef.document(variantId).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            if (callback != null) {
                                callback.onError("Biến thể sản phẩm không còn tồn tại");
                            }
                            return;
                        }
                        int stockQty = FirestoreStoreHelper.readInt(doc, "stockQty", 0);
                        handleStockCheckAndUpdate(cartRef, item.getId(), nextQty, stockQty, callback);
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) {
                            callback.onError("Không thể kiểm tra tồn kho, vui lòng thử lại");
                        }
                    });
        } else {
            variantsRef.get()
                    .addOnSuccessListener(snap -> {
                        ProductVariantModel matched = null;
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            ProductVariantModel v = FirestoreStoreHelper.mapVariant(doc, productId);
                            if (v.getSize().equalsIgnoreCase(item.getSize())
                                    && v.getColorName().equalsIgnoreCase(item.getColorName())) {
                                matched = v;
                                break;
                            }
                        }
                        if (matched == null) {
                            if (callback != null) {
                                callback.onError("Biến thể sản phẩm không còn tồn tại");
                            }
                            return;
                        }
                        handleStockCheckAndUpdate(cartRef, item.getId(), nextQty, matched.getStockQty(), callback);
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) {
                            callback.onError("Không thể kiểm tra tồn kho, vui lòng thử lại");
                        }
                    });
        }
    }

    private void handleStockCheckAndUpdate(
            @NonNull CollectionReference cartRef,
            @NonNull String cartItemId,
            int nextQty,
            int stockQty,
            @Nullable ResourceCallback<Void> callback
    ) {
        if (stockQty <= 0) {
            if (callback != null) {
                callback.onError("Sản phẩm này hiện đã hết hàng");
            }
            return;
        }
        if (nextQty > stockQty) {
            if (callback != null) {
                callback.onError("Chỉ còn " + stockQty + " đôi trong kho");
            }
            return;
        }
        applyQuantityUpdate(cartRef, cartItemId, nextQty, callback);
    }

    private void applyQuantityUpdate(
            @NonNull CollectionReference cartRef,
            @NonNull String cartItemId,
            int nextQty,
            @Nullable ResourceCallback<Void> callback
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("quantity", nextQty);
        updates.put("updatedAt", Timestamp.now());
        cartRef.document(cartItemId).update(updates)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError("Không thể cập nhật số lượng");
                    }
                });
    }

    public void toggleItemSelected(@NonNull String cartItemId, boolean selected) {
        CollectionReference cartRef = userCartRef();
        if (cartRef == null) {
            return;
        }
        cartRef.document(cartItemId).update("selected", selected);
    }

    public void selectAll(@Nullable List<CartItemModel> currentItems, boolean selected) {
        CollectionReference cartRef = userCartRef();
        if (cartRef == null || currentItems == null || currentItems.isEmpty()) {
            return;
        }
        WriteBatch batch = provider.getFirestore().batch();
        for (CartItemModel item : currentItems) {
            batch.update(cartRef.document(item.getId()), "selected", selected);
        }
        batch.commit();
    }

    public void removeItem(@NonNull String cartItemId) {
        CollectionReference cartRef = userCartRef();
        if (cartRef != null) {
            cartRef.document(cartItemId).delete();
        }
    }

    @Nullable
    private CollectionReference userCartRef() {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            return null;
        }
        return provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_CART);
    }
}
