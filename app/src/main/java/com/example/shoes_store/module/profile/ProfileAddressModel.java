package com.example.shoes_store.module.profile;

/**
 * Địa chỉ giao hàng trong sổ địa chỉ.
 */
public class ProfileAddressModel {

    private final String id;
    private final String receiverName;
    private final String phone;
    private final String addressDetail;
    private final String label;
    private final boolean defaultAddress;

    public ProfileAddressModel(
            String id,
            String receiverName,
            String phone,
            String addressDetail,
            String label,
            boolean defaultAddress
    ) {
        this.id = id != null ? id : "";
        this.receiverName = receiverName != null ? receiverName : "";
        this.phone = phone != null ? phone : "";
        this.addressDetail = addressDetail != null ? addressDetail : "";
        this.label = label != null ? label : "Nhà riêng";
        this.defaultAddress = defaultAddress;
    }

    public String getId() {
        return id;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddressDetail() {
        return addressDetail;
    }

    public String getLabel() {
        return label;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }
}
