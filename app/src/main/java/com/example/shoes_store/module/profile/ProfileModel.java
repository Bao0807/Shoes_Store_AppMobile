package com.example.shoes_store.module.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.R;

/**
 * Hồ sơ cá nhân & thông tin chạy bộ của người dùng.
 */
public class ProfileModel {

    private String uid;
    private String fullName;
    private String email;
    private String phone;
    private String preferredSize;
    private String runningGoal;
    private String role;
    private final int avatarResId;
    @Nullable
    private String avatarUri;

    public ProfileModel(
            String fullName,
            String email,
            String phone,
            String preferredSize,
            String runningGoal,
            int avatarResId
    ) {
        this("", fullName, email, phone, preferredSize, runningGoal, "CUSTOMER", avatarResId, null);
    }

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
        this.runningGoal = runningGoal != null && !runningGoal.isEmpty() ? runningGoal : "Daily";
        this.role = role != null && !role.isEmpty() ? role : "CUSTOMER";
        this.avatarResId = avatarResId != 0 ? avatarResId : R.drawable.img_avatar_runner;
        this.avatarUri = avatarUri;
    }

    @NonNull
    public String getUid() {
        return uid;
    }

    public void setUid(@Nullable String uid) {
        this.uid = uid != null ? uid : "";
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPreferredSize() {
        return preferredSize;
    }

    public void setPreferredSize(String preferredSize) {
        this.preferredSize = preferredSize;
    }

    public String getRunningGoal() {
        return runningGoal;
    }

    public void setRunningGoal(String runningGoal) {
        this.runningGoal = runningGoal;
    }

    @NonNull
    public String getRole() {
        return role;
    }

    public void setRole(@Nullable String role) {
        this.role = role != null && !role.isEmpty() ? role : "CUSTOMER";
    }

    public int getAvatarResId() {
        return avatarResId;
    }

    @Nullable
    public String getAvatarUri() {
        return avatarUri;
    }

    public void setAvatarUri(@Nullable String avatarUri) {
        this.avatarUri = avatarUri;
    }
}
