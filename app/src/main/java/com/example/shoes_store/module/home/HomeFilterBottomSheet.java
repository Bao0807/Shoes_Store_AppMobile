package com.example.shoes_store.module.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;

import java.util.List;
import java.util.Set;

/**
 * BottomSheet lọc giày chạy bộ theo nhu cầu (chọn nhiều), thương hiệu (chọn nhiều) và khoảng giá.
 */
public class HomeFilterBottomSheet extends BottomSheetDialogFragment {

    public interface OnFilterAppliedListener {
        void onFilterApplied(HomeFilterState updatedState);
    }

    private static final String[] CATEGORIES = {"Tất cả", "Daily", "Race", "Tempo", "Trail", "Recovery"};
    private static final String[] BRANDS = {"Tất cả", "Nike", "ASICS", "HOKA", "Adidas", "Saucony", "Brooks"};

    private final HomeRepository repository = new HomeRepository();
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
        TextView categoryHint = view.findViewById(R.id.filter_category_hint);
        TextView brandHint = view.findViewById(R.id.filter_brand_hint);
        TextView priceLabel = view.findViewById(R.id.filter_price_label);
        RangeSlider priceSlider = view.findViewById(R.id.filter_price_slider);
        MaterialButton applyBtn = view.findViewById(R.id.filter_apply_btn);

        populateChips(categoryGroup, CATEGORIES, value -> {
            if (HomeFilterState.ALL.equals(value)) {
                workingState.clearCategories();
            } else {
                workingState.toggleCategory(value);
            }
            syncFilterUi(categoryGroup, brandGroup, categoryHint, brandHint, applyBtn);
        });

        populateChips(brandGroup, BRANDS, value -> {
            if (HomeFilterState.ALL.equals(value)) {
                workingState.clearBrands();
            } else {
                workingState.toggleBrand(value);
            }
            syncFilterUi(categoryGroup, brandGroup, categoryHint, brandHint, applyBtn);
        });

        priceSlider.setValues((float) workingState.getMinPrice(), (float) workingState.getMaxPrice());
        updatePriceText(priceLabel, workingState.getMinPrice(), workingState.getMaxPrice());
        syncFilterUi(categoryGroup, brandGroup, categoryHint, brandHint, applyBtn);

        priceSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            if (values.size() >= 2) {
                long min = values.get(0).longValue();
                long max = values.get(1).longValue();
                workingState.setMinPrice(min);
                workingState.setMaxPrice(max);
                updatePriceText(priceLabel, min, max);
                updateApplyButtonText(applyBtn);
            }
        });

        view.findViewById(R.id.filter_reset_btn).setOnClickListener(v -> {
            workingState.resetFiltersOnly();
            priceSlider.setValues((float) HomeFilterState.DEFAULT_MIN_PRICE, (float) HomeFilterState.DEFAULT_MAX_PRICE);
            updatePriceText(priceLabel, HomeFilterState.DEFAULT_MIN_PRICE, HomeFilterState.DEFAULT_MAX_PRICE);
            syncFilterUi(categoryGroup, brandGroup, categoryHint, brandHint, applyBtn);
        });

        applyBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFilterApplied(new HomeFilterState(workingState));
            }
            dismiss();
        });
    }

    private interface ChipClickCallback {
        void onChipClicked(String value);
    }

    private void populateChips(ChipGroup group, String[] items, ChipClickCallback callback) {
        group.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (String label : items) {
            Chip chip = (Chip) inflater.inflate(R.layout.home_chip_template, group, false);
            chip.setId(View.generateViewId());
            chip.setText(label);
            chip.setOnClickListener(v -> callback.onChipClicked(label));
            group.addView(chip);
        }
    }

    private void syncFilterUi(
            ChipGroup categoryGroup,
            ChipGroup brandGroup,
            TextView categoryHint,
            TextView brandHint,
            MaterialButton applyBtn
    ) {
        syncChipGroup(categoryGroup, workingState.getSelectedCategories());
        syncChipGroup(brandGroup, workingState.getSelectedBrands());
        updateGroupHint(categoryHint, workingState.getSelectedCategories().size());
        updateGroupHint(brandHint, workingState.getSelectedBrands().size());
        updateApplyButtonText(applyBtn);
    }

    private void syncChipGroup(ChipGroup group, Set<String> selectedValues) {
        boolean allSelected = selectedValues.isEmpty();
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (!(child instanceof Chip)) {
                continue;
            }
            Chip chip = (Chip) child;
            String label = chip.getText().toString();
            boolean shouldBeChecked = HomeFilterState.ALL.equals(label)
                    ? allSelected
                    : containsIgnoreCase(selectedValues, label);
            if (chip.isChecked() != shouldBeChecked) {
                chip.setChecked(shouldBeChecked);
            }
        }
    }

    private boolean containsIgnoreCase(Set<String> values, String target) {
        for (String item : values) {
            if (target.equalsIgnoreCase(item)) {
                return true;
            }
        }
        return false;
    }

    private void updateGroupHint(TextView hintView, int selectedCount) {
        if (selectedCount <= 0) {
            hintView.setText("Chọn nhiều");
            hintView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_tertiary));
        } else {
            hintView.setText("Đã chọn " + selectedCount);
            hintView.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_accent));
        }
    }

    private void updateApplyButtonText(MaterialButton applyBtn) {
        if (workingState.hasAnyFilter()) {
            int matchCount = repository.filterAndSort(workingState).size();
            applyBtn.setText("Áp dụng (" + matchCount + " mẫu)");
        } else {
            applyBtn.setText(R.string.filter_apply);
        }
    }

    private void updatePriceText(TextView label, long min, long max) {
        label.setText(CurrencyFormatUtil.formatVnd(min) + " - " + CurrencyFormatUtil.formatVnd(max));
    }
}
