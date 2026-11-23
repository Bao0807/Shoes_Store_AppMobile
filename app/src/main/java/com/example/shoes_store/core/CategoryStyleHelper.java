package com.example.shoes_store.core;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.shoes_store.R;

/**
 * Áp dụng màu nền và màu chữ đồng bộ cho tag nhu cầu chạy bộ (Daily, Race, Tempo, Trail, Recovery).
 */
public final class CategoryStyleHelper {

    private CategoryStyleHelper() {
    }

    public static void applyCategoryBadge(@NonNull TextView badgeView, @NonNull String category) {
        Context context = badgeView.getContext();
        int bgColorRes = R.color.tag_daily_bg;
        int textColorRes = R.color.tag_daily_text;

        if ("Race".equalsIgnoreCase(category)) {
            bgColorRes = R.color.tag_race_bg;
            textColorRes = R.color.tag_race_text;
        } else if ("Tempo".equalsIgnoreCase(category)) {
            bgColorRes = R.color.tag_tempo_bg;
            textColorRes = R.color.tag_tempo_text;
        } else if ("Trail".equalsIgnoreCase(category)) {
            bgColorRes = R.color.tag_trail_bg;
            textColorRes = R.color.tag_trail_text;
        } else if ("Recovery".equalsIgnoreCase(category)) {
            bgColorRes = R.color.tag_recovery_bg;
            textColorRes = R.color.tag_recovery_text;
        }

        badgeView.setText(category.toUpperCase());
        badgeView.setTextColor(ContextCompat.getColor(context, textColorRes));
        if (badgeView.getBackground() != null) {
            badgeView.getBackground().mutate().setTint(ContextCompat.getColor(context, bgColorRes));
        }
    }
}
