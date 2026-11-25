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

    private final String orderId;
    private final Status status;
    private final String transactionCode;
    private final long createdAtMillis;

    public PaymentModel(
            @NonNull String orderId,
            @NonNull Status status,
            @Nullable String transactionCode,
            long createdAtMillis
    ) {
        this.orderId = orderId;
        this.status = status;
        this.transactionCode = transactionCode != null ? transactionCode : "";
        this.createdAtMillis = createdAtMillis;
    }

    @NonNull
    public String getOrderId() {
        return orderId;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @NonNull
    public String getTransactionCode() {
        return transactionCode;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }
}
