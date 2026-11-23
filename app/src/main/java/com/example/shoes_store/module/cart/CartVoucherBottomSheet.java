package com.example.shoes_store.module.cart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * BottomSheet hiển thị danh sách voucher của shop để người dùng chọn hoặc nhập mã.
 */
public class CartVoucherBottomSheet extends BottomSheetDialogFragment {

    public interface OnVoucherSelectedListener {
        void onVoucherSelected(@Nullable CartVoucherModel voucher);
    }

    private List<CartVoucherModel> vouchers = new ArrayList<>();
    private long subtotal = 0L;
    @Nullable
    private CartVoucherModel workingSelection;
    private OnVoucherSelectedListener listener;

    public static CartVoucherBottomSheet newInstance(
            List<CartVoucherModel> vouchers,
            long subtotal,
            @Nullable CartVoucherModel currentVoucher,
            OnVoucherSelectedListener listener
    ) {
        CartVoucherBottomSheet sheet = new CartVoucherBottomSheet();
        sheet.vouchers = vouchers != null ? vouchers : new ArrayList<>();
        sheet.subtotal = subtotal;
        sheet.workingSelection = currentVoucher;
        sheet.listener = listener;
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cart_sheet_voucher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView subtotalText = view.findViewById(R.id.voucher_sheet_subtotal);
        subtotalText.setText("Đơn tạm tính: " + CurrencyFormatUtil.formatVnd(subtotal));

        EditText codeInput = view.findViewById(R.id.voucher_code_input);
        RecyclerView recyclerView = view.findViewById(R.id.voucher_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        String initialCode = workingSelection != null ? workingSelection.getCode() : null;
        CartVoucherAdapter[] adapterHolder = new CartVoucherAdapter[1];
        adapterHolder[0] = new CartVoucherAdapter(vouchers, subtotal, initialCode, voucher -> {
            if (!voucher.isEligible(subtotal)) {
                String reason = voucher.getIneligibleReason(subtotal);
                Toast.makeText(
                        requireContext(),
                        reason != null ? reason : "Mã giảm giá không khả dụng",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            if (workingSelection != null && workingSelection.getCode().equalsIgnoreCase(voucher.getCode())) {
                workingSelection = null;
                adapterHolder[0].setSelectedCode(null);
            } else {
                workingSelection = voucher;
                adapterHolder[0].setSelectedCode(voucher.getCode());
            }
        });
        recyclerView.setAdapter(adapterHolder[0]);

        view.findViewById(R.id.voucher_code_apply_btn).setOnClickListener(v -> {
            String entered = codeInput.getText() != null ? codeInput.getText().toString().trim() : "";
            if (entered.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập mã voucher", Toast.LENGTH_SHORT).show();
                return;
            }
            CartVoucherModel matched = findByCode(entered);
            if (matched == null) {
                Toast.makeText(requireContext(), "Mã voucher không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!matched.isEligible(subtotal)) {
                String reason = matched.getIneligibleReason(subtotal);
                Toast.makeText(
                        requireContext(),
                        reason != null ? reason : "Mã giảm giá không khả dụng",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            workingSelection = matched;
            adapterHolder[0].setSelectedCode(matched.getCode());
            Toast.makeText(requireContext(), "Đã chọn mã " + matched.getCode(), Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.voucher_clear_btn).setOnClickListener(v -> {
            if (listener != null) {
                listener.onVoucherSelected(null);
            }
            dismiss();
        });

        view.findViewById(R.id.voucher_confirm_btn).setOnClickListener(v -> {
            if (listener != null) {
                listener.onVoucherSelected(workingSelection);
            }
            dismiss();
        });
    }

    @Nullable
    private CartVoucherModel findByCode(String code) {
        for (CartVoucherModel voucher : vouchers) {
            if (voucher.getCode().equalsIgnoreCase(code)) {
                return voucher;
            }
        }
        return null;
    }
}
