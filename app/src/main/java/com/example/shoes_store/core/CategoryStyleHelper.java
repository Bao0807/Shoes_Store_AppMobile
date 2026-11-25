package com.example.shoes_store.core;

import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.shoes_store.R;

/**
 * Áp dụng màu nền và màu chữ đồng bộ cho tag nhu cầu chạy bộ (Daily, Race, Tempo, Trail, Recovery).
 */
public final class CategoryStyleHelper {

    public static final String CAT_ALL = "Tất cả";
    public static final String CAT_DAILY = "Daily";
    public static final String CAT_RACE = "Race";
    public static final String CAT_TEMPO = "Tempo";
    public static final String CAT_TRAIL = "Trail";
    public static final String CAT_RECOVERY = "Recovery";

    private CategoryStyleHelper() {
    }

    public static void applyCategoryBadge(@NonNull TextView badgeView, @NonNull String category) {
        int bgColorRes = R.color.tag_daily_bg;
        int textColorRes = R.color.tag_daily_text;

        if (CAT_RACE.equalsIgnoreCase(category)) {
            bgColorRes = R.color.tag_race_bg;
            textColorRes = R.color.tag_race_text;
        } else if (CAT_TEMPO.equalsIgnoreCase(category)) {
            bgColorRes = R.color.tag_tempo_bg;
            textColorRes = R.color.tag_tempo_text;
        } else if (CAT_TRAIL.equalsIgnoreCase(category)) {
            bgColorRes = R.color.tag_trail_bg;
            textColorRes = R.color.tag_trail_text;
        } else if (CAT_RECOVERY.equalsIgnoreCase(category)) {
            bgColorRes = R.color.tag_recovery_bg;
            textColorRes = R.color.tag_recovery_text;
        }

        StatusBadgeHelper.applyBadge(badgeView, category.toUpperCase(), bgColorRes, textColorRes);
    }
}
