package com.example.shoes_store.module.auth;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

/**
 * Kiểm tra hợp lệ đầu vào cho luồng Đăng nhập và Đăng ký.
 */
public final class AuthValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^0\\d{9,10}$");

    private AuthValidator() {
    }

    @Nullable
    public static String validateFullName(@Nullable String fullName) {
        if (fullName == null || fullName.trim().length() < 2) {
            return "Vui lòng nhập họ và tên (tối thiểu 2 ký tự)";
        }
        return null;
    }

    @Nullable
    public static String validatePhone(@Nullable String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "Vui lòng nhập số điện thoại";
        }
        String digits = phone.trim().replaceAll("\\s+", "");
        if (!PHONE_PATTERN.matcher(digits).matches()) {
            return "Số điện thoại phải bắt đầu bằng 0 và gồm 10–11 chữ số";
        }
        return null;
    }

    @Nullable
    public static String validateEmail(@Nullable String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Vui lòng nhập email";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "Định dạng email không hợp lệ";
        }
        return null;
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
