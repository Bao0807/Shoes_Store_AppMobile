package com.example.shoes_store.module.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Nghiệp vụ thuần Java cho Sổ địa chỉ: quyết định địa chỉ mặc định khi thêm/xóa.
 */
public final class ProfileAddressService {

    private ProfileAddressService() {
    }

    public static boolean shouldMarkAsDefault(
            boolean requestedDefault,
            @Nullable List<ProfileAddressModel> existingAddresses
    ) {
        return requestedDefault || existingAddresses == null || existingAddresses.isEmpty();
    }

    @Nullable
    public static String findReplacementDefaultId(
            @NonNull String deletingId,
            @Nullable List<ProfileAddressModel> existingAddresses
    ) {
        if (existingAddresses == null || existingAddresses.isEmpty()) {
            return null;
        }
        boolean deletingDefault = false;
        String replacementId = null;
        for (ProfileAddressModel item : existingAddresses) {
            if (deletingId.equals(item.getId())) {
                deletingDefault = item.isDefaultAddress();
            } else if (replacementId == null) {
                replacementId = item.getId();
            }
        }
        return deletingDefault ? replacementId : null;
    }
}
