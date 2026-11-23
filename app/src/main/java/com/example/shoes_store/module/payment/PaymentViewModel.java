package com.example.shoes_store.module.payment;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.core.ResourceCallback;

/**
 * ViewModel quản lý truy vấn thông tin giao dịch thanh toán của đơn hàng.
 */
public class PaymentViewModel extends ViewModel {

    private final PaymentRepository repository = new PaymentRepository();

    public void loadLatestPayment(@NonNull String orderId, @NonNull ResourceCallback<PaymentModel> callback) {
        repository.loadLatestPayment(orderId, callback);
    }
}
