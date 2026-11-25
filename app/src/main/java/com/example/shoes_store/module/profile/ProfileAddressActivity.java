package com.example.shoes_store.module.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseActivity;
import com.example.shoes_store.core.DialogHelper;
import com.example.shoes_store.core.InputValidatorUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Màn hình Quản lý sổ địa chỉ giao hàng (hỗ trợ cả chế độ chọn địa chỉ khi Checkout).
 */
public class ProfileAddressActivity extends BaseActivity {

    public static final String EXTRA_PICK_MODE = "extra_pick_mode";
    public static final String EXTRA_SELECTED_ADDRESS_ID = "extra_selected_address_id";

    private ProfileViewModel viewModel;
    private ProfileAddressAdapter adapter;
    private boolean pickMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContent(R.layout.profile_address_activity, R.id.address_root);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        pickMode = getIntent().getBooleanExtra(EXTRA_PICK_MODE, false);

        TextView titleView = findViewById(R.id.address_title);
        if (pickMode) {
            titleView.setText("Chọn địa chỉ giao hàng");
        }

        bindBackButton(R.id.address_back_btn);
        findViewById(R.id.address_add_btn).setOnClickListener(v -> showAddressDialog(null));

        RecyclerView recyclerView = findViewById(R.id.address_recycler);
        adapter = new ProfileAddressAdapter(new ProfileAddressAdapter.OnAddressActionListener() {
            @Override
            public void onSelectAddress(ProfileAddressModel address) {
                if (pickMode) {
                    Intent data = new Intent();
                    data.putExtra(EXTRA_SELECTED_ADDRESS_ID, address.getId());
                    setResult(RESULT_OK, data);
                    finish();
                }
            }

            @Override
            public void onSetDefault(ProfileAddressModel address) {
                viewModel.setDefaultAddress(address.getId(), new ResourceCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        showToast("Đã đặt làm địa chỉ mặc định");
                    }

                    @Override
                    public void onError(String message) {
                        showToast(message);
                    }
                });
            }

            @Override
            public void onEditAddress(ProfileAddressModel address) {
                showAddressDialog(address);
            }

            @Override
            public void onDeleteAddress(ProfileAddressModel address) {
                viewModel.deleteAddress(address.getId(), new ResourceCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        showToast("Đã xóa địa chỉ");
                    }

                    @Override
                    public void onError(String message) {
                        showToast(message);
                    }
                });
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        viewModel.getAddressesLiveData().observe(this, list -> adapter.submitList(list));
    }

    private void showAddressDialog(@Nullable ProfileAddressModel existing) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.profile_dialog_address, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.dialog_addr_name);
        TextInputEditText phoneInput = dialogView.findViewById(R.id.dialog_addr_phone);
        TextInputEditText detailInput = dialogView.findViewById(R.id.dialog_addr_detail);
        ChipGroup labelGroup = dialogView.findViewById(R.id.dialog_addr_label_chips);
        MaterialCheckBox defaultCb = dialogView.findViewById(R.id.dialog_addr_default_cb);

        if (existing != null) {
            nameInput.setText(existing.getReceiverName());
            phoneInput.setText(existing.getPhone());
            detailInput.setText(existing.getAddressDetail());
            labelGroup.check("Văn phòng".equals(existing.getLabel()) ? R.id.chip_addr_office : R.id.chip_addr_home);
            defaultCb.setChecked(existing.isDefaultAddress());
        }

        DialogHelper.showFormDialog(
                this,
                existing == null ? "Thêm địa chỉ mới" : "Cập nhật địa chỉ",
                dialogView,
                "Hủy",
                "Lưu",
                (dialog, saveBtn) -> {
                    String name = DialogHelper.textOf(nameInput);
                    String phone = DialogHelper.textOf(phoneInput);
                    String detail = DialogHelper.textOf(detailInput);

                    String nameError = InputValidatorUtil.validateFullName(name);
                    if (nameError != null) {
                        showToast(nameError);
                        return;
                    }
                    String phoneError = InputValidatorUtil.validatePhone(phone);
                    if (phoneError != null) {
                        showToast(phoneError);
                        return;
                    }
                    if (detail.isEmpty()) {
                        showToast("Vui lòng nhập địa chỉ chi tiết");
                        return;
                    }

                    String label = labelGroup.getCheckedChipId() == R.id.chip_addr_office ? "Văn phòng" : "Nhà riêng";
                    saveBtn.setEnabled(false);
                    saveBtn.setText("Đang lưu...");
                    viewModel.addOrUpdateAddress(
                            existing != null ? existing.getId() : null,
                            name,
                            phone,
                            detail,
                            label,
                            defaultCb.isChecked(),
                            new ResourceCallback<Void>() {
                                @Override
                                public void onSuccess(Void data) {
                                    if (!isAlive()) {
                                        return;
                                    }
                                    if (dialog.isShowing()) {
                                        dialog.dismiss();
                                    }
                                    showToast("Đã lưu địa chỉ giao hàng");
                                }

                                @Override
                                public void onError(String message) {
                                    if (!isAlive()) {
                                        return;
                                    }
                                    saveBtn.setEnabled(true);
                                    saveBtn.setText("Lưu");
                                    showToast(message);
                                }
                            }
                    );
                }
        );
    }
}
