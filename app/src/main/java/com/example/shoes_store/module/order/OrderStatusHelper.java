package com.example.shoes_store.module.order;

import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.shoes_store.R;
import com.example.shoes_store.core.StatusBadgeHelper;

/**
 * Áp dụng màu sắc và nhãn hiển thị đồng bộ cho badge trạng thái đơn hàng.
 */
public final class OrderStatusHelper {

    private OrderStatusHelper() {
    }

    public static void applyStatusBadge(@NonNull TextView badge, @NonNull OrderModel.Status status) {
        int bgRes;
        int textRes;
        switch (status) {
            case PENDING:
                bgRes = R.color.status_warning_soft;
                textRes = R.color.status_warning;
                break;
            case SHIPPING:
                bgRes = R.color.status_info_soft;
                textRes = R.color.status_info;
                break;
            case COMPLETED:
                bgRes = R.color.status_success_soft;
                textRes = R.color.status_success;
                break;
            case CANCELLED:
            default:
                bgRes = R.color.status_danger_soft;
                textRes = R.color.status_danger;
                break;
        }
        StatusBadgeHelper.applyBadge(badge, status.getLabel(), bgRes, textRes);
    }
}
