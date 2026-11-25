package com.example.shoes_store.module.auth;

import androidx.annotation.Nullable;

/**
 * Kiểm tra hợp lệ mật khẩu cho luồng Đăng nhập và Đăng ký.
 */
public final class AuthValidator {

    private AuthValidator() {
    }

    @Nullable
    public static String validatePassword(@Nullable String password) {
        if (password == null || password.isEmpty()) {
            return "Vui lòng nhập mật khẩu";
        }
        if (password.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự";
        }
        return null;
    }

    @Nullable
    public static String validateConfirmPassword(@Nullable String password, @Nullable String confirm) {
        if (confirm == null || confirm.isEmpty()) {
            return "Vui lòng nhập lại mật khẩu";
        }
        if (!confirm.equals(password)) {
            return "Mật khẩu xác nhận không khớp";
        }
        return null;
    }
}
