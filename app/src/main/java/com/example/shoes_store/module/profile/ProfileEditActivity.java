package com.example.shoes_store.module.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.shoes_store.R;
import com.example.shoes_store.core.UiInsetHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Màn hình Chỉnh sửa thông tin cá nhân & hồ sơ chạy bộ.
 */
public class ProfileEditActivity extends AppCompatActivity {

    private static final String[] SIZES = {"38", "39", "40", "40.5", "41", "42", "43", "44"};
    private static final String[] GOALS = {"Daily", "Race", "Trail", "Daily & Half Marathon", "Full Marathon"};

    private String selectedSize = "42";
    private String selectedGoal = "Daily & Half Marathon";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.profile_edit_activity);
        UiInsetHelper.applySystemBarInsets(findViewById(R.id.profile_edit_root));

        ProfileViewModel viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        TextInputEditText nameInput = findViewById(R.id.edit_full_name);
        TextInputEditText phoneInput = findViewById(R.id.edit_phone);
        TextInputEditText emailInput = findViewById(R.id.edit_email);
        ChipGroup sizeChips = findViewById(R.id.edit_size_chips);
        ChipGroup goalChips = findViewById(R.id.edit_goal_chips);

        findViewById(R.id.edit_back_btn).setOnClickListener(v -> finish());

        viewModel.getProfileLiveData().observe(this, profile -> {
            if (profile == null) {
                return;
            }
            nameInput.setText(profile.getFullName());
            phoneInput.setText(profile.getPhone());
            emailInput.setText(profile.getEmail());
            selectedSize = profile.getPreferredSize();
            selectedGoal = profile.getRunningGoal();

            populateChips(sizeChips, SIZES, selectedSize, val -> selectedSize = val);
            populateChips(goalChips, GOALS, selectedGoal, val -> selectedGoal = val);
        });

        findViewById(R.id.edit_save_btn).setOnClickListener(v -> {
            String fullName = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            String phone = phoneInput.getText() != null ? phoneInput.getText().toString().trim() : "";
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            if (fullName.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ họ tên và SĐT", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.updateProfile(fullName, phone, email, selectedSize, selectedGoal);
            Toast.makeText(this, "Đã lưu hồ sơ chạy bộ", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private interface OnChipPick {
        void onPick(String value);
    }

    private void populateChips(ChipGroup group, String[] items, String current, OnChipPick callback) {
        group.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String label : items) {
            Chip chip = (Chip) inflater.inflate(R.layout.home_chip_template, group, false);
            chip.setText(label);
            chip.setChecked(label.equalsIgnoreCase(current));
            chip.setOnClickListener(v -> callback.onPick(label));
            group.addView(chip);
        }
    }
}
