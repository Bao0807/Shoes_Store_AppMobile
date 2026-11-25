package com.example.shoes_store.core;

import android.view.View;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

/**
 * Lớp cơ sở cho toàn bộ Activity: cấu hình edge-to-edge, nút quay lại và hiển thị Toast an toàn vòng đời.
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected void setEdgeToEdgeContent(@LayoutRes int layoutRes, @IdRes int insetTargetId) {
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(layoutRes);
        View insetTarget = findViewById(insetTargetId);
        if (insetTarget != null) {
            UiInsetHelper.applySystemBarInsets(insetTarget);
        }
    }

    protected void setEdgeToEdgeContent(
            @LayoutRes int layoutRes,
            @IdRes int topBarId,
            @IdRes int bottomBarId
    ) {
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(layoutRes);
        View topBar = findViewById(topBarId);
        if (topBar != null) {
            UiInsetHelper.applyTopInset(topBar);
        }
        View bottomBar = findViewById(bottomBarId);
        if (bottomBar != null) {
            UiInsetHelper.applyBottomInset(bottomBar);
        }
    }

    protected void bindBackButton(@IdRes int backBtnId) {
        View backBtn = findViewById(backBtnId);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }
    }

    protected boolean isAlive() {
        return !isFinishing() && !isDestroyed();
    }

    protected void showToast(@NonNull CharSequence message) {
        if (isAlive()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    protected void showToast(@StringRes int messageRes) {
        if (isAlive()) {
            Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
        }
    }

    protected void showLongToast(@NonNull CharSequence message) {
        if (isAlive()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }
}
