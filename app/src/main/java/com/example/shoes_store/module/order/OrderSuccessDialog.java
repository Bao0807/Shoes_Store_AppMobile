package com.example.shoes_store.module.order;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.shoes_store.R;

/**
 * Dialog thông báo đặt hàng thành công.
 */
public final class OrderSuccessDialog {

    public interface OnDialogActionListener {
        void onViewOrders();

        void onGoHome();
    }

    private OrderSuccessDialog() {
    }

    public static void show(Context context, String orderCode, OnDialogActionListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.order_dialog_success, null);
        dialog.setContentView(view);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }

        TextView codeText = view.findViewById(R.id.dialog_success_code);
        codeText.setText("Mã đơn #" + orderCode + " đã được ghi nhận.\nBạn có thể theo dõi trạng thái giao hàng ngay.");

        view.findViewById(R.id.dialog_view_order_btn).setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onViewOrders();
            }
        });

        view.findViewById(R.id.dialog_go_home_btn).setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onGoHome();
            }
        });

        dialog.show();
    }
}
