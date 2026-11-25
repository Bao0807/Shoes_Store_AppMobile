package com.example.shoes_store.core;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.shoes_store.R;
import com.google.android.material.button.MaterialButton;

/**
 * Tiện ích hiển thị AlertDialog xác nhận/biểu mẫu và kiểm tra vòng đời Activity host (thuần UI).
 */
public final class DialogHelper {

    public interface OnFormSubmitListener {
        void onSubmit(@NonNull AlertDialog dialog, @NonNull Button positiveButton);
    }

    private DialogHelper() {
    }

    public static boolean isHostAlive(@Nullable Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                Activity activity = (Activity) current;
                return !activity.isFinishing() && !activity.isDestroyed();
            }
            Context base = ((ContextWrapper) current).getBaseContext();
            if (base == null || base == current) {
                break;
            }
            current = base;
        }
        return current != null;
    }

    @NonNull
    public static AlertDialog showConfirmDialog(
            @NonNull Context context,
            @NonNull CharSequence title,
            @NonNull CharSequence message,
            @NonNull CharSequence negativeText,
            @NonNull CharSequence positiveText,
            @NonNull Runnable onConfirm
    ) {
        return showConfirmDialog(context, title, message, negativeText, positiveText, 0, false, onConfirm);
    }

    @NonNull
    public static AlertDialog showConfirmDialog(
            @NonNull Context context,
            @NonNull CharSequence title,
            @NonNull CharSequence message,
            @NonNull CharSequence negativeText,
            @NonNull CharSequence positiveText,
            @DrawableRes int iconRes,
            boolean destructive,
            @NonNull Runnable onConfirm
    ) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.core_dialog_confirm, new FrameLayout(context), false);
        FrameLayout iconBadge = view.findViewById(R.id.confirm_dialog_icon_badge);
        ImageView iconView = view.findViewById(R.id.confirm_dialog_icon);
        TextView titleView = view.findViewById(R.id.confirm_dialog_title);
        TextView messageView = view.findViewById(R.id.confirm_dialog_message);
        MaterialButton negativeBtn = view.findViewById(R.id.confirm_dialog_negative_btn);
        MaterialButton positiveBtn = view.findViewById(R.id.confirm_dialog_positive_btn);

        if (iconRes != 0) {
            iconBadge.setVisibility(View.VISIBLE);
            iconBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                    context, destructive ? R.color.status_danger_soft : R.color.brand_accent_soft
            )));
            iconView.setImageResource(iconRes);
            iconView.setColorFilter(ContextCompat.getColor(
                    context, destructive ? R.color.status_danger : R.color.brand_accent
            ));
        } else {
            iconBadge.setVisibility(View.GONE);
        }

        titleView.setText(title);
        messageView.setText(message);
        negativeBtn.setText(negativeText);
        positiveBtn.setText(positiveText);
        positiveBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                context, destructive ? R.color.status_danger : R.color.brand_primary
        )));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        negativeBtn.setOnClickListener(v -> dialog.dismiss());
        positiveBtn.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });

        dialog.show();
        return dialog;
    }

    @NonNull
    public static AlertDialog showFormDialog(
            @NonNull Context context,
            @NonNull CharSequence title,
            @NonNull View contentView,
            @NonNull CharSequence negativeText,
            @NonNull CharSequence positiveText,
            @NonNull OnFormSubmitListener onSubmit
    ) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(contentView)
                .setNegativeButton(negativeText, null)
                .setPositiveButton(positiveText, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setOnClickListener(v -> onSubmit.onSubmit(dialog, positiveBtn));
        });

        dialog.show();
        return dialog;
    }

    @NonNull
    public static String textOf(@Nullable EditText editText) {
        return editText != null && editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }

    public static long parseLong(@NonNull String raw, long fallback) {
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
