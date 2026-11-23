package com.example.shoes_store.module.order;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreStoreHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.cart.CartVoucherModel;
import com.example.shoes_store.module.notification.NotificationModel;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.payment.PaymentRepository;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.product.ProductVariantModel;
import com.example.shoes_store.module.profile.ProfileAddressModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Repository cho Đặt hàng và Hủy đơn với Firestore Transaction nguyên tử, phối hợp cùng {@link PaymentRepository}.
 */
public class OrderRepository {

    private final FirestoreStoreHelper store = FirestoreStoreHelper.getInstance();
    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();
    private final PaymentRepository paymentRepository = new PaymentRepository();

    @Nullable
    public ProductModel getProductById(@Nullable String productId) {
        return store.getProductById(productId);
    }

    public List<CartItemModel> getSelectedCartItems() {
        return store.getSelectedCartItems();
    }

    @Nullable
    public ProfileAddressModel getDefaultAddress() {
        return store.getDefaultAddress();
    }

    @Nullable
    public ProfileAddressModel findAddressById(@Nullable String addressId) {
        return store.findAddressById(addressId);
    }

    public LiveData<List<ProfileAddressModel>> getAddressesLiveData() {
        return store.getAddressesLiveData();
    }

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return store.getOrdersLiveData();
    }

    @Nullable
    public OrderModel getOrderByIdOrCode(@Nullable String orderIdOrCode) {
        if (orderIdOrCode == null) {
            return null;
        }
        OrderModel byId = store.getOrderById(orderIdOrCode);
        return byId != null ? byId : store.getOrderByCode(orderIdOrCode);
    }

    public List<CartVoucherModel> getVouchers() {
        return store.getVouchers();
    }

    public LiveData<List<CartVoucherModel>> getVouchersLiveData() {
        return store.getVouchersLiveData();
    }

    @Nullable
    public CartVoucherModel findVoucherByCode(@Nullable String code) {
        if (code == null) {
            return null;
        }
        for (CartVoucherModel voucher : store.getVouchers()) {
            if (voucher.getCode().equalsIgnoreCase(code)) {
                return voucher;
            }
        }
        return null;
    }

    public void placeOrder(
            @NonNull List<CartItemModel> items,
            @NonNull ProfileAddressModel address,
            @NonNull String shippingMethod,
            long shippingFee,
            @NonNull PaymentModel.Method paymentMethod,
            @Nullable String voucherCode,
            long requestedDiscount,
            boolean removeFromCart,
            @NonNull ResourceCallback<OrderModel> callback
    ) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            callback.onError("Vui lòng đăng nhập để đặt hàng");
            return;
        }
        if (items.isEmpty()) {
            callback.onError("Không có sản phẩm để thanh toán");
            return;
        }

        ensureItemVariantIds(items, () -> executePlaceOrderTransaction(
                uid,
                items,
                address,
                shippingMethod,
                shippingFee,
                paymentMethod,
                voucherCode,
                requestedDiscount,
                removeFromCart,
                callback
        ));
    }

    private void ensureItemVariantIds(@NonNull List<CartItemModel> items, @NonNull Runnable onReady) {
        int pendingCount = 0;
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

        final int totalToFetch = missingIndices.size();
        final int[] completed = {0};
        FirebaseFirestore db = provider.getFirestore();

        for (int idx : missingIndices) {
            CartItemModel item = items.get(idx);
            String productId = item.getProduct().getId();
            db.collection(FirebaseProviderHelper.COL_PRODUCTS)
                    .document(productId)
                    .collection(FirebaseProviderHelper.SUB_VARIANTS)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<ProductVariantModel> loaded = new ArrayList<>();
                            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                                loaded.add(FirestoreStoreHelper.mapVariant(doc, productId));
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
                        completed[0]++;
                        if (completed[0] >= totalToFetch) {
                            onReady.run();
                        }
                    });
        }
    }

    private void executePlaceOrderTransaction(
            @NonNull String uid,
            @NonNull List<CartItemModel> items,
            @NonNull ProfileAddressModel address,
            @NonNull String shippingMethod,
            long shippingFee,
            @NonNull PaymentModel.Method paymentMethod,
            @Nullable String voucherCode,
            long requestedDiscount,
            boolean removeFromCart,
            @NonNull ResourceCallback<OrderModel> callback
    ) {
        FirebaseFirestore db = provider.getFirestore();
        String cleanVoucher = voucherCode != null ? voucherCode.trim().toUpperCase(Locale.ROOT) : "";

        db.runTransaction(transaction -> {
            // Giai đoạn 1: ĐỌC & KIỂM TRA TOÀN BỘ (All Reads First)
            Map<String, DocumentReference> variantRefs = new LinkedHashMap<>();
            Map<String, Integer> requestedQtyByKey = new LinkedHashMap<>();
            Map<String, String> itemLabelByKey = new LinkedHashMap<>();

            long subtotal = 0L;
            List<OrderItemModel> orderItems = new ArrayList<>();
            for (CartItemModel item : items) {
                if (item.getVariantId().isEmpty()) {
                    throw new FirebaseFirestoreException(
                            "Không xác định được biến thể cho " + item.getProduct().getName() + " (EU " + item.getSize() + ")",
                            FirebaseFirestoreException.Code.ABORTED
                    );
                }
                String key = item.getProduct().getId() + "|" + item.getVariantId();
                if (!variantRefs.containsKey(key)) {
                    variantRefs.put(
                            key,
                            db.collection(FirebaseProviderHelper.COL_PRODUCTS)
                                    .document(item.getProduct().getId())
                                    .collection(FirebaseProviderHelper.SUB_VARIANTS)
                                    .document(item.getVariantId())
                    );
                    itemLabelByKey.put(key, item.getProduct().getName() + " (EU " + item.getSize() + ")");
                }
                requestedQtyByKey.put(key, requestedQtyByKey.getOrDefault(key, 0) + item.getQuantity());
                subtotal += item.getLineTotal();
                orderItems.add(OrderItemModel.fromCartItem(item));
            }

            Map<String, Integer> currentStockByKey = new LinkedHashMap<>();
            for (Map.Entry<String, DocumentReference> entry : variantRefs.entrySet()) {
                DocumentSnapshot snap = transaction.get(entry.getValue());
                if (!snap.exists()) {
                    throw new FirebaseFirestoreException(
                            "Sản phẩm " + itemLabelByKey.get(entry.getKey()) + " không còn tồn tại",
                            FirebaseFirestoreException.Code.ABORTED
                    );
                }
                int currentStock = FirestoreStoreHelper.readInt(snap, "stockQty", 0);
                int needed = requestedQtyByKey.getOrDefault(entry.getKey(), 0);
                if (currentStock < needed) {
                    throw new FirebaseFirestoreException(
                            itemLabelByKey.get(entry.getKey()) + " chỉ còn " + currentStock + " đôi trong kho",
                            FirebaseFirestoreException.Code.ABORTED
                    );
                }
                currentStockByKey.put(entry.getKey(), currentStock);
            }

            DocumentReference voucherRef = null;
            long effectiveDiscount = 0L;
            long nextVoucherUsedCount = 0L;
            if (!cleanVoucher.isEmpty()) {
                voucherRef = db.collection(FirebaseProviderHelper.COL_VOUCHERS).document(cleanVoucher);
                DocumentSnapshot voucherSnap = transaction.get(voucherRef);
                if (!voucherSnap.exists()) {
                    throw new FirebaseFirestoreException(
                            "Mã giảm giá " + cleanVoucher + " không tồn tại",
                            FirebaseFirestoreException.Code.ABORTED
                    );
                }
                boolean active = FirestoreStoreHelper.readBoolean(voucherSnap, "isActive", false);
                long minOrder = FirestoreStoreHelper.readLong(voucherSnap, "minOrderAmount", 0L);
                long voucherDiscount = FirestoreStoreHelper.readLong(voucherSnap, "discountAmount", requestedDiscount);
                long usageLimit = FirestoreStoreHelper.readLong(voucherSnap, "usageLimit", 0L);
                long usedCount = FirestoreStoreHelper.readLong(voucherSnap, "usedCount", 0L);
                long expiryMillis = FirestoreStoreHelper.readMillis(voucherSnap, "expiryDate");

                if (!active || (expiryMillis > 0L && System.currentTimeMillis() > expiryMillis)) {
                    throw new FirebaseFirestoreException(
                            "Mã giảm giá " + cleanVoucher + " đã hết hạn",
                            FirebaseFirestoreException.Code.ABORTED
                    );
                }
                if (subtotal < minOrder) {
                    throw new FirebaseFirestoreException(
                            "Đơn hàng chưa đạt mức tối thiểu để dùng mã " + cleanVoucher,
                            FirebaseFirestoreException.Code.ABORTED
                    );
                }
                if (usageLimit > 0L && usedCount >= usageLimit) {
                    throw new FirebaseFirestoreException(
                            "Mã giảm giá " + cleanVoucher + " đã hết lượt sử dụng",
                            FirebaseFirestoreException.Code.ABORTED
                    );
                }
                effectiveDiscount = Math.min(voucherDiscount, subtotal);
                nextVoucherUsedCount = usedCount + 1L;
            }

            // Giai đoạn 2: GHI ĐỒNG BỘ TOÀN BỘ (Atomic Writes)
            Timestamp now = Timestamp.now();
            long nowMillis = now.toDate().getTime();

            for (Map.Entry<String, DocumentReference> entry : variantRefs.entrySet()) {
                int currentStock = currentStockByKey.getOrDefault(entry.getKey(), 0);
                int needed = requestedQtyByKey.getOrDefault(entry.getKey(), 0);
                transaction.update(entry.getValue(), "stockQty", Math.max(0, currentStock - needed));
            }

            if (voucherRef != null) {
                transaction.update(voucherRef, "usedCount", nextVoucherUsedCount);
            }

            long total = Math.max(0L, subtotal + shippingFee - effectiveDiscount);
            DocumentReference orderRef = db.collection(FirebaseProviderHelper.COL_ORDERS).document();
            String orderId = orderRef.getId();
            String orderCode = "RN" + new SimpleDateFormat("yyMMddHHmmss", Locale.US).format(new Date(nowMillis));

            List<Map<String, Object>> itemMaps = new ArrayList<>();
            for (OrderItemModel oi : orderItems) {
                Map<String, Object> m = new HashMap<>();
                m.put("productId", oi.getProductId());
                m.put("variantId", oi.getVariantId());
                m.put("productName", oi.getProductName());
                m.put("thumbnailUrl", oi.getThumbnailUrl());
                m.put("size", oi.getSize());
                m.put("colorName", oi.getColorName());
                m.put("unitPrice", oi.getUnitPrice());
                m.put("quantity", oi.getQuantity());
                m.put("lineTotal", oi.getLineTotal());
                itemMaps.add(m);
            }

            Map<String, Object> orderData = new HashMap<>();
            orderData.put("orderId", orderId);
            orderData.put("orderCode", orderCode);
            orderData.put("userId", uid);
            orderData.put("status", OrderModel.Status.PENDING.name());
            orderData.put("paymentMethod", paymentMethod.name());
            orderData.put("receiverName", address.getReceiverName());
            orderData.put("receiverPhone", address.getPhone());
            orderData.put("shippingAddress", address.getAddressDetail());
            orderData.put("shippingMethod", shippingMethod);
            orderData.put("voucherCode", cleanVoucher);
            orderData.put("items", itemMaps);
            orderData.put("subtotal", subtotal);
            orderData.put("shippingFee", shippingFee);
            orderData.put("discount", effectiveDiscount);
            orderData.put("total", total);
            orderData.put("createdAt", now);
            orderData.put("updatedAt", now);
            transaction.set(orderRef, orderData);

            DocumentReference paymentRef = paymentRepository.newPaymentDocumentRef(db);
            Map<String, Object> paymentData = PaymentRepository.buildPaymentData(
                    paymentRef.getId(), orderId, uid, paymentMethod, total, orderCode, now
            );
            transaction.set(paymentRef, paymentData);

            if (removeFromCart) {
                CollectionReference cartRef = db.collection(FirebaseProviderHelper.COL_USERS)
                        .document(uid)
                        .collection(FirebaseProviderHelper.SUB_CART);
                for (CartItemModel item : items) {
                    if (!item.getId().isEmpty() && !item.getId().startsWith("direct_")) {
                        transaction.delete(cartRef.document(item.getId()));
                    }
                }
            }

            DocumentReference notifRef = db.collection(FirebaseProviderHelper.COL_USERS)
                    .document(uid)
                    .collection(FirebaseProviderHelper.SUB_NOTIFICATIONS)
                    .document();
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("notificationId", notifRef.getId());
            notifData.put("userId", uid);
            notifData.put("title", "Đặt hàng #" + orderCode + " thành công");
            notifData.put("message", "Đơn hàng đang chờ xác nhận (" + paymentMethod.getLabel() + ").");
            notifData.put("type", NotificationModel.Type.ORDER.name());
            notifData.put("targetId", orderCode);
            notifData.put("unread", true);
            notifData.put("createdAt", now);
            transaction.set(notifRef, notifData);

            return new OrderModel(
                    orderId,
                    orderCode,
                    uid,
                    OrderModel.Status.PENDING,
                    paymentMethod,
                    orderItems,
                    address.getAddressDetail(),
                    address.getReceiverName() + " · " + address.getPhone(),
                    shippingMethod,
                    cleanVoucher,
                    subtotal,
                    shippingFee,
                    effectiveDiscount,
                    total,
                    nowMillis,
                    nowMillis
            );
        }).addOnSuccessListener(callback::onSuccess)
          .addOnFailureListener(e -> callback.onError(
                  e.getMessage() != null ? e.getMessage() : "Không thể đặt hàng, vui lòng thử lại"
          ));
    }

    public void cancelOrder(@NonNull String orderId, @NonNull ResourceCallback<Void> callback) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            callback.onError("Vui lòng đăng nhập");
            return;
        }
        FirebaseFirestore db = provider.getFirestore();

        // Truy vấn trước Payment Reference qua PaymentRepository bên ngoài Transaction (Firestore Transaction không hỗ trợ Query)
        paymentRepository.findLatestPaymentRef(orderId, uid, new ResourceCallback<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference latestPaymentRef) {
                runCancelOrderTransaction(db, uid, orderId, latestPaymentRef, callback);
            }

            @Override
            public void onError(String message) {
                runCancelOrderTransaction(db, uid, orderId, null, callback);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void runCancelOrderTransaction(
            @NonNull FirebaseFirestore db,
            @NonNull String uid,
            @NonNull String orderId,
            @Nullable DocumentReference paymentRef,
            @NonNull ResourceCallback<Void> callback
    ) {
        DocumentReference orderRef = db.collection(FirebaseProviderHelper.COL_ORDERS).document(orderId);

        db.runTransaction(transaction -> {
            // Giai đoạn 1: ĐỌC TOÀN BỘ
            DocumentSnapshot orderSnap = transaction.get(orderRef);
            if (!orderSnap.exists()) {
                throw new FirebaseFirestoreException("Không tìm thấy đơn hàng", FirebaseFirestoreException.Code.ABORTED);
            }
            String statusStr = FirestoreStoreHelper.readString(orderSnap, "status", "PENDING");
            if (!OrderModel.Status.PENDING.name().equalsIgnoreCase(statusStr)) {
                throw new FirebaseFirestoreException("Chỉ có thể hủy đơn đang chờ xác nhận", FirebaseFirestoreException.Code.ABORTED);
            }
            String orderCode = FirestoreStoreHelper.readString(orderSnap, "orderCode", orderId);

            Map<String, DocumentReference> variantRefs = new LinkedHashMap<>();
            Map<String, Integer> restoreQtyByKey = new LinkedHashMap<>();
            Object rawItems = orderSnap.get("items");
            if (rawItems instanceof List<?>) {
                for (Object obj : (List<?>) rawItems) {
                    if (obj instanceof Map<?, ?>) {
                        Map<String, Object> m = (Map<String, Object>) obj;
                        String productId = m.get("productId") != null ? String.valueOf(m.get("productId")) : "";
                        String variantId = m.get("variantId") != null ? String.valueOf(m.get("variantId")) : "";
                        int qty = m.get("quantity") instanceof Number ? ((Number) m.get("quantity")).intValue() : 1;
                        if (!productId.isEmpty() && !variantId.isEmpty()) {
                            String key = productId + "|" + variantId;
                            variantRefs.put(key, db.collection(FirebaseProviderHelper.COL_PRODUCTS)
                                    .document(productId)
                                    .collection(FirebaseProviderHelper.SUB_VARIANTS)
                                    .document(variantId));
                            restoreQtyByKey.put(key, restoreQtyByKey.getOrDefault(key, 0) + qty);
                        }
                    }
                }
            }

            Map<String, Integer> currentStockByKey = new LinkedHashMap<>();
            for (Map.Entry<String, DocumentReference> entry : variantRefs.entrySet()) {
                DocumentSnapshot vSnap = transaction.get(entry.getValue());
                if (vSnap.exists()) {
                    currentStockByKey.put(entry.getKey(), FirestoreStoreHelper.readInt(vSnap, "stockQty", 0));
                }
            }

            DocumentSnapshot paymentSnap = paymentRef != null ? transaction.get(paymentRef) : null;

            // Giai đoạn 2: GHI ĐỒNG BỘ
            Timestamp now = Timestamp.now();
            transaction.update(orderRef, "status", OrderModel.Status.CANCELLED.name(), "updatedAt", now);

            for (Map.Entry<String, Integer> stockEntry : currentStockByKey.entrySet()) {
                DocumentReference vRef = variantRefs.get(stockEntry.getKey());
                int addQty = restoreQtyByKey.getOrDefault(stockEntry.getKey(), 0);
                if (vRef != null && addQty > 0) {
                    transaction.update(vRef, "stockQty", stockEntry.getValue() + addQty);
                }
            }

            if (paymentSnap != null && paymentSnap.exists()) {
                String currentPayStatus = FirestoreStoreHelper.readString(paymentSnap, "status", "UNPAID");
                PaymentModel.Status nextPayStatus = PaymentRepository.resolveStatusOnOrderCancel(currentPayStatus);
                transaction.update(paymentSnap.getReference(), "status", nextPayStatus.name());
            }

            DocumentReference notifRef = db.collection(FirebaseProviderHelper.COL_USERS)
                    .document(uid)
                    .collection(FirebaseProviderHelper.SUB_NOTIFICATIONS)
                    .document();
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("notificationId", notifRef.getId());
            notifData.put("userId", uid);
            notifData.put("title", "Đơn #" + orderCode + " đã được hủy");
            notifData.put("message", "Số lượng sản phẩm đã được hoàn lại vào kho của cửa hàng.");
            notifData.put("type", NotificationModel.Type.ORDER.name());
            notifData.put("targetId", orderCode);
            notifData.put("unread", true);
            notifData.put("createdAt", now);
            transaction.set(notifRef, notifData);

            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess(null))
          .addOnFailureListener(e -> callback.onError(
                  e.getMessage() != null ? e.getMessage() : "Không thể hủy đơn hàng"
          ));
    }

    public void reorder(@NonNull OrderModel order, @NonNull ResourceCallback<Integer> callback) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            callback.onError("Vui lòng đăng nhập");
            return;
        }
        List<OrderItemModel> items = order.getOrderItems();
        if (items.isEmpty()) {
            callback.onError("Đơn hàng không có sản phẩm");
            return;
        }
        FirebaseFirestore db = provider.getFirestore();
        CollectionReference cartRef = db.collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_CART);

        WriteBatch batch = db.batch();
        Timestamp now = Timestamp.now();
        int count = 0;
        for (OrderItemModel item : items) {
            ProductModel product = store.getProductById(item.getProductId());
            if (product == null) {
                continue;
            }
            DocumentReference docRef = cartRef.document();
            Map<String, Object> data = new HashMap<>();
            data.put("cartItemId", docRef.getId());
            data.put("userId", uid);
            data.put("productId", item.getProductId());
            data.put("variantId", item.getVariantId());
            data.put("size", item.getSize());
            data.put("colorName", item.getColorName());
            data.put("quantity", Math.max(1, item.getQuantity()));
            data.put("selected", true);
            data.put("updatedAt", now);
            batch.set(docRef, data);
            count++;
        }
        final int addedCount = count;
        if (addedCount == 0) {
            callback.onError("Sản phẩm trong đơn hiện không còn kinh doanh");
            return;
        }
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(addedCount))
                .addOnFailureListener(e -> callback.onError("Không thể thêm lại vào giỏ hàng"));
    }
}
