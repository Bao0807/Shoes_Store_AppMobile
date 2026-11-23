package com.example.shoes_store.module.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.shoes_store.MainActivity;
import com.example.shoes_store.R;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.core.UiInsetHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

/**
 * Màn hình Đăng ký tài khoản runner mới và khởi tạo hồ sơ trên Cloud Firestore.
 */
public class AuthRegisterActivity extends AppCompatActivity {

    private AuthViewModel viewModel;
    private int targetTabId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.auth_register_activity);
        UiInsetHelper.applySystemBarInsets(findViewById(R.id.auth_register_root));

        targetTabId = getIntent().getIntExtra(AuthLoginActivity.EXTRA_TARGET_TAB, 0);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        TextInputLayout nameLayout = findViewById(R.id.auth_reg_name_layout);
        TextInputLayout phoneLayout = findViewById(R.id.auth_reg_phone_layout);
        TextInputLayout emailLayout = findViewById(R.id.auth_reg_email_layout);
        TextInputLayout passwordLayout = findViewById(R.id.auth_reg_password_layout);
        TextInputLayout confirmLayout = findViewById(R.id.auth_reg_confirm_layout);

        TextInputEditText nameInput = findViewById(R.id.auth_reg_name);
        TextInputEditText phoneInput = findViewById(R.id.auth_reg_phone);
        TextInputEditText emailInput = findViewById(R.id.auth_reg_email);
        TextInputEditText passwordInput = findViewById(R.id.auth_reg_password);
        TextInputEditText confirmInput = findViewById(R.id.auth_reg_confirm);
        MaterialButton submitBtn = findViewById(R.id.auth_reg_submit_btn);

        findViewById(R.id.auth_reg_back_btn).setOnClickListener(v -> finish());
        findViewById(R.id.auth_go_login_btn).setOnClickListener(v -> finish());

        viewModel.getLoadingLiveData().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            submitBtn.setEnabled(!isLoading);
            submitBtn.setText(isLoading ? "Đang tạo tài khoản..." : getString(R.string.auth_register_btn));
        });

        submitBtn.setOnClickListener(v -> {
            String fullName = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            String phone = phoneInput.getText() != null ? phoneInput.getText().toString().trim() : "";
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";
            String confirm = confirmInput.getText() != null ? confirmInput.getText().toString() : "";

            String nameErr = AuthValidator.validateFullName(fullName);
            String phoneErr = AuthValidator.validatePhone(phone);
            String emailErr = AuthValidator.validateEmail(email);
            String passErr = AuthValidator.validatePassword(password);
            String confirmErr = AuthValidator.validateConfirmPassword(password, confirm);

            nameLayout.setError(nameErr);
            phoneLayout.setError(phoneErr);
            emailLayout.setError(emailErr);
            passwordLayout.setError(passErr);
            confirmLayout.setError(confirmErr);

            if (nameErr != null || phoneErr != null || emailErr != null || passErr != null || confirmErr != null) {
                return;
            }

            viewModel.register(fullName, phone, email, password, new ResourceCallback<FirebaseUser>() {
                @Override
                public void onSuccess(FirebaseUser data) {
                    Toast.makeText(AuthRegisterActivity.this, "Tạo tài khoản thành công", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AuthRegisterActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra(
                            MainActivity.EXTRA_TARGET_TAB,
                            targetTabId != 0 ? targetTabId : R.id.nav_home
                    );
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(AuthRegisterActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
