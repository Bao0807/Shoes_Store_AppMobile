package com.example.shoes_store.module.admin;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CategoryStyleHelper;
import com.example.shoes_store.core.DialogHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.product.ProductMapper;
import com.example.shoes_store.module.product.ProductModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hộp thoại Thêm mới / Cập nhật sản phẩm kèm khởi tạo & đồng bộ tồn kho biến thể (`variants`).
 */
public final class AdminProductDialog {

    private static final List<String> DEFAULT_COLORS = Arrays.asList("Trắng / Cam", "Đen / Trắng");
    private static final List<String> DEFAULT_HEXES = Arrays.asList("#FF5A36", "#111827", "#2563EB");
    private static final List<String> DEFAULT_SIZES = Arrays.asList("39", "40", "41", "42", "43");

    private AdminProductDialog() {
    }

    public static void show(
            @NonNull Context context,
            @NonNull AdminViewModel viewModel,
            @Nullable ProductModel existing
    ) {
        View view = LayoutInflater.from(context).inflate(R.layout.admin_dialog_product, null);
        EditText nameInput = view.findViewById(R.id.dialog_prod_name);
        EditText brandInput = view.findViewById(R.id.dialog_prod_brand);
        EditText categoryInput = view.findViewById(R.id.dialog_prod_category);
        EditText priceInput = view.findViewById(R.id.dialog_prod_price);
        EditText origPriceInput = view.findViewById(R.id.dialog_prod_original_price);
        EditText thumbInput = view.findViewById(R.id.dialog_prod_thumb_url);
        EditText colorsInput = view.findViewById(R.id.dialog_prod_colors);
        EditText sizesInput = view.findViewById(R.id.dialog_prod_sizes);
        EditText stockInput = view.findViewById(R.id.dialog_prod_stock);
        EditText descInput = view.findViewById(R.id.dialog_prod_desc);

        if (existing != null) {
            nameInput.setText(existing.getName());
            brandInput.setText(existing.getBrand());
            categoryInput.setText(existing.getCategory());
            priceInput.setText(String.valueOf(existing.getPrice()));
            origPriceInput.setText(String.valueOf(existing.getOriginalPrice()));
            thumbInput.setText(existing.getThumbnailUrl());
            colorsInput.setText(TextUtils.join(", ", existing.getColorNames()));
            sizesInput.setText(TextUtils.join(", ", existing.getAllSizes()));
            stockInput.setText("");
            stockInput.setHint("Trống = giữ tồn kho cũ");
            descInput.setText(existing.getDescription());
        } else {
            categoryInput.setText(CategoryStyleHelper.CAT_DAILY);
            colorsInput.setText("Trắng / Cam, Đen");
            sizesInput.setText("39, 40, 41, 42, 43");
            stockInput.setHint("Tồn kho / size");
            stockInput.setText("10");
        }

        DialogHelper.showFormDialog(
                context,
                existing == null ? "Thêm mẫu giày mới" : "Sửa giày & Cập nhật tồn kho",
                view,
                "Hủy",
                "Lưu & Đồng bộ kho",
                (dialog, saveBtn) -> {
                    String name = DialogHelper.textOf(nameInput);
                    String brand = DialogHelper.textOf(brandInput);
                    String category = DialogHelper.textOf(categoryInput);
                    long price = DialogHelper.parseLong(DialogHelper.textOf(priceInput), 0L);
                    long origPrice = DialogHelper.parseLong(DialogHelper.textOf(origPriceInput), price);
                    String thumbUrl = DialogHelper.textOf(thumbInput);
                    List<String> colors = AdminService.parseCsvList(DialogHelper.textOf(colorsInput), DEFAULT_COLORS);
                    List<String> hexes = buildColorHexes(colors.size(), existing != null ? existing.getColorHexes() : null);
                    List<String> sizes = AdminService.parseCsvList(DialogHelper.textOf(sizesInput), DEFAULT_SIZES);
                    Integer stockOverride = resolveStockOverride(DialogHelper.textOf(stockInput), existing != null);
                    String desc = DialogHelper.textOf(descInput);

                    String weight = existing != null && !existing.getWeightGrams().isEmpty() ? existing.getWeightGrams() : ProductMapper.DEFAULT_WEIGHT_GRAMS;
                    String drop = existing != null && !existing.getHeelDropMm().isEmpty() ? existing.getHeelDropMm() : ProductMapper.DEFAULT_HEEL_DROP_MM;
                    String cushion = existing != null && !existing.getCushionLevel().isEmpty() ? existing.getCushionLevel() : ProductMapper.DEFAULT_CUSHION_LEVEL;
                    String surface = existing != null && !existing.getSurfaceType().isEmpty() ? existing.getSurfaceType() : ProductMapper.DEFAULT_SURFACE_TYPE;
                    boolean active = existing == null || existing.isActive();

                    saveBtn.setEnabled(false);
                    viewModel.saveProductWithVariants(
                            existing != null ? existing.getId() : null,
                            name,
                            brand,
                            category,
                            price,
                            origPrice,
                            thumbUrl,
                            weight,
                            drop,
                            cushion,
                            surface,
                            desc,
                            colors,
                            hexes,
                            sizes,
                            stockOverride,
                            active,
                            new ResourceCallback<Void>() {
                                @Override
                                public void onSuccess(Void data) {
                                    if (!DialogHelper.isHostAlive(context)) {
                                        return;
                                    }
                                    Toast.makeText(context, "Đã lưu sản phẩm và đồng bộ tồn kho các size", Toast.LENGTH_SHORT).show();
                                    if (dialog.isShowing()) {
                                        dialog.dismiss();
                                    }
                                }

                                @Override
                                public void onError(String message) {
                                    if (!DialogHelper.isHostAlive(context)) {
                                        return;
                                    }
                                    saveBtn.setEnabled(true);
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                }
        );
    }

    @NonNull
    private static List<String> buildColorHexes(int colorCount, @Nullable List<String> existingHexes) {
        List<String> result = new ArrayList<>();
        if (existingHexes != null) {
            result.addAll(existingHexes);
        }
        while (result.size() < colorCount) {
            result.add(DEFAULT_HEXES.get(result.size() % DEFAULT_HEXES.size()));
        }
        return result;
    }

    @Nullable
    private static Integer resolveStockOverride(@NonNull String rawStock, boolean isEditMode) {
        if (rawStock.isEmpty()) {
            return isEditMode ? null : 10;
        }
        try {
            return Integer.parseInt(rawStock);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
