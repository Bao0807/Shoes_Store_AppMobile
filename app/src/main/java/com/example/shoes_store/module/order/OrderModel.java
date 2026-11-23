package com.example.shoes_store.module.order;

import com.example.shoes_store.module.cart.CartItemModel;

import java.util.List;

/**
 * Model đơn hàng với trạng thái và chi tiết thanh toán.
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
    }

    private final String orderCode;
    private final String createdAt;
    private Status status;
    private final List<CartItemModel> items;
    private final String addressSummary;
    private final String receiverInfo;
    private final String shippingMethod;
    private final String paymentMethod;
    private final long subtotal;
    private final long shippingFee;
    private final long discount;
    private final long total;

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
        this.orderCode = orderCode;
        this.createdAt = createdAt;
        this.status = status;
        this.items = items;
        this.addressSummary = addressSummary;
        this.receiverInfo = receiverInfo;
        this.shippingMethod = shippingMethod;
        this.paymentMethod = paymentMethod;
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.discount = discount;
        this.total = total;
    }

    public String getOrderCode() {
        return orderCode;
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

    public int getTotalQuantity() {
        int count = 0;
        for (CartItemModel item : items) {
            count += item.getQuantity();
        }
        return count;
    }
}
