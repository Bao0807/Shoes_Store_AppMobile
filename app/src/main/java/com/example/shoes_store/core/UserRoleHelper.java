package com.example.shoes_store.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Xử lý phân quyền người dùng dùng chung toàn ứng dụng ("admin" vs "users").
 */
public final class UserRoleHelper {

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USERS = "users";
    public static final String DEFAULT_ADMIN_EMAIL = "admin@gmail.com";

    private UserRoleHelper() {
    }

    public static boolean isDefaultAdminEmail(@Nullable String email) {
        return email != null && DEFAULT_ADMIN_EMAIL.equalsIgnoreCase(email.trim());
    }

    public static boolean isAdminRole(@Nullable String role) {
        if (role == null) {
            return false;
        }
        String clean = role.trim();
        return ROLE_ADMIN.equalsIgnoreCase(clean);
    }

    @NonNull
    public static String normalizeRole(@Nullable String roleValue) {
        return isAdminRole(roleValue) ? ROLE_ADMIN : ROLE_USERS;
    }

    @NonNull
    public static String resolveRole(@Nullable String roleValue, @Nullable String email) {
        if (isDefaultAdminEmail(email)) {
            return ROLE_ADMIN;
        }
        return normalizeRole(roleValue);
    }
}
