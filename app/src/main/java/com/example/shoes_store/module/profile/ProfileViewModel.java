package com.example.shoes_store.module.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.voucher.VoucherModel;

import java.util.List;

/**
 * ViewModel cho tab Tài khoản, Sửa hồ sơ và Sổ địa chỉ.
 */
public class ProfileViewModel extends ViewModel {

    private final ProfileRepository repository = ProfileRepository.getInstance();

    public LiveData<ProfileModel> getProfileLiveData() {
        return repository.getProfileLiveData();
    }

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return repository.getOrdersLiveData();
    }

    public LiveData<List<ProfileAddressModel>> getAddressesLiveData() {
        return repository.getAddressesLiveData();
    }

    public List<VoucherModel> getVouchers() {
        return repository.getVouchers();
    }

    public void updateProfile(
            @NonNull String fullName,
            @NonNull String phone,
            @NonNull String email,
            @NonNull String preferredSize,
            @NonNull String runningGoal,
            @Nullable String avatarUri,
            @Nullable ResourceCallback<Void> callback
    ) {
        repository.updateProfile(fullName, phone, email, preferredSize, runningGoal, avatarUri, callback);
    }

    public void addOrUpdateAddress(
            @Nullable String editId,
            @NonNull String receiverName,
            @NonNull String phone,
            @NonNull String addressDetail,
            @NonNull String label,
            boolean isDefault,
            @Nullable ResourceCallback<Void> callback
    ) {
        repository.addOrUpdateAddress(editId, receiverName, phone, addressDetail, label, isDefault, callback);
    }

    public void setDefaultAddress(@NonNull String id, @Nullable ResourceCallback<Void> callback) {
        repository.setDefaultAddress(id, callback);
    }

    public void deleteAddress(@NonNull String id, @Nullable ResourceCallback<Void> callback) {
        repository.deleteAddress(id, callback);
    }
}
