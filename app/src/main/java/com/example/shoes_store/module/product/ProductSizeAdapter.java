package com.example.shoes_store.module.product;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;

import java.util.List;
import java.util.Set;

/**
 * Adapter chọn size EU cho giày chạy bộ (hỗ trợ trạng thái hết hàng).
 */
public class ProductSizeAdapter extends RecyclerView.Adapter<ProductSizeAdapter.SizeViewHolder> {

    public interface OnSizeSelectedListener {
        void onSizeSelected(String size);
    }

    private final List<String> sizes;
    private final Set<String> outOfStockSizes;
    private final OnSizeSelectedListener listener;
    private String selectedSize;

    public ProductSizeAdapter(
            List<String> sizes,
            Set<String> outOfStockSizes,
            String initialSelected,
            OnSizeSelectedListener listener
    ) {
        this.sizes = sizes;
        this.outOfStockSizes = outOfStockSizes;
        this.selectedSize = initialSelected;
        this.listener = listener;
    }

    public String getSelectedSize() {
        return selectedSize;
    }

    @NonNull
    @Override
    public SizeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.product_item_size, parent, false);
        return new SizeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SizeViewHolder holder, int position) {
        String size = sizes.get(position);
        boolean disabled = outOfStockSizes != null && outOfStockSizes.contains(size);
        boolean selected = !disabled && size.equals(selectedSize);

        holder.sizeLabel.setText(size);
        if (disabled) {
            holder.sizeLabel.setBackgroundResource(R.drawable.bg_size_disabled);
            holder.sizeLabel.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_tertiary));
            holder.itemView.setEnabled(false);
        } else if (selected) {
            holder.sizeLabel.setBackgroundResource(R.drawable.bg_size_selected);
            holder.sizeLabel.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_on_dark));
            holder.itemView.setEnabled(true);
        } else {
            holder.sizeLabel.setBackgroundResource(R.drawable.bg_size_unselected);
            holder.sizeLabel.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
            holder.itemView.setEnabled(true);
        }

        holder.itemView.setOnClickListener(v -> {
            if (disabled) {
                return;
            }
            selectedSize = size;
            notifyDataSetChanged();
            if (listener != null) {
                listener.onSizeSelected(size);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sizes.size();
    }

    static class SizeViewHolder extends RecyclerView.ViewHolder {
        final TextView sizeLabel;

        SizeViewHolder(@NonNull View itemView) {
            super(itemView);
            sizeLabel = itemView.findViewById(R.id.size_label);
        }
    }
}
