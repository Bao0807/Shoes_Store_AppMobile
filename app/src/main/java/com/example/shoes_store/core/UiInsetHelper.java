package com.example.shoes_store.core;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Helper xử lý WindowInsets cho giao diện edge-to-edge.
 */
public final class UiInsetHelper {

    private UiInsetHelper() {
    }

    public static void applyTopInset(@NonNull View targetView) {
        final int baseLeft = targetView.getPaddingLeft();
        final int baseTop = targetView.getPaddingTop();
        final int baseRight = targetView.getPaddingRight();
        final int baseBottom = targetView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(targetView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    baseLeft + systemBars.left,
                    baseTop + systemBars.top,
                    baseRight + systemBars.right,
                    baseBottom
            );
            return insets;
        });
    }

    public static void applyBottomInset(@NonNull View targetView) {
        final int baseLeft = targetView.getPaddingLeft();
        final int baseTop = targetView.getPaddingTop();
        final int baseRight = targetView.getPaddingRight();
        final int baseBottom = targetView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(targetView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    baseLeft + systemBars.left,
                    baseTop,
                    baseRight + systemBars.right,
                    baseBottom + systemBars.bottom
            );
            return insets;
        });
    }

    public static void applySystemBarInsets(@NonNull View targetView) {
        final int baseLeft = targetView.getPaddingLeft();
        final int baseTop = targetView.getPaddingTop();
        final int baseRight = targetView.getPaddingRight();
        final int baseBottom = targetView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(targetView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    baseLeft + systemBars.left,
                    baseTop + systemBars.top,
                    baseRight + systemBars.right,
                    baseBottom + systemBars.bottom
            );
            return insets;
        });
    }
}
