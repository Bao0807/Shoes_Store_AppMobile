package com.example.shoes_store.module.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.R;
import com.example.shoes_store.core.DialogHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.voucher.VoucherModel;

/**
 * Hộp thoại Thêm mới / Cập nhật mã giảm giá (Voucher) trong Kênh Quản trị (Admin).
 */
public final class AdminVoucherDialog {

    private AdminVoucherDialog() {
    }

    public static void show(
            @NonNull Context context,
            @NonNull AdminViewModel viewModel,
            @Nullable VoucherModel existing
    ) {
        View view = LayoutInflater.from(context).inflate(R.layout.admin_dialog_voucher, null);
        EditText codeInput = view.findViewById(R.id.dialog_voucher_code);
        EditText badgeInput = view.findViewById(R.id.dialog_voucher_badge);
        EditText titleInput = view.findViewById(R.id.dialog_voucher_title);
        EditText descInput = view.findViewById(R.id.dialog_voucher_desc);
        EditText discountInput = view.findViewById(R.id.dialog_voucher_discount);
        EditText minOrderInput = view.findViewById(R.id.dialog_voucher_min_order);
        EditText limitInput = view.findViewById(R.id.dialog_voucher_limit);
        EditText daysInput = view.findViewById(R.id.dialog_voucher_days);

        if (existing != null) {
            codeInput.setText(existing.getCode());
            codeInput.setEnabled(false);
            badgeInput.setText(existing.getShortBadge());
            titleInput.setText(existing.getTitle());
            descInput.setText(existing.getDescription());
            discountInput.setText(String.valueOf(existing.getDiscountAmount()));
            minOrderInput.setText(String.valueOf(existing.getMinOrderAmount()));
            limitInput.setText(String.valueOf(existing.getUsageLimit()));
            long diffMs = existing.getExpiryTimestampMillis() - System.currentTimeMillis();
            int remainingDays = diffMs > 0L ? (int) Math.max(1L, diffMs / 86_400_000L) : 30;
            daysInput.setText(String.valueOf(remainingDays));
        } else {
            limitInput.setText("50");
            daysInput.setText("30");
        }

        DialogHelper.showFormDialog(
                context,
                existing == null ? "Tạo mã Voucher mới" : "Cập nhật Voucher " + existing.getCode(),
                view,
                "Hủy",
                "Lưu Voucher",
                (dialog, saveBtn) -> {
                    String code = DialogHelper.textOf(codeInput);
                    String badge = DialogHelper.textOf(badgeInput);
                    String title = DialogHelper.textOf(titleInput);
                    String desc = DialogHelper.textOf(descInput);
                    long discount = DialogHelper.parseLong(DialogHelper.textOf(discountInput), 0L);
                    long minOrder = DialogHelper.parseLong(DialogHelper.textOf(minOrderInput), 0L);
                    int limit = (int) DialogHelper.parseLong(DialogHelper.textOf(limitInput), 50L);
                    int validDays = (int) DialogHelper.parseLong(DialogHelper.textOf(daysInput), 30L);
                    boolean active = existing == null || existing.isActive();

                    saveBtn.setEnabled(false);
                    viewModel.saveVoucher(
                            code,
                            badge,
                            title,
                            desc,
                            discount,
                            minOrder,
                            limit,
                            validDays,
                            active,
                            new ResourceCallback<Void>() {
                                @Override
                                public void onSuccess(Void data) {
                                    if (!DialogHelper.isHostAlive(context)) {
                                        return;
                                    }
                                    Toast.makeText(context, "Đã lưu mã ưu đãi " + code.toUpperCase(), Toast.LENGTH_SHORT).show();
                                    if (dialog.isShowing()) {
                                        dialog.dismiss();
                                    }
                                }

                                @Override
                                public void onError(String message) {
                                    if (!DialogHelper.isHostAlive(context)) {
                                        return;
                                    }
                                    saveBtn.setEnabled(true);
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                }
        );
    }
}
