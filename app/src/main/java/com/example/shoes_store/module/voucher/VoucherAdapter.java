package com.example.shoes_store.module.voucher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * Adapter hiển thị danh sách voucher trong BottomSheet chọn mã giảm giá.
 */
public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

    public interface OnVoucherClickListener {
        void onVoucherClick(VoucherModel voucher);
    }

    private final List<VoucherModel> vouchers;
    private final long subtotal;
    @Nullable
    private String selectedCode;
    private final OnVoucherClickListener listener;

    public VoucherAdapter(
            List<VoucherModel> vouchers,
            long subtotal,
            @Nullable String selectedCode,
            OnVoucherClickListener listener
    ) {
        this.vouchers = vouchers;
        this.subtotal = subtotal;
        this.selectedCode = selectedCode;
        this.listener = listener;
    }

    public void setSelectedCode(@Nullable String selectedCode) {
        this.selectedCode = selectedCode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.voucher_item_card, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        VoucherModel item = vouchers.get(position);
        Context context = holder.itemView.getContext();
        boolean eligible = item.isEligible(subtotal);
        boolean isSelected = eligible && item.getCode().equalsIgnoreCase(selectedCode);

        holder.shortBadgeText.setText(item.getShortBadge());
        holder.codeText.setText(item.getCode());

        holder.expiryText.setText(VoucherService.formatExpiryAndQuotaMeta(item));

        holder.titleText.setText(item.getTitle());
        holder.descText.setText(item.getDescription());

        holder.radioButton.setChecked(isSelected);
        holder.radioButton.setEnabled(eligible);

        if (eligible) {
            holder.cardRoot.setAlpha(1.0f);
            holder.ineligibleHint.setVisibility(View.GONE);
            holder.cardRoot.setStrokeColor(ContextCompat.getColor(
                    context,
                    isSelected ? R.color.brand_accent : R.color.border_subtle
            ));
            holder.cardRoot.setStrokeWidth(Math.round((isSelected ? 1.6f : 1.0f) * context.getResources().getDisplayMetrics().density));
        } else {
            holder.cardRoot.setAlpha(0.55f);
            holder.cardRoot.setStrokeColor(ContextCompat.getColor(context, R.color.border_subtle));
            holder.cardRoot.setStrokeWidth(Math.round(1.0f * context.getResources().getDisplayMetrics().density));
            String reason = item.getIneligibleReason(subtotal);
            holder.ineligibleHint.setVisibility(View.VISIBLE);
            holder.ineligibleHint.setText(reason != null ? reason : "Mã giảm giá không khả dụng");
        }

        holder.cardRoot.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVoucherClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return vouchers != null ? vouchers.size() : 0;
    }

    static class VoucherViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final TextView shortBadgeText;
        final TextView codeText;
        final TextView expiryText;
        final TextView titleText;
        final TextView descText;
        final TextView ineligibleHint;
        final RadioButton radioButton;

        VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.voucher_card_root);
            shortBadgeText = itemView.findViewById(R.id.voucher_short_badge);
            codeText = itemView.findViewById(R.id.voucher_code);
            expiryText = itemView.findViewById(R.id.voucher_expiry);
            titleText = itemView.findViewById(R.id.voucher_title);
            descText = itemView.findViewById(R.id.voucher_desc);
            ineligibleHint = itemView.findViewById(R.id.voucher_ineligible_hint);
            radioButton = itemView.findViewById(R.id.voucher_radio);
        }
    }
}
