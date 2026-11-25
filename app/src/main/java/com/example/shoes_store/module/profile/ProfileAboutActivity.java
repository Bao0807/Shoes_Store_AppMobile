package com.example.shoes_store.module.profile;

import android.os.Bundle;

import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseActivity;

/**
 * Màn hình Giới thiệu cửa hàng, chi nhánh và thông tin liên hệ.
 */
public class ProfileAboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContent(R.layout.profile_about_activity, R.id.about_root);
        bindBackButton(R.id.about_back_btn);
    }
}
