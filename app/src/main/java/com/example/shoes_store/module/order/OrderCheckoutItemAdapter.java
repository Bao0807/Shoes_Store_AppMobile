package com.example.shoes_store.module.order;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.module.cart.CartItemModel;

import java.util.List;

/**
 * Adapter rút gọn hiển thị từng đôi giày trong Thanh toán và Chi tiết đơn hàng.
 */
public class OrderCheckoutItemAdapter extends RecyclerView.Adapter<OrderCheckoutItemAdapter.ItemViewHolder> {

    private final List<CartItemModel> items;

    public OrderCheckoutItemAdapter(List<CartItemModel> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.order_item_checkout_shoe, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        CartItemModel item = items.get(position);
        holder.imageView.setImageResource(item.getProduct().getImageResId());
        holder.nameText.setText(item.getProduct().getBrand() + " " + item.getProduct().getName());
        holder.metaText.setText("EU " + item.getSize() + " · " + item.getColorName() + " · x" + item.getQuantity());
        holder.priceText.setText(CurrencyFormatUtil.formatVnd(item.getLineTotal()));
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final TextView nameText;
        final TextView metaText;
        final TextView priceText;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.order_shoe_image);
            nameText = itemView.findViewById(R.id.order_shoe_name);
            metaText = itemView.findViewById(R.id.order_shoe_meta);
            priceText = itemView.findViewById(R.id.order_shoe_price);
        }
    }
}
