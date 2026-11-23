package com.example.shoes_store.module.profile;

import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.shoes_store.R;
import com.example.shoes_store.core.AvatarImageHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.core.UiInsetHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Chỉnh sửa thông tin cá nhân & hồ sơ chạy bộ.
 */
public class ProfileEditActivity extends AppCompatActivity {

    private static final String KEY_AVATAR_URI = "state_avatar_uri";
    private static final String KEY_FALLBACK_AVATAR_RES = "state_fallback_avatar_res";
    private static final String KEY_SELECTED_SIZE = "state_selected_size";
    private static final String KEY_SELECTED_GOAL = "state_selected_goal";
    private static final String KEY_INITIALIZED = "state_initialized";

    private static final String[] SIZES = {"38", "39", "40", "40.5", "41", "42", "43", "44"};
    private static final String[] GOALS = {"Daily", "Race", "Trail", "Daily & Half Marathon", "Full Marathon"};

    private String selectedSize = "42";
    private String selectedGoal = "Daily & Half Marathon";
    @Nullable
    private String selectedAvatarUri;
    private int fallbackAvatarResId = R.drawable.img_avatar_runner;
    private boolean hasInitializedForm = false;

    private ShapeableImageView avatarView;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) {
                    return;
                }
                String savedUri = AvatarImageHelper.saveAvatarFromUri(this, uri);
                if (savedUri != null) {
                    selectedAvatarUri = savedUri;
                    AvatarImageHelper.bindAvatar(avatarView, selectedAvatarUri, fallbackAvatarResId);
                    Toast.makeText(this, "Đã chọn ảnh đại diện mới", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Không thể đọc ảnh đã chọn", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Void> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap == null) {
                    return;
                }
                String savedUri = AvatarImageHelper.saveAvatarBitmap(this, bitmap);
                if (savedUri != null) {
                    selectedAvatarUri = savedUri;
                    AvatarImageHelper.bindAvatar(avatarView, selectedAvatarUri, fallbackAvatarResId);
                    Toast.makeText(this, "Đã chụp ảnh đại diện mới", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Không thể lưu ảnh vừa chụp", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.profile_edit_activity);
        UiInsetHelper.applySystemBarInsets(findViewById(R.id.profile_edit_root));

        if (savedInstanceState != null) {
            selectedAvatarUri = savedInstanceState.getString(KEY_AVATAR_URI);
            fallbackAvatarResId = savedInstanceState.getInt(KEY_FALLBACK_AVATAR_RES, R.drawable.img_avatar_runner);
            selectedSize = savedInstanceState.getString(KEY_SELECTED_SIZE, selectedSize);
            selectedGoal = savedInstanceState.getString(KEY_SELECTED_GOAL, selectedGoal);
            hasInitializedForm = savedInstanceState.getBoolean(KEY_INITIALIZED, false);
        }

        ProfileViewModel viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        avatarView = findViewById(R.id.edit_avatar);
        View avatarContainer = findViewById(R.id.edit_avatar_container);
        View changeAvatarBtn = findViewById(R.id.edit_change_avatar_btn);
        TextInputEditText nameInput = findViewById(R.id.edit_full_name);
        TextInputEditText phoneInput = findViewById(R.id.edit_phone);
        TextInputEditText emailInput = findViewById(R.id.edit_email);
        ChipGroup sizeChips = findViewById(R.id.edit_size_chips);
        ChipGroup goalChips = findViewById(R.id.edit_goal_chips);

        findViewById(R.id.edit_back_btn).setOnClickListener(v -> finish());
        View.OnClickListener openAvatarPicker = v -> showAvatarOptionsDialog();
        avatarContainer.setOnClickListener(openAvatarPicker);
        changeAvatarBtn.setOnClickListener(openAvatarPicker);

        if (hasInitializedForm) {
            AvatarImageHelper.bindAvatar(avatarView, selectedAvatarUri, fallbackAvatarResId);
            populateChips(sizeChips, SIZES, selectedSize, val -> selectedSize = val);
            populateChips(goalChips, GOALS, selectedGoal, val -> selectedGoal = val);
        }

        viewModel.getProfileLiveData().observe(this, profile -> {
            if (profile == null || hasInitializedForm) {
                return;
            }
            nameInput.setText(profile.getFullName());
            phoneInput.setText(profile.getPhone());
            emailInput.setText(profile.getEmail());
            selectedSize = profile.getPreferredSize();
            selectedGoal = profile.getRunningGoal();
            fallbackAvatarResId = profile.getAvatarResId();
            selectedAvatarUri = profile.getAvatarUri();

            AvatarImageHelper.bindAvatar(avatarView, selectedAvatarUri, fallbackAvatarResId);
            populateChips(sizeChips, SIZES, selectedSize, val -> selectedSize = val);
            populateChips(goalChips, GOALS, selectedGoal, val -> selectedGoal = val);
            hasInitializedForm = true;
        });

        findViewById(R.id.edit_save_btn).setOnClickListener(v -> {
            String fullName = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            String phone = phoneInput.getText() != null ? phoneInput.getText().toString().trim() : "";
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            if (fullName.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ họ tên và SĐT", Toast.LENGTH_SHORT).show();
                return;
            }
            v.setEnabled(false);
            viewModel.updateProfile(fullName, phone, email, selectedSize, selectedGoal, selectedAvatarUri,
                    new ResourceCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            Toast.makeText(ProfileEditActivity.this, "Đã lưu hồ sơ chạy bộ", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onError(String message) {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            v.setEnabled(true);
                            Toast.makeText(ProfileEditActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_AVATAR_URI, selectedAvatarUri);
        outState.putInt(KEY_FALLBACK_AVATAR_RES, fallbackAvatarResId);
        outState.putString(KEY_SELECTED_SIZE, selectedSize);
        outState.putString(KEY_SELECTED_GOAL, selectedGoal);
        outState.putBoolean(KEY_INITIALIZED, hasInitializedForm);
    }

    private void showAvatarOptionsDialog() {
        List<String> options = new ArrayList<>();
        options.add(getString(R.string.profile_avatar_from_url));
        options.add("Chọn ảnh từ thư viện");
        options.add("Chụp ảnh mới");
        boolean hasCustomAvatar = !TextUtils.isEmpty(selectedAvatarUri);
        if (hasCustomAvatar) {
            options.add("Dùng lại ảnh mặc định");
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_change_avatar)
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        showAvatarUrlDialog();
                    } else if (which == 1) {
                        launchPhotoPicker();
                    } else if (which == 2) {
                        launchCameraPreview();
                    } else if (which == 3 && hasCustomAvatar) {
                        selectedAvatarUri = null;
                        AvatarImageHelper.bindAvatar(avatarView, null, fallbackAvatarResId);
                        Toast.makeText(this, "Đã chọn lại ảnh mặc định", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showAvatarUrlDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.profile_dialog_avatar_url, null);
        TextInputLayout inputLayout = dialogView.findViewById(R.id.dialog_avatar_url_layout);
        TextInputEditText urlInput = dialogView.findViewById(R.id.dialog_avatar_url_input);

        if (AvatarImageHelper.isHttpUrl(selectedAvatarUri)) {
            urlInput.setText(selectedAvatarUri);
            if (urlInput.getText() != null) {
                urlInput.setSelection(urlInput.getText().length());
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.profile_avatar_from_url)
                .setView(dialogView)
                .setPositiveButton("Áp dụng", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setOnClickListener(v -> {
                String url = urlInput.getText() != null ? urlInput.getText().toString().trim() : "";
                if (!AvatarImageHelper.isHttpUrl(url)) {
                    inputLayout.setError("Vui lòng nhập link ảnh hợp lệ (http:// hoặc https://)");
                    return;
                }
                inputLayout.setError(null);
                positiveBtn.setEnabled(false);
                positiveBtn.setText("Đang tải...");

                AvatarImageHelper.loadAvatarFromUrl(avatarView, url, fallbackAvatarResId, (success, errorMessage) -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (success) {
                        selectedAvatarUri = url;
                        dialog.dismiss();
                        Toast.makeText(this, "Đã cập nhật ảnh đại diện từ link", Toast.LENGTH_SHORT).show();
                    } else {
                        positiveBtn.setEnabled(true);
                        positiveBtn.setText("Áp dụng");
                        inputLayout.setError(errorMessage);
                    }
                });
            });
        });

        dialog.show();
    }

    private void launchPhotoPicker() {
        try {
            pickPhotoLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Thiết bị không hỗ trợ chọn ảnh từ thư viện", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchCameraPreview() {
        try {
            takePhotoLauncher.launch(null);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Không tìm thấy ứng dụng camera", Toast.LENGTH_SHORT).show();
        }
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
