package com.example.shoes_store.module.profile;

/**
 * Địa chỉ giao hàng trong sổ địa chỉ.
 */
public class ProfileAddressModel {

    private final String id;
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
        this.id = id;
        this.receiverName = receiverName;
        this.phone = phone;
        this.addressDetail = addressDetail;
        this.label = label;
        this.defaultAddress = defaultAddress;
    }

    public String getId() {
        return id;
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
