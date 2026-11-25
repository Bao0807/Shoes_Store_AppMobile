package com.example.shoes_store.module.order;

import androidx.annotation.NonNull;

import com.example.shoes_store.R;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.profile.ProfileAddressModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chuyển đổi dữ liệu Firestore cho Đơn hàng (`orders`) và từng dòng sản phẩm (`OrderItemModel`).
 */
public final class OrderMapper {

    public static final String FIELD_ORDER_ID = "orderId";
    public static final String FIELD_ORDER_CODE = "orderCode";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_PAYMENT_METHOD = "paymentMethod";
    public static final String FIELD_RECEIVER_NAME = "receiverName";
    public static final String FIELD_RECEIVER_PHONE = "receiverPhone";
    public static final String FIELD_SHIPPING_ADDRESS = "shippingAddress";
    public static final String FIELD_SHIPPING_METHOD = "shippingMethod";
    public static final String FIELD_VOUCHER_CODE = "voucherCode";
    public static final String FIELD_ITEMS = "items";
    public static final String FIELD_SUBTOTAL = "subtotal";
    public static final String FIELD_SHIPPING_FEE = "shippingFee";
    public static final String FIELD_DISCOUNT = "discount";
    public static final String FIELD_TOTAL = "total";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";

    public static final String ITEM_PRODUCT_ID = "productId";
    public static final String ITEM_VARIANT_ID = "variantId";
    public static final String ITEM_PRODUCT_NAME = "productName";
    public static final String ITEM_THUMBNAIL_URL = "thumbnailUrl";
    public static final String ITEM_SIZE = "size";
    public static final String ITEM_COLOR_NAME = "colorName";
    public static final String ITEM_UNIT_PRICE = "unitPrice";
    public static final String ITEM_QUANTITY = "quantity";
    public static final String ITEM_LINE_TOTAL = "lineTotal";

    private OrderMapper() {
    }

    @NonNull
    public static List<OrderItemModel> parseItemsFromSnapshot(@NonNull DocumentSnapshot doc) {
        List<OrderItemModel> orderItems = new ArrayList<>();
        Object rawItems = doc.get(FIELD_ITEMS);
        if (rawItems instanceof List<?>) {
            for (Object entry : (List<?>) rawItems) {
                if (entry instanceof Map<?, ?>) {
                    Map<?, ?> map = (Map<?, ?>) entry;
                    orderItems.add(new OrderItemModel(
                            FirestoreReadUtil.mapString(map, ITEM_PRODUCT_ID, ""),
                            FirestoreReadUtil.mapString(map, ITEM_VARIANT_ID, ""),
                            FirestoreReadUtil.mapString(map, ITEM_PRODUCT_NAME, ""),
                            FirestoreReadUtil.mapString(map, ITEM_THUMBNAIL_URL, ""),
                            R.drawable.img_shoe_asics_novablast,
                            FirestoreReadUtil.mapString(map, ITEM_SIZE, "42"),
                            FirestoreReadUtil.mapString(map, ITEM_COLOR_NAME, "Mặc định"),
                            FirestoreReadUtil.mapLong(map, ITEM_UNIT_PRICE, 0L),
                            FirestoreReadUtil.mapInt(map, ITEM_QUANTITY, 1),
                            FirestoreReadUtil.mapLong(map, ITEM_LINE_TOTAL, 0L)
                    ));
                }
            }
        }
        return orderItems;
    }

    public static boolean snapshotContainsProduct(@NonNull DocumentSnapshot doc, @NonNull String productId) {
        for (OrderItemModel item : parseItemsFromSnapshot(doc)) {
            if (productId.equals(item.getProductId())) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public static OrderModel fromSnapshot(@NonNull DocumentSnapshot doc) {
        List<OrderItemModel> orderItems = parseItemsFromSnapshot(doc);
        String receiverName = FirestoreReadUtil.readString(doc, FIELD_RECEIVER_NAME, "");
        String receiverPhone = FirestoreReadUtil.readString(doc, FIELD_RECEIVER_PHONE, "");
        String receiverInfo = receiverPhone.isEmpty() ? receiverName : receiverName + " · " + receiverPhone;

        return new OrderModel(
                FirestoreReadUtil.readString(doc, FIELD_ORDER_ID, doc.getId()),
                FirestoreReadUtil.readString(doc, FIELD_ORDER_CODE, doc.getId()),
                FirestoreReadUtil.readString(doc, FIELD_USER_ID, ""),
                OrderModel.Status.fromStorage(FirestoreReadUtil.readString(doc, FIELD_STATUS, OrderModel.Status.PENDING.name())),
                PaymentModel.Method.fromStorage(FirestoreReadUtil.readString(doc, FIELD_PAYMENT_METHOD, PaymentModel.Method.COD.name())),
                orderItems,
                FirestoreReadUtil.readString(doc, FIELD_SHIPPING_ADDRESS, ""),
                receiverInfo,
                FirestoreReadUtil.readString(doc, FIELD_SHIPPING_METHOD, OrderService.SHIPPING_METHOD_STANDARD),
                FirestoreReadUtil.readString(doc, FIELD_VOUCHER_CODE, ""),
                FirestoreReadUtil.readLong(doc, FIELD_SUBTOTAL, 0L),
                FirestoreReadUtil.readLong(doc, FIELD_SHIPPING_FEE, 0L),
                FirestoreReadUtil.readLong(doc, FIELD_DISCOUNT, 0L),
                FirestoreReadUtil.readLong(doc, FIELD_TOTAL, 0L),
                FirestoreReadUtil.readMillis(doc, FIELD_CREATED_AT)
        );
    }

    @NonNull
    public static List<Map<String, Object>> orderItemsToMaps(@NonNull List<OrderItemModel> orderItems) {
        List<Map<String, Object>> itemMaps = new ArrayList<>();
        for (OrderItemModel oi : orderItems) {
            Map<String, Object> m = new HashMap<>();
            m.put(ITEM_PRODUCT_ID, oi.getProductId());
            m.put(ITEM_VARIANT_ID, oi.getVariantId());
            m.put(ITEM_PRODUCT_NAME, oi.getProductName());
            m.put(ITEM_THUMBNAIL_URL, oi.getThumbnailUrl());
            m.put(ITEM_SIZE, oi.getSize());
            m.put(ITEM_COLOR_NAME, oi.getColorName());
            m.put(ITEM_UNIT_PRICE, oi.getUnitPrice());
            m.put(ITEM_QUANTITY, oi.getQuantity());
            m.put(ITEM_LINE_TOTAL, oi.getLineTotal());
            itemMaps.add(m);
        }
        return itemMaps;
    }

    @NonNull
    public static Map<String, Object> orderToMap(
            @NonNull String orderId,
            @NonNull String orderCode,
            @NonNull String uid,
            @NonNull PaymentModel.Method paymentMethod,
            @NonNull ProfileAddressModel address,
            @NonNull String shippingMethod,
            @NonNull String voucherCode,
            @NonNull List<OrderItemModel> orderItems,
            long subtotal,
            long shippingFee,
            long discount,
            long total,
            @NonNull Timestamp now
    ) {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put(FIELD_ORDER_ID, orderId);
        orderData.put(FIELD_ORDER_CODE, orderCode);
        orderData.put(FIELD_USER_ID, uid);
        orderData.put(FIELD_STATUS, OrderModel.Status.PENDING.name());
        orderData.put(FIELD_PAYMENT_METHOD, paymentMethod.name());
        orderData.put(FIELD_RECEIVER_NAME, address.getReceiverName());
        orderData.put(FIELD_RECEIVER_PHONE, address.getPhone());
        orderData.put(FIELD_SHIPPING_ADDRESS, address.getAddressDetail());
        orderData.put(FIELD_SHIPPING_METHOD, shippingMethod);
        orderData.put(FIELD_VOUCHER_CODE, voucherCode);
        orderData.put(FIELD_ITEMS, orderItemsToMaps(orderItems));
        orderData.put(FIELD_SUBTOTAL, subtotal);
        orderData.put(FIELD_SHIPPING_FEE, shippingFee);
        orderData.put(FIELD_DISCOUNT, discount);
        orderData.put(FIELD_TOTAL, total);
        orderData.put(FIELD_CREATED_AT, now);
        orderData.put(FIELD_UPDATED_AT, now);
        return orderData;
    }
}
