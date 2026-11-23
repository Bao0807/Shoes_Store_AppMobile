package com.example.shoes_store.module.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.module.voucher.VoucherModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter quản lý danh sách Voucher ưu đãi trong Kênh Quản trị (Admin).
 */
public class AdminVoucherAdapter extends RecyclerView.Adapter<AdminVoucherAdapter.VoucherViewHolder> {

    public interface OnAdminVoucherActionListener {
        void onEditVoucher(@NonNull VoucherModel voucher);

        void onToggleActive(@NonNull VoucherModel voucher, boolean isActive);
    }

    private final List<VoucherModel> vouchers = new ArrayList<>();
    private final OnAdminVoucherActionListener listener;

    public AdminVoucherAdapter(@NonNull OnAdminVoucherActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<VoucherModel> list) {
        vouchers.clear();
        if (list != null) {
            vouchers.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_item_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        VoucherModel voucher = vouchers.get(position);

        holder.badgeText.setText(voucher.getShortBadge());
        holder.codeText.setText(voucher.getCode());
        holder.titleText.setText(voucher.getTitle());
        holder.metaText.setText("Giảm " + CurrencyFormatUtil.formatVnd(voucher.getDiscountAmount())
                + " · Đơn tối thiểu " + CurrencyFormatUtil.formatVnd(voucher.getMinOrderAmount()));

        String quotaText = voucher.getUsageLimit() > 0
                ? "Đã dùng " + voucher.getUsedCount() + "/" + voucher.getUsageLimit() + " lượt"
                : "Đã dùng " + voucher.getUsedCount() + " lượt (Không giới hạn)";
        if (!voucher.getExpiryDate().isEmpty()) {
            quotaText += " · HSD: " + voucher.getExpiryDate();
        }
        holder.quotaText.setText(quotaText);

        holder.activeSwitch.setOnCheckedChangeListener(null);
        holder.activeSwitch.setChecked(voucher.isActive());
        holder.activeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                listener.onToggleActive(voucher, isChecked));

        holder.editBtn.setOnClickListener(v -> listener.onEditVoucher(voucher));
        holder.itemView.setOnClickListener(v -> listener.onEditVoucher(voucher));
    }

    @Override
    public int getItemCount() {
        return vouchers.size();
    }

    static class VoucherViewHolder extends RecyclerView.ViewHolder {
        final TextView badgeText;
        final TextView codeText;
        final TextView titleText;
        final TextView metaText;
        final TextView quotaText;
        final MaterialSwitch activeSwitch;
        final MaterialButton editBtn;

        VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            badgeText = itemView.findViewById(R.id.admin_voucher_badge);
            codeText = itemView.findViewById(R.id.admin_voucher_code);
            titleText = itemView.findViewById(R.id.admin_voucher_title);
            metaText = itemView.findViewById(R.id.admin_voucher_meta);
            quotaText = itemView.findViewById(R.id.admin_voucher_quota);
            activeSwitch = itemView.findViewById(R.id.admin_voucher_active_switch);
            editBtn = itemView.findViewById(R.id.admin_voucher_edit_btn);
        }
    }
}
