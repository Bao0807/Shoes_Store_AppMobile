package com.example.shoes_store.module.admin;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.notification.NotificationRepository;
import com.example.shoes_store.module.order.OrderMapper;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.order.OrderService;
import com.example.shoes_store.module.payment.PaymentMapper;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.payment.PaymentService;
import com.example.shoes_store.module.product.ProductMapper;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.voucher.VoucherMapper;
import com.example.shoes_store.module.voucher.VoucherModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Repository cho Kênh Quản trị (Admin): lắng nghe toàn bộ Đơn hàng, Sản phẩm (kể cả đang ẩn),
 * Voucher (kể cả đang tắt) và thực thi các Transaction duyệt đơn / Batch quản lý kho & ưu đãi.
 */
public class AdminRepository {

    private static final String TAG = "AdminRepository";

    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();
    private final MutableLiveData<List<OrderModel>> allOrdersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ProductModel>> allProductsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<VoucherModel>> allVouchersLiveData = new MutableLiveData<>(new ArrayList<>());

    @Nullable
    private ListenerRegistration ordersRegistration;
    @Nullable
    private ListenerRegistration productsRegistration;
    @Nullable
    private ListenerRegistration vouchersRegistration;

    // ===== 1. REALTIME =====

    public void startListening() {
        stopListening();
        FirebaseFirestore db = provider.getFirestore();

        ordersRegistration = db.collection(FirebaseProviderHelper.COL_ORDERS)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Admin orders listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<OrderModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        list.add(OrderMapper.fromSnapshot(doc));
                    }
                    Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()));
                    allOrdersLiveData.setValue(list);
                });

        productsRegistration = db.collection(FirebaseProviderHelper.COL_PRODUCTS)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Admin products listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<ProductModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        list.add(ProductMapper.productFromSnapshot(doc));
                    }
                    Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()));
                    allProductsLiveData.setValue(list);
                });

        vouchersRegistration = db.collection(FirebaseProviderHelper.COL_VOUCHERS)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Admin vouchers listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<VoucherModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        list.add(VoucherMapper.fromSnapshot(doc));
                    }
                    Collections.sort(list, Comparator.comparingLong(VoucherModel::getMinOrderAmount));
                    allVouchersLiveData.setValue(list);
                });
    }

    public void stopListening() {
        if (ordersRegistration != null) {
            ordersRegistration.remove();
            ordersRegistration = null;
        }
        if (productsRegistration != null) {
            productsRegistration.remove();
            productsRegistration = null;
        }
        if (vouchersRegistration != null) {
            vouchersRegistration.remove();
            vouchersRegistration = null;
        }
    }

    // ===== 2. ĐỌC =====

    public LiveData<List<OrderModel>> getAllOrdersLiveData() {
        return allOrdersLiveData;
    }

    public LiveData<List<ProductModel>> getAllProductsLiveData() {
        return allProductsLiveData;
    }

    public LiveData<List<VoucherModel>> getAllVouchersLiveData() {
        return allVouchersLiveData;
    }

    // ===== 3. GHI / TRANSACTION QUẢN TRỊ =====

    public void updateOrderStatus(
            @NonNull String orderId,
            @NonNull OrderModel.Status targetStatus,
            @NonNull ResourceCallback<Void> callback
    ) {
        FirebaseFirestore db = provider.getFirestore();
        db.collection(FirebaseProviderHelper.COL_PAYMENTS)
                .whereEqualTo(PaymentMapper.FIELD_ORDER_ID, orderId)
                .get()
                .addOnCompleteListener(payTask -> {
                    DocumentReference latestPaymentRef = null;
                    if (payTask.isSuccessful() && payTask.getResult() != null) {
                        long newestMillis = -1L;
                        for (DocumentSnapshot doc : payTask.getResult().getDocuments()) {
                            long created = FirestoreReadUtil.readMillis(doc, PaymentMapper.FIELD_CREATED_AT);
                            if (latestPaymentRef == null || created >= newestMillis) {
                                newestMillis = created;
                                latestPaymentRef = doc.getReference();
                            }
                        }
                    }
                    runAdminOrderStatusTransaction(db, orderId, targetStatus, latestPaymentRef, callback);
                });
    }

    public void saveProductWithVariants(
            @Nullable String existingProductId,
            @NonNull String name,
            @NonNull String brand,
            @NonNull String category,
            long price,
            long originalPrice,
            @NonNull String thumbnailUrl,
            @NonNull String weightGrams,
            @NonNull String heelDropMm,
            @NonNull String cushionLevel,
            @NonNull String surfaceType,
            @NonNull String description,
            @NonNull List<String> colorNames,
            @NonNull List<String> colorHexes,
            @NonNull List<String> sizes,
            int stockPerVariant,
            boolean isActive,
            @NonNull ResourceCallback<Void> callback
    ) {
        String error = AdminService.validateProductForm(name, brand, category, price, originalPrice, stockPerVariant);
        if (error != null) {
            callback.onError(error);
            return;
        }

        FirebaseFirestore db = provider.getFirestore();
        boolean isNew = existingProductId == null || existingProductId.trim().isEmpty();
        String productId = isNew
                ? "p_" + System.currentTimeMillis()
                : existingProductId.trim();
        DocumentReference productRef = db.collection(FirebaseProviderHelper.COL_PRODUCTS).document(productId);

        String cleanThumb = thumbnailUrl.trim().isEmpty()
                ? "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=1000&q=85"
                : thumbnailUrl.trim();
        long finalOriginalPrice = originalPrice > price ? originalPrice : price;
        List<String> outOfStockSizes = stockPerVariant <= 0 ? new ArrayList<>(sizes) : new ArrayList<>();
        Timestamp now = Timestamp.now();

        Map<String, Object> productData = new HashMap<>();
        productData.put(ProductMapper.FIELD_PRODUCT_ID, productId);
        productData.put(ProductMapper.FIELD_NAME, name.trim());
        productData.put(ProductMapper.FIELD_BRAND, brand.trim());
        productData.put(ProductMapper.FIELD_CATEGORY, category.trim());
        productData.put(ProductMapper.FIELD_PRICE, price);
        productData.put(ProductMapper.FIELD_ORIGINAL_PRICE, finalOriginalPrice);
        productData.put(ProductMapper.FIELD_THUMBNAIL_URL, cleanThumb);
        productData.put(ProductMapper.FIELD_GALLERY_URLS, Collections.singletonList(cleanThumb));
        productData.put(ProductMapper.FIELD_WEIGHT_GRAMS, weightGrams.trim().isEmpty() ? "250g" : weightGrams.trim());
        productData.put(ProductMapper.FIELD_HEEL_DROP_MM, heelDropMm.trim().isEmpty() ? "8 mm" : heelDropMm.trim());
        productData.put(ProductMapper.FIELD_CUSHION_LEVEL, cushionLevel.trim().isEmpty() ? "Cao" : cushionLevel.trim());
        productData.put(ProductMapper.FIELD_SURFACE_TYPE, surfaceType.trim().isEmpty() ? "Road" : surfaceType.trim());
        productData.put(ProductMapper.FIELD_DESCRIPTION, description.trim().isEmpty()
                ? "Giày chạy bộ chính hãng phân phối bởi Shoes Store."
                : description.trim());
        productData.put(ProductMapper.FIELD_COLOR_NAMES, colorNames);
        productData.put(ProductMapper.FIELD_COLOR_HEXES, colorHexes);
        productData.put(ProductMapper.FIELD_ALL_SIZES, sizes);
        productData.put(ProductMapper.FIELD_OUT_OF_STOCK_SIZES, outOfStockSizes);
        productData.put(ProductMapper.FIELD_IS_ACTIVE, isActive);
        if (isNew) {
            productData.put(ProductMapper.FIELD_RATING_AVG, 0.0);
            productData.put(ProductMapper.FIELD_REVIEW_COUNT, 0);
            productData.put(ProductMapper.FIELD_CREATED_AT, now);
        }

        WriteBatch batch = db.batch();
        batch.set(productRef, productData, SetOptions.merge());

        for (int cIdx = 0; cIdx < colorNames.size(); cIdx++) {
            String cName = colorNames.get(cIdx);
            String cHex = cIdx < colorHexes.size() ? colorHexes.get(cIdx) : "#111827";
            for (String size : sizes) {
                String variantId = "v_" + cIdx + "_" + size.replace(".", "_");
                DocumentReference vRef = productRef
                        .collection(FirebaseProviderHelper.SUB_VARIANTS)
                        .document(variantId);
                Map<String, Object> vData = new HashMap<>();
                vData.put(ProductMapper.FIELD_VARIANT_ID, variantId);
                vData.put(ProductMapper.FIELD_PRODUCT_ID, productId);
                vData.put(ProductMapper.FIELD_COLOR_NAME, cName);
                vData.put(ProductMapper.FIELD_COLOR_HEX, cHex);
                vData.put(ProductMapper.FIELD_IMAGE_URL, cleanThumb);
                vData.put(ProductMapper.FIELD_SIZE, size);
                vData.put(ProductMapper.FIELD_STOCK_QTY, stockPerVariant);
                vData.put(ProductMapper.FIELD_SKU, productId.toUpperCase(Locale.ROOT) + "-" + cIdx + "-" + size);
                batch.set(vRef, vData, SetOptions.merge());
            }
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Không thể lưu sản phẩm và tồn kho"
                ));
    }

    public void toggleProductActive(
            @NonNull String productId,
            boolean isActive,
            @NonNull ResourceCallback<Void> callback
    ) {
        provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_PRODUCTS)
                .document(productId)
                .update(ProductMapper.FIELD_IS_ACTIVE, isActive)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError("Không thể cập nhật trạng thái sản phẩm"));
    }

    public void saveVoucher(
            @NonNull String code,
            @NonNull String shortBadge,
            @NonNull String title,
            @NonNull String description,
            long discountAmount,
            long minOrderAmount,
            int usageLimit,
            int validDays,
            boolean isActive,
            @NonNull ResourceCallback<Void> callback
    ) {
        String error = AdminService.validateVoucherForm(code, title, discountAmount, minOrderAmount, usageLimit);
        if (error != null) {
            callback.onError(error);
            return;
        }
        String cleanCode = code.trim().toUpperCase(Locale.ROOT);
        String cleanBadge = shortBadge.trim().isEmpty()
                ? "-" + (discountAmount / 1000L) + "K"
                : shortBadge.trim();
        long expiryMillis = System.currentTimeMillis() + Math.max(1, validDays) * 86_400_000L;

        Map<String, Object> data = new HashMap<>();
        data.put(VoucherMapper.FIELD_CODE, cleanCode);
        data.put(VoucherMapper.FIELD_SHORT_BADGE, cleanBadge);
        data.put(VoucherMapper.FIELD_TITLE, title.trim());
        data.put(VoucherMapper.FIELD_DESCRIPTION, description.trim().isEmpty()
                ? "Áp dụng cho mọi dòng giày chạy bộ đủ điều kiện"
                : description.trim());
        data.put(VoucherMapper.FIELD_DISCOUNT_AMOUNT, discountAmount);
        data.put(VoucherMapper.FIELD_MIN_ORDER_AMOUNT, minOrderAmount);
        data.put(VoucherMapper.FIELD_USAGE_LIMIT, usageLimit);
        data.put(VoucherMapper.FIELD_EXPIRY_DATE, new Timestamp(new Date(expiryMillis)));
        data.put(VoucherMapper.FIELD_IS_ACTIVE, isActive);

        DocumentReference ref = provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_VOUCHERS)
                .document(cleanCode);

        ref.get().addOnSuccessListener(existing -> {
            if (!existing.exists()) {
                data.put(VoucherMapper.FIELD_USED_COUNT, 0);
            }
            ref.set(data, SetOptions.merge())
                    .addOnSuccessListener(unused -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onError("Không thể lưu mã giảm giá"));
        }).addOnFailureListener(e -> callback.onError("Không thể kiểm tra mã giảm giá"));
    }

    public void toggleVoucherActive(
            @NonNull String code,
            boolean isActive,
            @NonNull ResourceCallback<Void> callback
    ) {
        provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_VOUCHERS)
                .document(code)
                .update(VoucherMapper.FIELD_IS_ACTIVE, isActive)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError("Không thể cập nhật trạng thái voucher"));
    }

    // ===== 4. PRIVATE HELPERS =====

    private void runAdminOrderStatusTransaction(
            @NonNull FirebaseFirestore db,
            @NonNull String orderId,
            @NonNull OrderModel.Status targetStatus,
            @Nullable DocumentReference paymentRef,
            @NonNull ResourceCallback<Void> callback
    ) {
        DocumentReference orderRef = db.collection(FirebaseProviderHelper.COL_ORDERS).document(orderId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // B1. ĐỌC toàn bộ trước khi ghi
            DocumentSnapshot orderSnap = transaction.get(orderRef);
            if (!orderSnap.exists()) {
                throw new FirebaseFirestoreException("Không tìm thấy đơn hàng", FirebaseFirestoreException.Code.ABORTED);
            }
            OrderModel.Status currentStatus = OrderModel.Status.fromStorage(
                    FirestoreReadUtil.readString(orderSnap, OrderMapper.FIELD_STATUS, "PENDING")
            );
            if (!AdminService.canAdvanceOrderStatus(currentStatus, targetStatus)) {
                throw new FirebaseFirestoreException(
                        "Không thể chuyển trạng thái từ " + currentStatus.getLabel() + " sang " + targetStatus.getLabel(),
                        FirebaseFirestoreException.Code.ABORTED
                );
            }

            String customerUid = FirestoreReadUtil.readString(orderSnap, OrderMapper.FIELD_USER_ID, "");
            String orderCode = FirestoreReadUtil.readString(orderSnap, OrderMapper.FIELD_ORDER_CODE, orderId);
            String voucherCode = FirestoreReadUtil.readString(orderSnap, OrderMapper.FIELD_VOUCHER_CODE, "")
                    .trim().toUpperCase(Locale.ROOT);

            Map<String, DocumentReference> variantRefs = new LinkedHashMap<>();
            Map<String, Integer> restoreQtyByKey = new LinkedHashMap<>();
            Map<String, Integer> currentStockByKey = new LinkedHashMap<>();
            DocumentSnapshot voucherSnap = null;

            if (targetStatus == OrderModel.Status.CANCELLED) {
                Object rawItems = orderSnap.get(OrderMapper.FIELD_ITEMS);
                if (rawItems instanceof List<?>) {
                    for (Object obj : (List<?>) rawItems) {
                        if (obj instanceof Map<?, ?>) {
                            Map<?, ?> m = (Map<?, ?>) obj;
                            String productId = FirestoreReadUtil.mapString(m, OrderMapper.ITEM_PRODUCT_ID, "");
                            String variantId = FirestoreReadUtil.mapString(m, OrderMapper.ITEM_VARIANT_ID, "");
                            int qty = FirestoreReadUtil.mapInt(m, OrderMapper.ITEM_QUANTITY, 1);
                            if (!productId.isEmpty() && !variantId.isEmpty()) {
                                String key = OrderService.buildVariantKey(productId, variantId);
                                variantRefs.put(key, db.collection(FirebaseProviderHelper.COL_PRODUCTS)
                                        .document(productId)
                                        .collection(FirebaseProviderHelper.SUB_VARIANTS)
                                        .document(variantId));
                                restoreQtyByKey.put(key, restoreQtyByKey.getOrDefault(key, 0) + qty);
                            }
                        }
                    }
                }
                for (Map.Entry<String, DocumentReference> entry : variantRefs.entrySet()) {
                    DocumentSnapshot vSnap = transaction.get(entry.getValue());
                    if (vSnap.exists()) {
                        currentStockByKey.put(
                                entry.getKey(),
                                FirestoreReadUtil.readInt(vSnap, ProductMapper.FIELD_STOCK_QTY, 0)
                        );
                    }
                }
                if (!voucherCode.isEmpty()) {
                    DocumentReference voucherRef = db.collection(FirebaseProviderHelper.COL_VOUCHERS).document(voucherCode);
                    voucherSnap = transaction.get(voucherRef);
                }
            }

            DocumentSnapshot paymentSnap = paymentRef != null ? transaction.get(paymentRef) : null;

            // B2. GHI đồng bộ
            Timestamp now = Timestamp.now();
            transaction.update(
                    orderRef,
                    OrderMapper.FIELD_STATUS, targetStatus.name(),
                    OrderMapper.FIELD_UPDATED_AT, now
            );

            if (targetStatus == OrderModel.Status.CANCELLED) {
                for (Map.Entry<String, Integer> stockEntry : currentStockByKey.entrySet()) {
                    DocumentReference vRef = variantRefs.get(stockEntry.getKey());
                    int addQty = restoreQtyByKey.getOrDefault(stockEntry.getKey(), 0);
                    if (vRef != null && addQty > 0) {
                        transaction.update(
                                vRef,
                                ProductMapper.FIELD_STOCK_QTY,
                                OrderService.computeRestoredStock(stockEntry.getValue(), addQty)
                        );
                    }
                }
                if (voucherSnap != null && voucherSnap.exists()) {
                    long currentUsed = FirestoreReadUtil.readLong(voucherSnap, VoucherMapper.FIELD_USED_COUNT, 0L);
                    transaction.update(
                            voucherSnap.getReference(),
                            VoucherMapper.FIELD_USED_COUNT,
                            OrderService.computeNextVoucherUsedCountOnCancel(currentUsed)
                    );
                }
                if (paymentSnap != null && paymentSnap.exists()) {
                    String currentPayStatus = FirestoreReadUtil.readString(paymentSnap, PaymentMapper.FIELD_STATUS, "UNPAID");
                    PaymentModel.Status nextPayStatus = PaymentService.resolveStatusOnOrderCancel(currentPayStatus);
                    transaction.update(paymentSnap.getReference(), PaymentMapper.FIELD_STATUS, nextPayStatus.name());
                }
            } else if (targetStatus == OrderModel.Status.COMPLETED) {
                if (paymentSnap != null && paymentSnap.exists()) {
                    PaymentModel.Status paidStatus = AdminService.resolvePaymentStatusOnOrderComplete();
                    String existingTxn = FirestoreReadUtil.readString(paymentSnap, PaymentMapper.FIELD_TRANSACTION_CODE, "");
                    String nextTxn = existingTxn.isEmpty()
                            ? PaymentService.buildTransactionCode(paidStatus, orderCode)
                            : existingTxn;
                    transaction.update(
                            paymentSnap.getReference(),
                            PaymentMapper.FIELD_STATUS, paidStatus.name(),
                            PaymentMapper.FIELD_PAID_AT, now,
                            PaymentMapper.FIELD_TRANSACTION_CODE, nextTxn
                    );
                }
            }

            if (!customerUid.isEmpty()) {
                DocumentReference notifRef = db.collection(FirebaseProviderHelper.COL_USERS)
                        .document(customerUid)
                        .collection(FirebaseProviderHelper.SUB_NOTIFICATIONS)
                        .document();
                transaction.set(notifRef, NotificationRepository.buildOrderNotificationData(
                        notifRef.getId(),
                        customerUid,
                        AdminService.buildOrderStatusNotificationTitle(orderCode, targetStatus),
                        AdminService.buildOrderStatusNotificationMessage(targetStatus),
                        orderCode,
                        now
                ));
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess(null))
          .addOnFailureListener(e -> callback.onError(
                  e.getMessage() != null ? e.getMessage() : "Không thể cập nhật trạng thái đơn hàng"
          ));
    }
}
