package com.example.shoes_store.module.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.shoes_store.core.MockStoreHelper;
import com.example.shoes_store.module.order.OrderModel;

import java.util.List;

/**
 * Repository cho Hồ sơ runner và Sổ địa chỉ.
 */
public class ProfileRepository {

    private final MockStoreHelper store = MockStoreHelper.getInstance();

    public LiveData<ProfileModel> getProfileLiveData() {
        return store.getProfileLiveData();
    }

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return store.getOrdersLiveData();
    }

    public LiveData<List<ProfileAddressModel>> getAddressesLiveData() {
        return store.getAddressesLiveData();
    }

    public void updateProfile(
            @NonNull String fullName,
            @NonNull String phone,
            @NonNull String email,
            @NonNull String preferredSize,
            @NonNull String runningGoal
    ) {
        store.updateProfile(fullName, phone, email, preferredSize, runningGoal);
    }

    public void addOrUpdateAddress(
            @Nullable String editId,
            @NonNull String receiverName,
            @NonNull String phone,
            @NonNull String addressDetail,
            @NonNull String label,
            boolean isDefault
    ) {
        store.addOrUpdateAddress(editId, receiverName, phone, addressDetail, label, isDefault);
    }

    public void setDefaultAddress(@NonNull String id) {
        store.setDefaultAddress(id);
    }

    public void deleteAddress(@NonNull String id) {
        store.deleteAddress(id);
    }
}
