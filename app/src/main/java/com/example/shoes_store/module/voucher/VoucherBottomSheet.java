package com.example.shoes_store.module.voucher;

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
public class VoucherBottomSheet extends BottomSheetDialogFragment {

    public interface OnVoucherSelectedListener {
        void onVoucherSelected(@Nullable VoucherModel voucher);
    }

    private List<VoucherModel> vouchers = new ArrayList<>();
    private long subtotal = 0L;
    @Nullable
    private VoucherModel workingSelection;
    private OnVoucherSelectedListener listener;

    public static VoucherBottomSheet newInstance(
            List<VoucherModel> vouchers,
            long subtotal,
            @Nullable VoucherModel currentVoucher,
            OnVoucherSelectedListener listener
    ) {
        VoucherBottomSheet sheet = new VoucherBottomSheet();
        sheet.vouchers = vouchers != null ? vouchers : new ArrayList<>();
        sheet.subtotal = subtotal;
        sheet.workingSelection = currentVoucher;
        sheet.listener = listener;
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && listener == null) {
            dismissAllowingStateLoss();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.voucher_sheet_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (listener == null) {
            dismissAllowingStateLoss();
            return;
        }

        TextView subtotalText = view.findViewById(R.id.voucher_sheet_subtotal);
        subtotalText.setText("Đơn tạm tính: " + CurrencyFormatUtil.formatVnd(subtotal));

        EditText codeInput = view.findViewById(R.id.voucher_code_input);
        RecyclerView recyclerView = view.findViewById(R.id.voucher_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        String initialCode = workingSelection != null ? workingSelection.getCode() : null;
        VoucherAdapter[] adapterHolder = new VoucherAdapter[1];
        adapterHolder[0] = new VoucherAdapter(vouchers, subtotal, initialCode, voucher -> {
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
            VoucherModel matched = VoucherService.findByCode(vouchers, entered);
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
}
