package com.example.shoes_store.module.order;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.cart.CartMapper;
import com.example.shoes_store.module.cart.CartRepository;
import com.example.shoes_store.module.cart.CartService;
import com.example.shoes_store.module.notification.NotificationRepository;
import com.example.shoes_store.module.payment.PaymentMapper;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.payment.PaymentRepository;
import com.example.shoes_store.module.payment.PaymentService;
import com.example.shoes_store.module.product.ProductMapper;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.product.ProductRepository;
import com.example.shoes_store.module.product.ProductVariantModel;
import com.example.shoes_store.module.profile.ProfileAddressModel;
import com.example.shoes_store.module.profile.ProfileRepository;
import com.example.shoes_store.module.voucher.VoucherMapper;
import com.example.shoes_store.module.voucher.VoucherModel;
import com.example.shoes_store.module.voucher.VoucherRepository;
import com.example.shoes_store.module.voucher.VoucherService;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Repository sở hữu Đơn hàng (`orders`): đồng bộ realtime, Transaction đặt/hủy đơn và mua lại có kiểm tồn kho.
 */
public class OrderRepository {

    private static final String TAG = "OrderRepository";
    private static volatile OrderRepository instance;

    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();
    private final MutableLiveData<List<OrderModel>> ordersLiveData = new MutableLiveData<>(new ArrayList<>());
    @Nullable
    private ListenerRegistration ordersRegistration;

    private OrderRepository() {
    }

    public static OrderRepository getInstance() {
        if (instance == null) {
            synchronized (OrderRepository.class) {
                if (instance == null) {
                    instance = new OrderRepository();
                }
            }
        }
        return instance;
    }

    // ===== 1. REALTIME =====

    public void startUserListening(@NonNull String uid) {
        stopUserListening();
        ordersRegistration = provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_ORDERS)
                .whereEqualTo(OrderMapper.FIELD_USER_ID, uid)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Orders listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<OrderModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        list.add(OrderMapper.fromSnapshot(doc));
                    }
                    Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()));
                    ordersLiveData.setValue(list);
                });
    }

    public void stopUserListening() {
        if (ordersRegistration != null) {
            ordersRegistration.remove();
            ordersRegistration = null;
        }
        ordersLiveData.setValue(new ArrayList<>());
    }

    // ===== 2. ĐỌC =====

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return ordersLiveData;
    }

    @Nullable
    public OrderModel getOrderById(@Nullable String orderId) {
        if (orderId == null) {
            return null;
        }
        List<OrderModel> list = ordersLiveData.getValue();
        if (list == null) {
            return null;
        }
        for (OrderModel order : list) {
            if (orderId.equals(order.getOrderId())) {
                return order;
            }
        }
        return null;
    }

    @Nullable
    public OrderModel getOrderByCode(@Nullable String orderCode) {
        if (orderCode == null) {
            return null;
        }
        List<OrderModel> list = ordersLiveData.getValue();
        if (list == null) {
            return null;
        }
        for (OrderModel order : list) {
            if (orderCode.equals(order.getOrderCode()) || orderCode.equals(order.getOrderId())) {
                return order;
            }
        }
        return null;
    }

    @Nullable
    public OrderModel getOrderByIdOrCode(@Nullable String orderIdOrCode) {
        if (orderIdOrCode == null) {
            return null;
        }
        OrderModel byId = getOrderById(orderIdOrCode);
        return byId != null ? byId : getOrderByCode(orderIdOrCode);
    }

    @Nullable
    public ProductModel getProductById(@Nullable String productId) {
        return ProductRepository.getInstance().getProductById(productId);
    }

    public List<CartItemModel> getSelectedCartItems() {
        return CartRepository.getInstance().getSelectedCartItems();
    }

    @Nullable
    public ProfileAddressModel getDefaultAddress() {
        return ProfileRepository.getInstance().getDefaultAddress();
    }

    @Nullable
    public ProfileAddressModel findAddressById(@Nullable String addressId) {
        return ProfileRepository.getInstance().findAddressById(addressId);
    }

    public LiveData<List<ProfileAddressModel>> getAddressesLiveData() {
        return ProfileRepository.getInstance().getAddressesLiveData();
    }

    public List<VoucherModel> getVouchers() {
        return VoucherRepository.getInstance().getVouchers();
    }

    public LiveData<List<VoucherModel>> getVouchersLiveData() {
        return VoucherRepository.getInstance().getVouchersLiveData();
    }

    @Nullable
    public VoucherModel findVoucherByCode(@Nullable String code) {
        return VoucherRepository.getInstance().findByCode(code);
    }

    // ===== 3. GHI / TRANSACTION =====

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

    public void cancelOrder(@NonNull String orderId, @NonNull ResourceCallback<Void> callback) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            callback.onError("Vui lòng đăng nhập");
            return;
        }
        FirebaseFirestore db = provider.getFirestore();

        // Truy vấn Payment Reference qua PaymentRepository trước khi vào Transaction (Firestore không hỗ trợ Query trong Transaction)
        PaymentRepository.getInstance().findLatestPaymentRef(orderId, uid, new ResourceCallback<DocumentReference>() {
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
        ProductRepository productRepo = ProductRepository.getInstance();

        List<OrderItemModel> activeItems = new ArrayList<>();
        List<Task<QuerySnapshot>> variantTasks = new ArrayList<>();
        for (OrderItemModel item : items) {
            if (productRepo.getProductById(item.getProductId()) == null) {
                continue;
            }
            activeItems.add(item);
            variantTasks.add(db.collection(FirebaseProviderHelper.COL_PRODUCTS)
                    .document(item.getProductId())
                    .collection(FirebaseProviderHelper.SUB_VARIANTS)
                    .get());
        }
        if (activeItems.isEmpty()) {
            callback.onError("Sản phẩm trong đơn hiện không còn kinh doanh");
            return;
        }

        Tasks.whenAllComplete(variantTasks)
                .addOnCompleteListener(all -> commitReorderBatch(db, uid, activeItems, variantTasks, callback));
    }

    // ===== 4. PRIVATE HELPERS =====

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
        PaymentRepository paymentRepository = PaymentRepository.getInstance();

        db.runTransaction(transaction -> {
            // B1. ĐỌC tồn kho các biến thể và voucher
            Map<String, DocumentReference> variantRefs = new LinkedHashMap<>();
            Map<String, String> itemLabelByKey = new LinkedHashMap<>();
            List<OrderItemModel> orderItems = new ArrayList<>();
            long subtotal = OrderService.calculateSubtotal(items);

            for (CartItemModel item : items) {
                if (item.getVariantId().isEmpty()) {
                    throw abort("Không xác định được biến thể cho " + item.getProduct().getName() + " (EU " + item.getSize() + ")");
                }
                String key = OrderService.buildVariantKey(item.getProduct().getId(), item.getVariantId());
                if (!variantRefs.containsKey(key)) {
                    variantRefs.put(key, variantDocRef(db, item.getProduct().getId(), item.getVariantId()));
                    itemLabelByKey.put(key, item.getProduct().getName() + " (EU " + item.getSize() + ")");
                }
                orderItems.add(OrderItemModel.fromCartItem(item));
            }

            Map<String, Integer> requestedQtyByKey = OrderService.aggregateRequestedQuantities(items);
            Map<String, Integer> currentStockByKey = new LinkedHashMap<>();
            for (Map.Entry<String, DocumentReference> entry : variantRefs.entrySet()) {
                DocumentSnapshot snap = transaction.get(entry.getValue());
                if (snap.exists()) {
                    currentStockByKey.put(entry.getKey(), FirestoreReadUtil.readInt(snap, ProductMapper.FIELD_STOCK_QTY, 0));
                }
            }

            DocumentReference voucherRef = null;
            DocumentSnapshot voucherSnap = null;
            if (!cleanVoucher.isEmpty()) {
                voucherRef = db.collection(FirebaseProviderHelper.COL_VOUCHERS).document(cleanVoucher);
                voucherSnap = transaction.get(voucherRef);
            }

            // B2. KIỂM TRA quy tắc nghiệp vụ
            String stockError = OrderService.validateStockSufficiency(requestedQtyByKey, currentStockByKey, itemLabelByKey);
            if (stockError != null) {
                throw abort(stockError);
            }

            long effectiveDiscount = 0L;
            long nextVoucherUsedCount = 0L;
            if (voucherRef != null) {
                if (voucherSnap == null || !voucherSnap.exists()) {
                    throw abort("Mã giảm giá " + cleanVoucher + " không tồn tại");
                }
                boolean active = FirestoreReadUtil.readBoolean(voucherSnap, VoucherMapper.FIELD_IS_ACTIVE, false);
                long minOrder = FirestoreReadUtil.readLong(voucherSnap, VoucherMapper.FIELD_MIN_ORDER_AMOUNT, 0L);
                long voucherDiscount = FirestoreReadUtil.readLong(voucherSnap, VoucherMapper.FIELD_DISCOUNT_AMOUNT, requestedDiscount);
                long usageLimit = FirestoreReadUtil.readLong(voucherSnap, VoucherMapper.FIELD_USAGE_LIMIT, 0L);
                long usedCount = FirestoreReadUtil.readLong(voucherSnap, VoucherMapper.FIELD_USED_COUNT, 0L);
                long expiryMillis = FirestoreReadUtil.readMillis(voucherSnap, VoucherMapper.FIELD_EXPIRY_DATE);

                String voucherError = VoucherService.validateVoucherSnapshotForTransaction(
                        active, expiryMillis, System.currentTimeMillis(), minOrder, usageLimit, usedCount, subtotal, cleanVoucher
                );
                if (voucherError != null) {
                    throw abort(voucherError);
                }
                effectiveDiscount = Math.min(voucherDiscount, subtotal);
                nextVoucherUsedCount = usedCount + 1L;
            }

            // B3. GHI nguyên tử
            Timestamp now = Timestamp.now();
            long nowMillis = now.toDate().getTime();

            for (Map.Entry<String, DocumentReference> entry : variantRefs.entrySet()) {
                int currentStock = currentStockByKey.getOrDefault(entry.getKey(), 0);
                int needed = requestedQtyByKey.getOrDefault(entry.getKey(), 0);
                transaction.update(entry.getValue(), ProductMapper.FIELD_STOCK_QTY, Math.max(0, currentStock - needed));
            }
            if (voucherRef != null) {
                transaction.update(voucherRef, VoucherMapper.FIELD_USED_COUNT, nextVoucherUsedCount);
            }

            long total = OrderService.calculateTotal(subtotal, shippingFee, effectiveDiscount);
            DocumentReference orderRef = db.collection(FirebaseProviderHelper.COL_ORDERS).document();
            String orderId = orderRef.getId();
            String orderCode = OrderService.generateOrderCode(nowMillis);

            transaction.set(orderRef, OrderMapper.orderToMap(
                    orderId, orderCode, uid, paymentMethod, address, shippingMethod, cleanVoucher,
                    orderItems, subtotal, shippingFee, effectiveDiscount, total, now
            ));

            DocumentReference paymentRef = paymentRepository.newPaymentDocumentRef(db);
            transaction.set(paymentRef, PaymentRepository.buildPaymentData(
                    paymentRef.getId(), orderId, uid, paymentMethod, total, orderCode, now
            ));

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
            transaction.set(notifRef, NotificationRepository.buildOrderNotificationData(
                    notifRef.getId(),
                    uid,
                    "Đặt hàng #" + orderCode + " thành công",
                    "Đơn hàng đang chờ xác nhận (" + paymentMethod.getLabel() + ").",
                    orderCode,
                    now
            ));

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

    @SuppressWarnings("unchecked")
    private void runCancelOrderTransaction(
            @NonNull FirebaseFirestore db,
            @NonNull String uid,
            @NonNull String orderId,
            @Nullable DocumentReference paymentRef,
            @NonNull ResourceCallback<Void> callback
    ) {
        DocumentReference orderRef = db.collection(FirebaseProviderHelper.COL_ORDERS).document(orderId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // B1. ĐỌC toàn bộ trước khi ghi
            DocumentSnapshot orderSnap = transaction.get(orderRef);
            if (!orderSnap.exists()) {
                throw abort("Không tìm thấy đơn hàng");
            }
            String statusStr = FirestoreReadUtil.readString(orderSnap, OrderMapper.FIELD_STATUS, "PENDING");
            if (!OrderService.canCancelStatus(statusStr)) {
                throw abort("Chỉ có thể hủy đơn đang chờ xác nhận");
            }
            String orderCode = FirestoreReadUtil.readString(orderSnap, OrderMapper.FIELD_ORDER_CODE, orderId);
            String voucherCode = FirestoreReadUtil.readString(orderSnap, OrderMapper.FIELD_VOUCHER_CODE, "")
                    .trim().toUpperCase(Locale.ROOT);

            Map<String, DocumentReference> variantRefs = new LinkedHashMap<>();
            Map<String, Integer> restoreQtyByKey = new LinkedHashMap<>();
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
                            variantRefs.put(key, variantDocRef(db, productId, variantId));
                            restoreQtyByKey.put(key, restoreQtyByKey.getOrDefault(key, 0) + qty);
                        }
                    }
                }
            }

            Map<String, Integer> currentStockByKey = new LinkedHashMap<>();
            for (Map.Entry<String, DocumentReference> entry : variantRefs.entrySet()) {
                DocumentSnapshot vSnap = transaction.get(entry.getValue());
                if (vSnap.exists()) {
                    currentStockByKey.put(entry.getKey(), FirestoreReadUtil.readInt(vSnap, ProductMapper.FIELD_STOCK_QTY, 0));
                }
            }

            DocumentSnapshot paymentSnap = paymentRef != null ? transaction.get(paymentRef) : null;
            DocumentReference voucherRef = !voucherCode.isEmpty()
                    ? db.collection(FirebaseProviderHelper.COL_VOUCHERS).document(voucherCode)
                    : null;
            DocumentSnapshot voucherSnap = voucherRef != null ? transaction.get(voucherRef) : null;

            // B2. GHI đồng bộ (hủy đơn, hoàn kho, hoàn lượt voucher, cập nhật thanh toán, tạo thông báo)
            Timestamp now = Timestamp.now();
            transaction.update(
                    orderRef,
                    OrderMapper.FIELD_STATUS, OrderModel.Status.CANCELLED.name(),
                    OrderMapper.FIELD_UPDATED_AT, now
            );

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

            DocumentReference notifRef = db.collection(FirebaseProviderHelper.COL_USERS)
                    .document(uid)
                    .collection(FirebaseProviderHelper.SUB_NOTIFICATIONS)
                    .document();
            transaction.set(notifRef, NotificationRepository.buildOrderNotificationData(
                    notifRef.getId(),
                    uid,
                    "Đơn #" + orderCode + " đã được hủy",
                    "Số lượng sản phẩm đã được hoàn lại vào kho của cửa hàng.",
                    orderCode,
                    now
            ));

            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess(null))
          .addOnFailureListener(e -> callback.onError(
                  e.getMessage() != null ? e.getMessage() : "Không thể hủy đơn hàng"
          ));
    }

    private void commitReorderBatch(
            @NonNull FirebaseFirestore db,
            @NonNull String uid,
            @NonNull List<OrderItemModel> activeItems,
            @NonNull List<Task<QuerySnapshot>> variantTasks,
            @NonNull ResourceCallback<Integer> callback
    ) {
        CollectionReference cartRef = db.collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_CART);

        List<CartItemModel> existingCart = CartRepository.getInstance().getCurrentCartItems();
        Map<String, Integer> pendingQtyByCartDoc = new HashMap<>();
        WriteBatch batch = db.batch();
        Timestamp now = Timestamp.now();
        int addedCount = 0;
        boolean hitStockLimit = false;

        for (int i = 0; i < activeItems.size(); i++) {
            OrderItemModel item = activeItems.get(i);
            Task<QuerySnapshot> task = variantTasks.get(i);
            if (!task.isSuccessful() || task.getResult() == null) {
                continue;
            }
            ProductVariantModel matched = null;
            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                ProductVariantModel v = ProductMapper.variantFromSnapshot(doc, item.getProductId());
                if ((!item.getVariantId().isEmpty() && item.getVariantId().equals(v.getVariantId()))
                        || (v.getSize().equalsIgnoreCase(item.getSize()) && v.getColorName().equalsIgnoreCase(item.getColorName()))) {
                    matched = v;
                    break;
                }
            }
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

    private void ensureItemVariantIds(@NonNull List<CartItemModel> items, @NonNull Runnable onReady) {
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
                                loaded.add(ProductMapper.variantFromSnapshot(doc, productId));
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

    @NonNull
    private static DocumentReference variantDocRef(
            @NonNull FirebaseFirestore db,
            @NonNull String productId,
            @NonNull String variantId
    ) {
        return db.collection(FirebaseProviderHelper.COL_PRODUCTS)
                .document(productId)
                .collection(FirebaseProviderHelper.SUB_VARIANTS)
                .document(variantId);
    }

    @NonNull
    private static FirebaseFirestoreException abort(@NonNull String message) {
        return new FirebaseFirestoreException(message, FirebaseFirestoreException.Code.ABORTED);
    }
}
