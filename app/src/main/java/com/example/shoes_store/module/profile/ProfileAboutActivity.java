package com.example.shoes_store.module.profile;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.shoes_store.R;
import com.example.shoes_store.core.UiInsetHelper;

/**
 * Màn hình Giới thiệu cửa hàng, chi nhánh và thông tin liên hệ.
 */
public class ProfileAboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.profile_about_activity);
        UiInsetHelper.applySystemBarInsets(findViewById(R.id.about_root));

        findViewById(R.id.about_back_btn).setOnClickListener(v -> finish());
    }
}
