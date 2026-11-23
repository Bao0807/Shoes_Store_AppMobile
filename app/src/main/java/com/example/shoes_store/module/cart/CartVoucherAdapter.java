package com.example.shoes_store.module.cart;

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
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * Adapter hiển thị danh sách voucher trong BottomSheet chọn mã giảm giá.
 */
public class CartVoucherAdapter extends RecyclerView.Adapter<CartVoucherAdapter.VoucherViewHolder> {

    public interface OnVoucherClickListener {
        void onVoucherClick(CartVoucherModel voucher);
    }

    private final List<CartVoucherModel> vouchers;
    private final long subtotal;
    @Nullable
    private String selectedCode;
    private final OnVoucherClickListener listener;

    public CartVoucherAdapter(
            List<CartVoucherModel> vouchers,
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
                .inflate(R.layout.cart_item_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        CartVoucherModel item = vouchers.get(position);
        Context context = holder.itemView.getContext();
        boolean eligible = item.isEligible(subtotal);
        boolean isSelected = eligible && item.getCode().equalsIgnoreCase(selectedCode);

        holder.shortBadgeText.setText(item.getShortBadge());
        holder.codeText.setText(item.getCode());
        holder.expiryText.setText("HSD: " + item.getExpiryDate());
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
            long missing = Math.max(0L, item.getMinOrderAmount() - subtotal);
            holder.ineligibleHint.setVisibility(View.VISIBLE);
            holder.ineligibleHint.setText("Mua thêm " + CurrencyFormatUtil.formatVnd(missing) + " để dùng mã này");
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
