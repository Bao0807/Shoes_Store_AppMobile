package com.example.shoes_store.module.order;

import android.app.Activity;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.MainActivity;
import com.example.shoes_store.R;
import com.example.shoes_store.core.DialogHelper;
import com.example.shoes_store.core.ResourceCallback;

/**
 * Điều phối thao tác Hủy đơn và Mua lại dùng chung cho Chi tiết đơn hàng và Lịch sử đơn hàng (qua OrderViewModel).
 */
public final class OrderActionHelper {

    private OrderActionHelper() {
    }

    public static void confirmCancelOrder(
            @NonNull Activity activity,
            @NonNull OrderViewModel viewModel,
            @NonNull OrderModel order,
            @Nullable View triggerView
    ) {
        DialogHelper.showConfirmDialog(
                activity,
                "Hủy đơn #" + order.getOrderCode(),
                "Bạn muốn hủy đơn hàng này và hoàn lại số lượng về kho?",
                "Không",
                "Hủy đơn",
                () -> {
                    if (triggerView != null) {
                        triggerView.setEnabled(false);
                    }
                    viewModel.cancelOrder(order.getOrderId(), new ResourceCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            if (!DialogHelper.isHostAlive(activity)) {
                                return;
                            }
                            Toast.makeText(
                                    activity,
                                    "Đã hủy đơn #" + order.getOrderCode(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        @Override
                        public void onError(String message) {
                            if (!DialogHelper.isHostAlive(activity)) {
                                return;
                            }
                            if (triggerView != null) {
                                triggerView.setEnabled(true);
                            }
                            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                        }
                    });
                }
        );
    }

    public static void executeReorder(
            @NonNull Activity activity,
            @NonNull OrderViewModel viewModel,
            @NonNull OrderModel order,
            @Nullable View triggerView
    ) {
        if (triggerView != null) {
            triggerView.setEnabled(false);
        }
        viewModel.reorder(order, new ResourceCallback<Integer>() {
            @Override
            public void onSuccess(Integer added) {
                if (!DialogHelper.isHostAlive(activity)) {
                    return;
                }
                Toast.makeText(
                        activity,
                        "Đã thêm lại " + added + " mẫu vào Giỏ hàng",
                        Toast.LENGTH_SHORT
                ).show();
                MainActivity.openTab(activity, R.id.nav_cart);
                activity.finish();
            }

            @Override
            public void onError(String message) {
                if (!DialogHelper.isHostAlive(activity)) {
                    return;
                }
                if (triggerView != null) {
                    triggerView.setEnabled(true);
                }
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
