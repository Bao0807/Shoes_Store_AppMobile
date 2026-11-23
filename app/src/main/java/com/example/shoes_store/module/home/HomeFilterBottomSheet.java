package com.example.shoes_store.module.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;

import java.util.List;

/**
 * BottomSheet lọc giày chạy bộ theo nhu cầu, thương hiệu và khoảng giá.
 */
public class HomeFilterBottomSheet extends BottomSheetDialogFragment {

    public interface OnFilterAppliedListener {
        void onFilterApplied(HomeFilterState updatedState);
    }

    private static final String[] CATEGORIES = {"Tất cả", "Daily", "Race", "Tempo", "Trail", "Recovery"};
    private static final String[] BRANDS = {"Tất cả", "Nike", "ASICS", "HOKA", "Adidas", "Saucony", "Brooks"};

    private HomeFilterState workingState = new HomeFilterState();
    private OnFilterAppliedListener listener;

    public static HomeFilterBottomSheet newInstance(HomeFilterState currentState, OnFilterAppliedListener listener) {
        HomeFilterBottomSheet sheet = new HomeFilterBottomSheet();
        sheet.workingState = new HomeFilterState(currentState);
        sheet.listener = listener;
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_sheet_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ChipGroup categoryGroup = view.findViewById(R.id.filter_category_chips);
        ChipGroup brandGroup = view.findViewById(R.id.filter_brand_chips);
        TextView priceLabel = view.findViewById(R.id.filter_price_label);
        RangeSlider priceSlider = view.findViewById(R.id.filter_price_slider);

        populateChips(categoryGroup, CATEGORIES, workingState.getCategory(), selected -> workingState.setCategory(selected));
        populateChips(brandGroup, BRANDS, workingState.getBrand(), selected -> workingState.setBrand(selected));

        priceSlider.setValues((float) workingState.getMinPrice(), (float) workingState.getMaxPrice());
        updatePriceText(priceLabel, workingState.getMinPrice(), workingState.getMaxPrice());

        priceSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            if (values.size() >= 2) {
                long min = values.get(0).longValue();
                long max = values.get(1).longValue();
                workingState.setMinPrice(min);
                workingState.setMaxPrice(max);
                updatePriceText(priceLabel, min, max);
            }
        });

        view.findViewById(R.id.filter_reset_btn).setOnClickListener(v -> {
            workingState.resetFiltersOnly();
            selectChipByText(categoryGroup, HomeFilterState.ALL);
            selectChipByText(brandGroup, HomeFilterState.ALL);
            priceSlider.setValues((float) HomeFilterState.DEFAULT_MIN_PRICE, (float) HomeFilterState.DEFAULT_MAX_PRICE);
            updatePriceText(priceLabel, HomeFilterState.DEFAULT_MIN_PRICE, HomeFilterState.DEFAULT_MAX_PRICE);
        });

        view.findViewById(R.id.filter_apply_btn).setOnClickListener(v -> {
            if (listener != null) {
                listener.onFilterApplied(new HomeFilterState(workingState));
            }
            dismiss();
        });
    }

    private interface ChipSelectionCallback {
        void onSelected(String value);
    }

    private void populateChips(ChipGroup group, String[] items, String selectedValue, ChipSelectionCallback callback) {
        group.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (String label : items) {
            Chip chip = (Chip) inflater.inflate(R.layout.home_chip_template, group, false);
            chip.setId(View.generateViewId());
            chip.setText(label);
            chip.setChecked(label.equalsIgnoreCase(selectedValue));
            chip.setOnClickListener(v -> callback.onSelected(label));
            group.addView(chip);
        }
    }

    private void selectChipByText(ChipGroup group, String text) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (text.equalsIgnoreCase(chip.getText().toString())) {
                    chip.setChecked(true);
                    break;
                }
            }
        }
    }

    private void updatePriceText(TextView label, long min, long max) {
        label.setText(CurrencyFormatUtil.formatVnd(min) + " - " + CurrencyFormatUtil.formatVnd(max));
    }
}
