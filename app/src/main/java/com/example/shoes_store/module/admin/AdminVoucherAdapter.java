package com.example.shoes_store.module.admin;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseListAdapter;
import com.example.shoes_store.module.voucher.VoucherModel;
import com.example.shoes_store.module.voucher.VoucherService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * Adapter quản lý danh sách Voucher ưu đãi trong Kênh Quản trị (Admin).
 */
public class AdminVoucherAdapter extends BaseListAdapter<VoucherModel, AdminVoucherAdapter.VoucherViewHolder> {

    public interface OnAdminVoucherActionListener {
        void onEditVoucher(@NonNull VoucherModel voucher);

        void onToggleActive(@NonNull VoucherModel voucher, boolean isActive);
    }

    private final OnAdminVoucherActionListener listener;

    public AdminVoucherAdapter(@NonNull OnAdminVoucherActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VoucherViewHolder(inflate(parent, R.layout.admin_item_voucher));
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        VoucherModel voucher = getItem(position);

        holder.badgeText.setText(voucher.getShortBadge());
        holder.codeText.setText(voucher.getCode());
        holder.titleText.setText(voucher.getTitle());
        holder.metaText.setText(VoucherService.formatAdminSummary(voucher));
        holder.quotaText.setText(VoucherService.formatAdminQuotaAndExpiry(voucher));

        holder.activeSwitch.setOnCheckedChangeListener(null);
        holder.activeSwitch.setChecked(voucher.isActive());
        holder.activeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                listener.onToggleActive(voucher, isChecked));

        holder.editBtn.setOnClickListener(v -> listener.onEditVoucher(voucher));
        holder.itemView.setOnClickListener(v -> listener.onEditVoucher(voucher));
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
