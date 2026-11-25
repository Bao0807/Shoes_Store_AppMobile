package com.example.shoes_store.module.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CategoryStyleHelper;

/**
 * Hồ sơ cá nhân & thông tin chạy bộ của người dùng.
 */
public class ProfileModel {

    private final String uid;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String preferredSize;
    private final String runningGoal;
    private final String role;
    private final int avatarResId;
    @Nullable
    private final String avatarUri;

    public ProfileModel(
            @Nullable String uid,
            @Nullable String fullName,
            @Nullable String email,
            @Nullable String phone,
            @Nullable String preferredSize,
            @Nullable String runningGoal,
            @Nullable String role,
            int avatarResId,
            @Nullable String avatarUri
    ) {
        this.uid = uid != null ? uid : "";
        this.fullName = fullName != null ? fullName : "";
        this.email = email != null ? email : "";
        this.phone = phone != null ? phone : "";
        this.preferredSize = preferredSize != null && !preferredSize.isEmpty() ? preferredSize : "42";
        this.runningGoal = runningGoal != null && !runningGoal.isEmpty() ? runningGoal : CategoryStyleHelper.CAT_DAILY;
        this.role = role != null && !role.isEmpty() ? role : "CUSTOMER";
        this.avatarResId = avatarResId != 0 ? avatarResId : R.drawable.img_avatar_runner;
        this.avatarUri = avatarUri;
    }

    @NonNull
    public String getUid() {
        return uid;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPreferredSize() {
        return preferredSize;
    }

    public String getRunningGoal() {
        return runningGoal;
    }

    @NonNull
    public String getRole() {
        return role;
    }

    public int getAvatarResId() {
        return avatarResId;
    }

    @Nullable
    public String getAvatarUri() {
        return avatarUri;
    }
}
