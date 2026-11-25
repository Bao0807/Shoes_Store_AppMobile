package com.example.shoes_store.module.payment;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.shoes_store.R;
import com.example.shoes_store.core.StatusBadgeHelper;

/**
 * Áp dụng màu sắc và nhãn hiển thị đồng bộ cho badge trạng thái thanh toán.
 */
public final class PaymentStatusHelper {

    private PaymentStatusHelper() {
    }

    public static void applyPaymentStatusBadge(@NonNull TextView badge, @NonNull PaymentModel.Status status) {
        badge.setVisibility(View.VISIBLE);
        int bgRes;
        int textRes;
        switch (status) {
            case PAID:
                bgRes = R.color.status_success_soft;
                textRes = R.color.status_success;
                break;
            case REFUNDED:
                bgRes = R.color.status_info_soft;
                textRes = R.color.status_info;
                break;
            case FAILED:
                bgRes = R.color.status_danger_soft;
                textRes = R.color.status_danger;
                break;
            case UNPAID:
            default:
                bgRes = R.color.status_warning_soft;
                textRes = R.color.status_warning;
                break;
        }
        StatusBadgeHelper.applyBadge(badge, status.getLabel(), bgRes, textRes);
    }
}
