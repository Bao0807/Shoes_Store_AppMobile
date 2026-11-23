package com.example.shoes_store.module.cart;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.product.ProductMapper;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.product.ProductRepository;
import com.example.shoes_store.module.product.ProductVariantModel;
import com.example.shoes_store.module.voucher.VoucherModel;
import com.example.shoes_store.module.voucher.VoucherRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repository sở hữu toàn bộ Giỏ hàng (`users/{uid}/cart`): đồng bộ realtime, kiểm tồn kho và cập nhật Firestore.
 */
public class CartRepository {

    private static final String TAG = "CartRepository";
    private static volatile CartRepository instance;

    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();
    private final MutableLiveData<List<CartItemModel>> cartItemsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> cartTotalCountLiveData = new MutableLiveData<>(0);
    private final List<RawCartDoc> rawCartDocs = new ArrayList<>();
    @Nullable
    private ListenerRegistration cartRegistration;

    private CartRepository() {
    }

    public static CartRepository getInstance() {
        if (instance == null) {
            synchronized (CartRepository.class) {
                if (instance == null) {
                    instance = new CartRepository();
                }
            }
        }
        return instance;
    }

    // ===== 1. REALTIME =====

    public void startUserListening(@NonNull String uid) {
        stopUserListening();
        cartRegistration = provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_CART)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Cart listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    rawCartDocs.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        rawCartDocs.add(new RawCartDoc(
                                doc.getId(),
                                FirestoreReadUtil.readString(doc, CartMapper.FIELD_PRODUCT_ID, ""),
                                FirestoreReadUtil.readString(doc, CartMapper.FIELD_VARIANT_ID, ""),
                                FirestoreReadUtil.readString(doc, CartMapper.FIELD_SIZE, "42"),
                                FirestoreReadUtil.readString(doc, CartMapper.FIELD_COLOR_NAME, "Mặc định"),
                                Math.max(1, FirestoreReadUtil.readInt(doc, CartMapper.FIELD_QUANTITY, 1)),
                                FirestoreReadUtil.readBoolean(doc, CartMapper.FIELD_SELECTED, true),
                                FirestoreReadUtil.readMillis(doc, CartMapper.FIELD_UPDATED_AT)
                        ));
                    }
                    Collections.sort(rawCartDocs, (a, b) -> Long.compare(b.updatedAtMillis, a.updatedAtMillis));
                    rebuildCartLiveData();
                });
    }

    public void stopUserListening() {
        if (cartRegistration != null) {
            cartRegistration.remove();
            cartRegistration = null;
        }
        rawCartDocs.clear();
        cartItemsLiveData.setValue(new ArrayList<>());
        cartTotalCountLiveData.setValue(0);
    }

    public void rebuildCartLiveData() {
        ProductRepository productRepository = ProductRepository.getInstance();
        List<CartItemModel> resolved = new ArrayList<>();
        int totalQty = 0;
        for (RawCartDoc raw : rawCartDocs) {
            ProductModel product = productRepository.getProductById(raw.productId);
            if (product == null) {
                continue;
            }
            resolved.add(new CartItemModel(
                    raw.id,
                    product,
                    raw.variantId,
                    raw.size,
                    raw.colorName,
                    raw.quantity,
                    raw.selected,
                    raw.updatedAtMillis
            ));
            totalQty += raw.quantity;
        }
        cartItemsLiveData.setValue(resolved);
        cartTotalCountLiveData.setValue(totalQty);
    }

    // ===== 2. ĐỌC =====

    public LiveData<List<CartItemModel>> getCartItemsLiveData() {
        return cartItemsLiveData;
    }

    public LiveData<Integer> getCartTotalCountLiveData() {
        return cartTotalCountLiveData;
    }

    @NonNull
    public List<CartItemModel> getCurrentCartItems() {
        List<CartItemModel> list = cartItemsLiveData.getValue();
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    @NonNull
    public List<CartItemModel> getSelectedCartItems() {
        List<CartItemModel> selected = new ArrayList<>();
        for (CartItemModel item : getCurrentCartItems()) {
            if (item.isSelected()) {
                selected.add(item.copy());
            }
        }
        return selected;
    }

    public LiveData<List<VoucherModel>> getVouchersLiveData() {
        return VoucherRepository.getInstance().getVouchersLiveData();
    }

    public List<VoucherModel> getVouchers() {
        return VoucherRepository.getInstance().getVouchers();
    }

    // ===== 3. GHI =====

    public boolean addToCartWithinStock(
            @NonNull ProductModel product,
            @NonNull String variantId,
            @NonNull String size,
            @NonNull String colorName,
            int quantity,
            int maxStock
    ) {
        int existingQty = CartService.getQuantityForVariant(
                cartItemsLiveData.getValue(),
                product.getId(),
                variantId,
                size,
                colorName
        );
        if (CartService.validateStockForAdd(existingQty, quantity, maxStock) != null) {
            return false;
        }
        addToCart(product, variantId, size, colorName, quantity);
        return true;
    }

    public void addToCart(
            @NonNull ProductModel product,
            @NonNull String variantId,
            @NonNull String size,
            @NonNull String colorName,
            int quantity
    ) {
        String uid = provider.getCurrentUid();
        CollectionReference cartRef = userCartRef();
        if (uid == null || cartRef == null) {
            return;
        }
        Timestamp now = Timestamp.now();
        CartItemModel existing = CartService.findMatchingItem(
                cartItemsLiveData.getValue(),
                product.getId(),
                variantId,
                size,
                colorName
        );
        if (existing != null) {
            cartRef.document(existing.getId())
                    .update(CartMapper.cartMergeUpdateMap(existing.getQuantity() + Math.max(1, quantity), now))
                    .addOnFailureListener(e -> Log.w(TAG, "Merge cart item failed: " + e.getMessage()));
            return;
        }

        String docId = cartRef.document().getId();
        cartRef.document(docId)
                .set(CartMapper.cartItemToMap(
                        docId, uid, product.getId(), variantId, size, colorName, quantity, true, now
                ))
                .addOnFailureListener(e -> Log.w(TAG, "Add cart item failed: " + e.getMessage()));
    }

    public void updateQuantity(
            @NonNull CartItemModel item,
            int delta,
            @Nullable ResourceCallback<Void> callback
    ) {
        CollectionReference cartRef = userCartRef();
        if (cartRef == null) {
            notifyError(callback, "Vui lòng đăng nhập");
            return;
        }
        int nextQty = item.getQuantity() + delta;
        if (delta <= 0) {
            if (nextQty <= 0) {
                cartRef.document(item.getId()).delete()
                        .addOnSuccessListener(unused -> notifySuccess(callback))
                        .addOnFailureListener(e -> notifyError(callback, "Không thể cập nhật giỏ hàng"));
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
                            notifyError(callback, "Biến thể sản phẩm không còn tồn tại");
                            return;
                        }
                        int stockQty = FirestoreReadUtil.readInt(doc, ProductMapper.FIELD_STOCK_QTY, 0);
                        handleStockCheckAndUpdate(cartRef, item.getId(), nextQty, stockQty, callback);
                    })
                    .addOnFailureListener(e -> notifyError(callback, "Không thể kiểm tra tồn kho, vui lòng thử lại"));
        } else {
            variantsRef.get()
                    .addOnSuccessListener(snap -> {
                        ProductVariantModel matched = null;
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            ProductVariantModel v = ProductMapper.variantFromSnapshot(doc, productId);
                            if (v.getSize().equalsIgnoreCase(item.getSize())
                                    && v.getColorName().equalsIgnoreCase(item.getColorName())) {
                                matched = v;
                                break;
                            }
                        }
                        if (matched == null) {
                            notifyError(callback, "Biến thể sản phẩm không còn tồn tại");
                            return;
                        }
                        handleStockCheckAndUpdate(cartRef, item.getId(), nextQty, matched.getStockQty(), callback);
                    })
                    .addOnFailureListener(e -> notifyError(callback, "Không thể kiểm tra tồn kho, vui lòng thử lại"));
        }
    }

    public void toggleItemSelected(
            @NonNull String cartItemId,
            boolean selected,
            @Nullable ResourceCallback<Void> callback
    ) {
        CollectionReference cartRef = userCartRef();
        if (cartRef == null) {
            notifyError(callback, "Vui lòng đăng nhập");
            return;
        }
        cartRef.document(cartItemId).update(CartMapper.FIELD_SELECTED, selected)
                .addOnSuccessListener(unused -> notifySuccess(callback))
                .addOnFailureListener(e -> notifyError(callback, "Không thể cập nhật lựa chọn"));
    }

    public void selectAll(
            @Nullable List<CartItemModel> currentItems,
            boolean selected,
            @Nullable ResourceCallback<Void> callback
    ) {
        CollectionReference cartRef = userCartRef();
        if (cartRef == null) {
            notifyError(callback, "Vui lòng đăng nhập");
            return;
        }
        if (currentItems == null || currentItems.isEmpty()) {
            notifySuccess(callback);
            return;
        }
        WriteBatch batch = provider.getFirestore().batch();
        for (CartItemModel item : currentItems) {
            batch.update(cartRef.document(item.getId()), CartMapper.FIELD_SELECTED, selected);
        }
        batch.commit()
                .addOnSuccessListener(unused -> notifySuccess(callback))
                .addOnFailureListener(e -> notifyError(callback, "Không thể cập nhật giỏ hàng"));
    }

    public void removeItem(@NonNull String cartItemId, @Nullable ResourceCallback<Void> callback) {
        CollectionReference cartRef = userCartRef();
        if (cartRef == null) {
            notifyError(callback, "Vui lòng đăng nhập");
            return;
        }
        cartRef.document(cartItemId).delete()
                .addOnSuccessListener(unused -> notifySuccess(callback))
                .addOnFailureListener(e -> notifyError(callback, "Không thể xóa sản phẩm khỏi giỏ hàng"));
    }

    // ===== 4. PRIVATE HELPERS =====

    private void handleStockCheckAndUpdate(
            @NonNull CollectionReference cartRef,
            @NonNull String cartItemId,
            int nextQty,
            int stockQty,
            @Nullable ResourceCallback<Void> callback
    ) {
        String error = CartService.validateStockForNextQuantity(nextQty, stockQty);
        if (error != null) {
            notifyError(callback, error);
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
        cartRef.document(cartItemId)
                .update(CartMapper.cartQuantityUpdateMap(nextQty, Timestamp.now()))
                .addOnSuccessListener(unused -> notifySuccess(callback))
                .addOnFailureListener(e -> notifyError(callback, "Không thể cập nhật số lượng"));
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

    private static void notifySuccess(@Nullable ResourceCallback<Void> callback) {
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    private static void notifyError(@Nullable ResourceCallback<Void> callback, @NonNull String message) {
        if (callback != null) {
            callback.onError(message);
        }
    }

    private static final class RawCartDoc {
        final String id;
        final String productId;
        final String variantId;
        final String size;
        final String colorName;
        final int quantity;
        final boolean selected;
        final long updatedAtMillis;

        RawCartDoc(String id, String productId, String variantId, String size, String colorName,
                   int quantity, boolean selected, long updatedAtMillis) {
            this.id = id;
            this.productId = productId;
            this.variantId = variantId;
            this.size = size;
            this.colorName = colorName;
            this.quantity = quantity;
            this.selected = selected;
            this.updatedAtMillis = updatedAtMillis;
        }
    }
}
