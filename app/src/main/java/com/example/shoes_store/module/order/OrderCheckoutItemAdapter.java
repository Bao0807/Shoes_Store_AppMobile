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
import com.example.shoes_store.core.RemoteImageHelper;
import com.example.shoes_store.module.cart.CartItemModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị từng đôi giày trong Thanh toán và Chi tiết đơn hàng.
 */
public class OrderCheckoutItemAdapter extends RecyclerView.Adapter<OrderCheckoutItemAdapter.ItemViewHolder> {

    private final List<OrderItemModel> items = new ArrayList<>();

    public OrderCheckoutItemAdapter(List<CartItemModel> cartItems) {
        if (cartItems != null) {
            for (CartItemModel item : cartItems) {
                items.add(OrderItemModel.fromCartItem(item));
            }
        }
    }

    public static OrderCheckoutItemAdapter fromOrderItems(List<OrderItemModel> orderItems) {
        OrderCheckoutItemAdapter adapter = new OrderCheckoutItemAdapter(null);
        if (orderItems != null) {
            adapter.items.addAll(orderItems);
        }
        return adapter;
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
        OrderItemModel item = items.get(position);
        RemoteImageHelper.bindImage(holder.imageView, item.getThumbnailUrl(), item.getFallbackImageResId());
        holder.nameText.setText(item.getProductName());
        holder.metaText.setText("EU " + item.getSize() + " · " + item.getColorName() + " · x" + item.getQuantity());
        holder.priceText.setText(CurrencyFormatUtil.formatVnd(item.getLineTotal()));
    }

    @Override
    public int getItemCount() {
        return items.size();
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
