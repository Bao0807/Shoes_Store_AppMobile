package com.example.shoes_store.module.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.shoes_store.MainActivity;
import com.example.shoes_store.R;
import com.example.shoes_store.core.AppPrefsHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.core.UiInsetHelper;
import com.example.shoes_store.module.admin.AdminDashboardActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Màn hình Đăng nhập bằng Email / Mật khẩu (hỗ trợ Guest-first quay về tab mục tiêu).
 */
public class AuthLoginActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_TAB = "extra_target_tab";

    private AuthViewModel viewModel;
    private int targetTabId = 0;

    public static void open(@NonNull Context context) {
        Intent intent = new Intent(context, AuthLoginActivity.class);
        context.startActivity(intent);
    }

    public static void openForTab(@NonNull Context context, int targetTabId) {
        Intent intent = new Intent(context, AuthLoginActivity.class);
        intent.putExtra(EXTRA_TARGET_TAB, targetTabId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.auth_login_activity);
        UiInsetHelper.applySystemBarInsets(findViewById(R.id.auth_login_root));

        targetTabId = getIntent().getIntExtra(EXTRA_TARGET_TAB, 0);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        TextInputLayout emailLayout = findViewById(R.id.auth_login_email_layout);
        TextInputLayout passwordLayout = findViewById(R.id.auth_login_password_layout);
        TextInputEditText emailInput = findViewById(R.id.auth_login_email);
        TextInputEditText passwordInput = findViewById(R.id.auth_login_password);
        MaterialButton submitBtn = findViewById(R.id.auth_login_submit_btn);

        String savedEmail = AppPrefsHelper.getLastLoginEmail();
        if (!savedEmail.isEmpty()) {
            emailInput.setText(savedEmail);
        }

        findViewById(R.id.auth_login_back_btn).setOnClickListener(v -> finish());

        findViewById(R.id.auth_go_register_btn).setOnClickListener(v -> {
            Intent intent = new Intent(this, AuthRegisterActivity.class);
            if (targetTabId != 0) {
                intent.putExtra(EXTRA_TARGET_TAB, targetTabId);
            }
            startActivity(intent);
        });

        findViewById(R.id.auth_forgot_btn).setOnClickListener(v -> {
            String prefill = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            showForgotPasswordDialog(prefill);
        });

        viewModel.getLoadingLiveData().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            submitBtn.setEnabled(!isLoading);
            submitBtn.setText(isLoading ? "Đang đăng nhập..." : getString(R.string.auth_login_btn));
        });

        submitBtn.setOnClickListener(v -> {
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";

            String emailError = AuthValidator.validateEmail(email);
            String passError = AuthValidator.validatePassword(password);
            emailLayout.setError(emailError);
            passwordLayout.setError(passError);
            if (emailError != null || passError != null) {
                return;
            }

            viewModel.loginAndResolveAdmin(email, password, new ResourceCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean isAdmin) {
                    if (Boolean.TRUE.equals(isAdmin)) {
                        Toast.makeText(
                                AuthLoginActivity.this,
                                "Xin chào Quản trị viên (Admin)",
                                Toast.LENGTH_SHORT
                        ).show();
                        MainActivity.markAdminAutoRouted();
                        Intent adminIntent = new Intent(AuthLoginActivity.this, AdminDashboardActivity.class);
                        adminIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(adminIntent);
                        finish();
                        return;
                    }
                    Toast.makeText(AuthLoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                    navigateAfterLogin();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(AuthLoginActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void showForgotPasswordDialog(@Nullable String prefillEmail) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("Nhập email tài khoản");
        if (prefillEmail != null && !prefillEmail.isEmpty()) {
            input.setText(prefillEmail);
            input.setSelection(prefillEmail.length());
        }

        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Khôi phục mật khẩu")
                .setMessage("Chúng tôi sẽ gửi liên kết đặt lại mật khẩu vào email của bạn.")
                .setView(container)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi email", (dialog, which) -> {
                    String email = input.getText() != null ? input.getText().toString().trim() : "";
                    String err = AuthValidator.validateEmail(email);
                    if (err != null) {
                        Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.sendPasswordReset(email, new ResourceCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            Toast.makeText(
                                    AuthLoginActivity.this,
                                    "Đã gửi email đặt lại mật khẩu tới " + email,
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(AuthLoginActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .show();
    }

    private void navigateAfterLogin() {
        if (targetTabId != 0) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(MainActivity.EXTRA_TARGET_TAB, targetTabId);
            startActivity(intent);
        } else {
            setResult(RESULT_OK);
        }
        finish();
    }
}
