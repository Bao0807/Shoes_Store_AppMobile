package com.example.shoes_store.module.admin;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.shoes_store.R;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.product.ProductModel;

import java.util.Arrays;
import java.util.Collections;
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
            int defaultStock = existing.getOutOfStockSizes().size() >= existing.getAllSizes().size()
                    && !existing.getAllSizes().isEmpty() ? 0 : 10;
            stockInput.setText(String.valueOf(defaultStock));
            descInput.setText(existing.getDescription());
        } else {
            categoryInput.setText("Daily");
            colorsInput.setText("Trắng / Cam, Đen");
            sizesInput.setText("39, 40, 41, 42, 43");
            stockInput.setText("10");
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(existing == null ? "Thêm mẫu giày mới" : "Sửa giày & Cập nhật tồn kho")
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu & Đồng bộ kho", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(v -> {
                String name = textOf(nameInput);
                String brand = textOf(brandInput);
                String category = textOf(categoryInput);
                long price = parseLong(textOf(priceInput), 0L);
                long origPrice = parseLong(textOf(origPriceInput), price);
                String thumbUrl = textOf(thumbInput);
                List<String> colors = AdminService.parseCsvList(textOf(colorsInput), DEFAULT_COLORS);
                List<String> hexes = existing != null && !existing.getColorHexes().isEmpty()
                        ? existing.getColorHexes()
                        : DEFAULT_HEXES;
                List<String> sizes = AdminService.parseCsvList(textOf(sizesInput), DEFAULT_SIZES);
                int stockPerSize = (int) parseLong(textOf(stockInput), 10L);
                String desc = textOf(descInput);

                String weight = existing != null && !existing.getWeightGrams().isEmpty() ? existing.getWeightGrams() : "250g";
                String drop = existing != null && !existing.getHeelDropMm().isEmpty() ? existing.getHeelDropMm() : "8 mm";
                String cushion = existing != null && !existing.getCushionLevel().isEmpty() ? existing.getCushionLevel() : "Cao";
                String surface = existing != null && !existing.getSurfaceType().isEmpty() ? existing.getSurfaceType() : "Road";
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
                        stockPerSize,
                        active,
                        new ResourceCallback<Void>() {
                            @Override
                            public void onSuccess(Void data) {
                                Toast.makeText(context, "Đã lưu sản phẩm và đồng bộ tồn kho các size", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }

                            @Override
                            public void onError(String message) {
                                saveBtn.setEnabled(true);
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            });
        });

        dialog.show();
    }

    @NonNull
    private static String textOf(@NonNull EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private static long parseLong(@NonNull String raw, long fallback) {
        if (raw.isEmpty()) {
            return fallback;
        }
        try {
            return Long.parseLong(raw.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
