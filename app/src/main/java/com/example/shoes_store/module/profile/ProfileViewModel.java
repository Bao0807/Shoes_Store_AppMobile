package com.example.shoes_store.module.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.module.order.OrderModel;

import java.util.List;

/**
 * ViewModel cho tab Tài khoản, Sửa hồ sơ và Sổ địa chỉ.
 */
public class ProfileViewModel extends ViewModel {

    private final ProfileRepository repository = new ProfileRepository();

    public LiveData<ProfileModel> getProfileLiveData() {
        return repository.getProfileLiveData();
    }

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return repository.getOrdersLiveData();
    }

    public LiveData<List<ProfileAddressModel>> getAddressesLiveData() {
        return repository.getAddressesLiveData();
    }

    public void updateProfile(
            @NonNull String fullName,
            @NonNull String phone,
            @NonNull String email,
            @NonNull String preferredSize,
            @NonNull String runningGoal,
            @Nullable String avatarUri
    ) {
        repository.updateProfile(fullName, phone, email, preferredSize, runningGoal, avatarUri);
    }

    public void addOrUpdateAddress(
            @Nullable String editId,
            @NonNull String receiverName,
            @NonNull String phone,
            @NonNull String addressDetail,
            @NonNull String label,
            boolean isDefault
    ) {
        repository.addOrUpdateAddress(editId, receiverName, phone, addressDetail, label, isDefault);
    }

    public void setDefaultAddress(@NonNull String id) {
        repository.setDefaultAddress(id);
    }

    public void deleteAddress(@NonNull String id) {
        repository.deleteAddress(id);
    }
}
