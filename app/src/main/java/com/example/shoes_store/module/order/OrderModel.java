package com.example.shoes_store.module.order;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.payment.PaymentModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Model đơn hàng với trạng thái, phương thức thanh toán và snapshot chi tiết thanh toán.
 */
public class OrderModel {

    public enum Status {
        PENDING("Chờ xác nhận"),
        SHIPPING("Đang giao"),
        COMPLETED("Hoàn thành"),
        CANCELLED("Đã hủy");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @NonNull
        public static Status fromStorage(@Nullable String value) {
            if (value == null) {
                return PENDING;
            }
            for (Status s : values()) {
                if (s.name().equalsIgnoreCase(value)) {
                    return s;
                }
            }
            return PENDING;
        }
    }

    private final String orderId;
    private final String orderCode;
    private final String userId;
    private final String createdAt;
    private Status status;
    private final List<CartItemModel> items;
    private final List<OrderItemModel> orderItems;
    private final String addressSummary;
    private final String receiverInfo;
    private final String shippingMethod;
    private final String paymentMethod;
    private final PaymentModel.Method paymentMethodEnum;
    private final String voucherCode;
    private final long subtotal;
    private final long shippingFee;
    private final long discount;
    private final long total;
    private final long createdAtMillis;
    private final long updatedAtMillis;

    public OrderModel(
            String orderCode,
            String createdAt,
            Status status,
            List<CartItemModel> items,
            String addressSummary,
            String receiverInfo,
            String shippingMethod,
            String paymentMethod,
            long subtotal,
            long shippingFee,
            long discount,
            long total
    ) {
        this.orderId = orderCode != null ? orderCode : "";
        this.orderCode = orderCode != null ? orderCode : "";
        this.userId = "";
        this.createdAt = createdAt != null ? createdAt : "";
        this.status = status != null ? status : Status.PENDING;
        this.items = items != null ? items : new ArrayList<>();
        this.orderItems = new ArrayList<>();
        for (CartItemModel cartItem : this.items) {
            this.orderItems.add(OrderItemModel.fromCartItem(cartItem));
        }
        this.addressSummary = addressSummary != null ? addressSummary : "";
        this.receiverInfo = receiverInfo != null ? receiverInfo : "";
        this.shippingMethod = shippingMethod != null ? shippingMethod : "";
        this.paymentMethodEnum = PaymentModel.Method.fromStorage(paymentMethod);
        this.paymentMethod = paymentMethod != null ? paymentMethod : paymentMethodEnum.getLabel();
        this.voucherCode = "";
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.discount = discount;
        this.total = total;
        this.createdAtMillis = 0L;
        this.updatedAtMillis = 0L;
    }

    public OrderModel(
            @NonNull String orderId,
            @NonNull String orderCode,
            @NonNull String userId,
            @NonNull Status status,
            @NonNull PaymentModel.Method paymentMethodEnum,
            @NonNull List<OrderItemModel> orderItems,
            @NonNull String addressSummary,
            @NonNull String receiverInfo,
            @NonNull String shippingMethod,
            @Nullable String voucherCode,
            long subtotal,
            long shippingFee,
            long discount,
            long total,
            long createdAtMillis,
            long updatedAtMillis
    ) {
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.userId = userId;
        this.status = status;
        this.paymentMethodEnum = paymentMethodEnum;
        this.paymentMethod = paymentMethodEnum.getLabel();
        this.items = new ArrayList<>();
        this.orderItems = orderItems != null ? new ArrayList<>(orderItems) : new ArrayList<>();
        this.addressSummary = addressSummary;
        this.receiverInfo = receiverInfo;
        this.shippingMethod = shippingMethod;
        this.voucherCode = voucherCode != null ? voucherCode : "";
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.discount = discount;
        this.total = total;
        this.createdAtMillis = createdAtMillis;
        this.updatedAtMillis = updatedAtMillis;
        this.createdAt = createdAtMillis > 0L
                ? new SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(new Date(createdAtMillis))
                : "";
    }

    @NonNull
    public String getOrderId() {
        return orderId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<CartItemModel> getItems() {
        return items;
    }

    @NonNull
    public List<OrderItemModel> getOrderItems() {
        return orderItems;
    }

    public String getAddressSummary() {
        return addressSummary;
    }

    public String getReceiverInfo() {
        return receiverInfo;
    }

    public String getShippingMethod() {
        return shippingMethod;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    @NonNull
    public PaymentModel.Method getPaymentMethodEnum() {
        return paymentMethodEnum;
    }

    @NonNull
    public String getVoucherCode() {
        return voucherCode;
    }

    public long getSubtotal() {
        return subtotal;
    }

    public long getShippingFee() {
        return shippingFee;
    }

    public long getDiscount() {
        return discount;
    }

    public long getTotal() {
        return total;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public boolean canCancel() {
        return status == Status.PENDING;
    }

    public int getTotalQuantity() {
        int count = 0;
        if (!orderItems.isEmpty()) {
            for (OrderItemModel item : orderItems) {
                count += item.getQuantity();
            }
            return count;
        }
        for (CartItemModel item : items) {
            count += item.getQuantity();
        }
        return count;
    }
}
