package com.example.shoes_store.module.profile;

import android.os.Bundle;

import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseActivity;

/**
 * Màn hình Điều khoản sử dụng và các chính sách cửa hàng.
 */
public class ProfileTermsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContent(R.layout.profile_terms_activity, R.id.terms_root);
        bindBackButton(R.id.terms_back_btn);
    }
}
