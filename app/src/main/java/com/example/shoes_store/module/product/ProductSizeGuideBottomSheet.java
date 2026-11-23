package com.example.shoes_store.module.product;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * BottomSheet bảng quy đổi size giày chạy bộ EU / US / cm.
 */
public class ProductSizeGuideBottomSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.product_sheet_size_guide, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.size_guide_close_btn).setOnClickListener(v -> dismiss());
    }
}
