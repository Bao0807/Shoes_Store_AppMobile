package com.example.shoes_store.module.profile;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.shoes_store.R;
import com.example.shoes_store.core.UiInsetHelper;

/**
 * Màn hình Điều khoản sử dụng và các chính sách cửa hàng.
 */
public class ProfileTermsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.profile_terms_activity);
        UiInsetHelper.applySystemBarInsets(findViewById(R.id.terms_root));

        findViewById(R.id.terms_back_btn).setOnClickListener(v -> finish());
    }
}
