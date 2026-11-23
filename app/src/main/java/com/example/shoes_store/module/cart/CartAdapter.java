package com.example.shoes_store.module.cart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách sản phẩm trong Giỏ hàng.
 */
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface OnCartItemActionListener {
        void onItemClick(CartItemModel item);

        void onSelectionChanged(CartItemModel item, boolean selected);

        void onQuantityDelta(CartItemModel item, int delta);

        void onRemoveClick(CartItemModel item);
    }

    private final List<CartItemModel> items = new ArrayList<>();
    private final OnCartItemActionListener listener;

    public CartAdapter(OnCartItemActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<CartItemModel> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cart_item_line, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItemModel item = items.get(position);
        holder.imageView.setImageResource(item.getProduct().getImageResId());
        holder.brandText.setText(item.getProduct().getBrand());
        holder.nameText.setText(item.getProduct().getName());
        holder.variantText.setText("EU " + item.getSize() + " · " + item.getColorName());
        holder.priceText.setText(CurrencyFormatUtil.formatVnd(item.getProduct().getPrice()));
        holder.qtyText.setText(String.valueOf(item.getQuantity()));

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(item.isSelected());
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onSelectionChanged(item, isChecked);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
        holder.plusBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuantityDelta(item, 1);
            }
        });
        holder.minusBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuantityDelta(item, -1);
            }
        });
        holder.deleteBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        final MaterialCheckBox checkBox;
        final ImageView imageView;
        final TextView brandText;
        final TextView nameText;
        final TextView variantText;
        final TextView priceText;
        final TextView qtyText;
        final ImageButton plusBtn;
        final ImageButton minusBtn;
        final ImageButton deleteBtn;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cart_item_checkbox);
            imageView = itemView.findViewById(R.id.cart_item_image);
            brandText = itemView.findViewById(R.id.cart_item_brand);
            nameText = itemView.findViewById(R.id.cart_item_name);
            variantText = itemView.findViewById(R.id.cart_item_variant);
            priceText = itemView.findViewById(R.id.cart_item_price);
            qtyText = itemView.findViewById(R.id.cart_item_qty);
            plusBtn = itemView.findViewById(R.id.cart_item_plus);
            minusBtn = itemView.findViewById(R.id.cart_item_minus);
            deleteBtn = itemView.findViewById(R.id.cart_item_delete);
        }
    }
}
