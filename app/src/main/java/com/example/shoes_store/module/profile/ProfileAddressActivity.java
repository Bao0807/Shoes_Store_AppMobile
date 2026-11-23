package com.example.shoes_store.module.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.UiInsetHelper;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Màn hình Quản lý sổ địa chỉ giao hàng (hỗ trợ cả chế độ chọn địa chỉ khi Checkout).
 */
public class ProfileAddressActivity extends AppCompatActivity {

    public static final String EXTRA_PICK_MODE = "extra_pick_mode";
    public static final String EXTRA_SELECTED_ADDRESS_ID = "extra_selected_address_id";

    private ProfileViewModel viewModel;
    private ProfileAddressAdapter adapter;
    private boolean pickMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.profile_address_activity);
        UiInsetHelper.applySystemBarInsets(findViewById(R.id.address_root));

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        pickMode = getIntent().getBooleanExtra(EXTRA_PICK_MODE, false);

        TextView titleView = findViewById(R.id.address_title);
        if (pickMode) {
            titleView.setText("Chọn địa chỉ giao hàng");
        }

        findViewById(R.id.address_back_btn).setOnClickListener(v -> finish());
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
                viewModel.setDefaultAddress(address.getId());
                Toast.makeText(ProfileAddressActivity.this, "Đã đặt làm địa chỉ mặc định", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEditAddress(ProfileAddressModel address) {
                showAddressDialog(address);
            }

            @Override
            public void onDeleteAddress(ProfileAddressModel address) {
                viewModel.deleteAddress(address.getId());
                Toast.makeText(ProfileAddressActivity.this, "Đã xóa địa chỉ", Toast.LENGTH_SHORT).show();
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

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Thêm địa chỉ mới" : "Cập nhật địa chỉ")
                .setView(dialogView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
                    String phone = phoneInput.getText() != null ? phoneInput.getText().toString().trim() : "";
                    String detail = detailInput.getText() != null ? detailInput.getText().toString().trim() : "";
                    if (name.isEmpty() || phone.isEmpty() || detail.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin địa chỉ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String label = labelGroup.getCheckedChipId() == R.id.chip_addr_office ? "Văn phòng" : "Nhà riêng";
                    viewModel.addOrUpdateAddress(
                            existing != null ? existing.getId() : null,
                            name,
                            phone,
                            detail,
                            label,
                            defaultCb.isChecked()
                    );
                })
                .show();
    }
}
