package com.example.shoes_store.module.order;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.core.DateFormatUtil;
import com.example.shoes_store.module.payment.PaymentModel;

import java.util.ArrayList;
import java.util.List;

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
    private final Status status;
    private final List<OrderItemModel> orderItems;
    private final String addressSummary;
    private final String receiverInfo;
    private final String shippingMethod;
    private final String paymentMethod;
    private final String voucherCode;
    private final long subtotal;
    private final long shippingFee;
    private final long discount;
    private final long total;
    private final long createdAtMillis;

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
            long createdAtMillis
    ) {
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.userId = userId;
        this.status = status;
        this.paymentMethod = paymentMethodEnum.getLabel();
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
        this.createdAt = createdAtMillis > 0L
                ? DateFormatUtil.formatDateTime(createdAtMillis)
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

    public boolean canCancel() {
        return status == Status.PENDING;
    }

    public int getTotalQuantity() {
        int count = 0;
        for (OrderItemModel item : orderItems) {
            count += item.getQuantity();
        }
        return count;
    }
}
