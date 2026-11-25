package com.example.shoes_store.core;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;

/**
 * Hợp đồng vòng đời phiên đăng nhập để các Repository tự đồng bộ dữ liệu mà không tạo phụ thuộc ngược từ {@code core/}.
 */
public interface SessionSyncCallback {

    default void onAppInit() {
    }

    default void onSessionStart(@NonNull String uid, @NonNull FirebaseUser authUser) {
    }

    default void onSessionEnd() {
    }
}
