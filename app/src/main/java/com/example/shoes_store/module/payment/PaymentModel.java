package com.example.shoes_store.module.payment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Giao dịch thanh toán tách riêng gắn với một đơn hàng.
 */
public class PaymentModel {

    public enum Method {
        COD("Thanh toán khi nhận hàng (COD)"),
        BANK_QR("Chuyển khoản QR"),
        E_WALLET("Ví điện tử");

        private final String label;

        Method(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @NonNull
        public static Method fromStorage(@Nullable String value) {
            if (value == null) {
                return COD;
            }
            for (Method m : values()) {
                if (m.name().equalsIgnoreCase(value) || m.label.equalsIgnoreCase(value)) {
                    return m;
                }
            }
            return COD;
        }
    }

    public enum Status {
        UNPAID("Chưa thanh toán"),
        PAID("Đã thanh toán"),
        FAILED("Thanh toán thất bại"),
        REFUNDED("Đã hoàn tiền");

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
                return UNPAID;
            }
            for (Status s : values()) {
                if (s.name().equalsIgnoreCase(value)) {
                    return s;
                }
            }
            return UNPAID;
        }
    }

    private final String paymentId;
    private final String orderId;
    private final String userId;
    private final Method method;
    private final Status status;
    private final long amount;
    private final String transactionCode;
    private final long paidAtMillis;
    private final long createdAtMillis;

    public PaymentModel(
            @NonNull String paymentId,
            @NonNull String orderId,
            @NonNull String userId,
            @NonNull Method method,
            @NonNull Status status,
            long amount,
            @Nullable String transactionCode,
            long paidAtMillis,
            long createdAtMillis
    ) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.userId = userId;
        this.method = method;
        this.status = status;
        this.amount = amount;
        this.transactionCode = transactionCode != null ? transactionCode : "";
        this.paidAtMillis = paidAtMillis;
        this.createdAtMillis = createdAtMillis;
    }

    @NonNull
    public String getPaymentId() {
        return paymentId;
    }

    @NonNull
    public String getOrderId() {
        return orderId;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    @NonNull
    public Method getMethod() {
        return method;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    public long getAmount() {
        return amount;
    }

    @NonNull
    public String getTransactionCode() {
        return transactionCode;
    }

    public long getPaidAtMillis() {
        return paidAtMillis;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public boolean isPaid() {
        return status == Status.PAID;
    }
}
