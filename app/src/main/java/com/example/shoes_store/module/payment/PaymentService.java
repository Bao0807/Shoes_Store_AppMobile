package com.example.shoes_store.module.payment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Nghiệp vụ thuần Java cho Thanh toán: trạng thái khởi tạo theo phương thức, trạng thái khi hủy đơn và mã giao dịch.
 */
public final class PaymentService {

    private PaymentService() {
    }

    @NonNull
    public static PaymentModel.Status resolveInitialStatus(@NonNull PaymentModel.Method method) {
        return method == PaymentModel.Method.COD
                ? PaymentModel.Status.UNPAID
                : PaymentModel.Status.PAID;
    }

    @NonNull
    public static PaymentModel.Status resolveStatusOnOrderCancel(@Nullable String currentStatus) {
        return PaymentModel.Status.PAID.name().equalsIgnoreCase(currentStatus)
                ? PaymentModel.Status.REFUNDED
                : PaymentModel.Status.FAILED;
    }

    @NonNull
    public static String buildTransactionCode(@NonNull PaymentModel.Status status, @NonNull String orderCode) {
        return status == PaymentModel.Status.PAID ? "TXN-" + orderCode : "";
    }
}
