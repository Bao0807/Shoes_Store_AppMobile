package com.example.shoes_store.module.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Địa chỉ giao hàng trong sổ địa chỉ.
 */
public class ProfileAddressModel {

    private final String id;
    private final String userId;
    private String receiverName;
    private String phone;
    private String addressDetail;
    private String label;
    private boolean defaultAddress;

    public ProfileAddressModel(
            String id,
            String receiverName,
            String phone,
            String addressDetail,
            String label,
            boolean defaultAddress
    ) {
        this(id, "", receiverName, phone, addressDetail, label, defaultAddress);
    }

    public ProfileAddressModel(
            String id,
            @Nullable String userId,
            String receiverName,
            String phone,
            String addressDetail,
            String label,
            boolean defaultAddress
    ) {
        this.id = id != null ? id : "";
        this.userId = userId != null ? userId : "";
        this.receiverName = receiverName != null ? receiverName : "";
        this.phone = phone != null ? phone : "";
        this.addressDetail = addressDetail != null ? addressDetail : "";
        this.label = label != null ? label : "Nhà riêng";
        this.defaultAddress = defaultAddress;
    }

    public String getId() {
        return id;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddressDetail() {
        return addressDetail;
    }

    public void setAddressDetail(String addressDetail) {
        this.addressDetail = addressDetail;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public void setDefaultAddress(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
    }
}
