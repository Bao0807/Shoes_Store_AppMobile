package com.example.shoes_store.core;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * Tiện ích tô màu badge trạng thái/danh mục dùng chung toàn dự án (thuần UI).
 */
public final class StatusBadgeHelper {

    private StatusBadgeHelper() {
    }

    public static void applyBadge(
            @NonNull TextView badgeView,
            @NonNull CharSequence text,
            @ColorRes int bgColorRes,
            @ColorRes int textColorRes
    ) {
        badgeView.setText(text);
        applyColors(badgeView, bgColorRes, textColorRes);
    }

    public static void applyColors(
            @NonNull TextView badgeView,
            @ColorRes int bgColorRes,
            @ColorRes int textColorRes
    ) {
        badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), textColorRes));
        applyBackgroundTint(badgeView, bgColorRes);
    }

    public static void applyBackgroundTint(@NonNull View view, @ColorRes int bgColorRes) {
        if (view.getBackground() != null) {
            view.getBackground().mutate().setTint(ContextCompat.getColor(view.getContext(), bgColorRes));
        }
    }
}
