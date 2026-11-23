package com.example.shoes_store.module.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.shoes_store.R;
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

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(existing == null ? "Tạo mã Voucher mới" : "Cập nhật Voucher " + existing.getCode())
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu Voucher", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(v -> {
                String code = textOf(codeInput);
                String badge = textOf(badgeInput);
                String title = textOf(titleInput);
                String desc = textOf(descInput);
                long discount = parseLong(textOf(discountInput), 0L);
                long minOrder = parseLong(textOf(minOrderInput), 0L);
                int limit = (int) parseLong(textOf(limitInput), 50L);
                int validDays = (int) parseLong(textOf(daysInput), 30L);
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
                                Toast.makeText(context, "Đã lưu mã ưu đãi " + code.toUpperCase(), Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }

                            @Override
                            public void onError(String message) {
                                saveBtn.setEnabled(true);
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            });
        });

        dialog.show();
    }

    @NonNull
    private static String textOf(@NonNull EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private static long parseLong(@NonNull String raw, long fallback) {
        if (raw.isEmpty()) {
            return fallback;
        }
        try {
            return Long.parseLong(raw.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
