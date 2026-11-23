package com.example.shoes_store.module.profile;

/**
 * Hồ sơ cá nhân & thông tin chạy bộ của người dùng.
 */
public class ProfileModel {

    private String fullName;
    private String email;
    private String phone;
    private String preferredSize;
    private String runningGoal;
    private final int avatarResId;

    public ProfileModel(
            String fullName,
            String email,
            String phone,
            String preferredSize,
            String runningGoal,
            int avatarResId
    ) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.preferredSize = preferredSize;
        this.runningGoal = runningGoal;
        this.avatarResId = avatarResId;
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

    public int getAvatarResId() {
        return avatarResId;
    }
}
