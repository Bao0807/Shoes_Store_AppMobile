package com.example.shoes_store.module.order;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseListAdapter;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.core.RemoteImageHelper;
import com.example.shoes_store.module.cart.CartItemModel;
import com.google.android.material.button.MaterialButton;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter hiển thị từng đôi giày trong Thanh toán và Chi tiết đơn hàng.
 */
public class OrderCheckoutItemAdapter extends BaseListAdapter<OrderItemModel, OrderCheckoutItemAdapter.ItemViewHolder> {

    public interface OnOrderItemInteractionListener {
        void onItemClick(@NonNull OrderItemModel item);

        void onReviewClick(@NonNull OrderItemModel item);
    }

    private final Set<String> reviewedProductIds = new HashSet<>();
    @Nullable
    private OnOrderItemInteractionListener interactionListener;
    private boolean reviewModeEnabled = false;

    public OrderCheckoutItemAdapter(List<CartItemModel> cartItems) {
        if (cartItems != null) {
            for (CartItemModel item : cartItems) {
                items.add(OrderItemModel.fromCartItem(item));
            }
        }
    }

    public static OrderCheckoutItemAdapter fromOrderItems(List<OrderItemModel> orderItems) {
        OrderCheckoutItemAdapter adapter = new OrderCheckoutItemAdapter(null);
        adapter.setInitialItems(orderItems);
        return adapter;
    }

    public void setInteractionListener(@Nullable OnOrderItemInteractionListener listener) {
        this.interactionListener = listener;
        notifyDataSetChanged();
    }

    public void setReviewMode(boolean enabled, @Nullable Set<String> reviewedIds) {
        this.reviewModeEnabled = enabled;
        reviewedProductIds.clear();
        if (reviewedIds != null) {
            reviewedProductIds.addAll(reviewedIds);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ItemViewHolder(inflate(parent, R.layout.order_item_checkout_shoe));
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        OrderItemModel item = getItem(position);
        RemoteImageHelper.bindImage(holder.imageView, item.getThumbnailUrl(), item.getFallbackImageResId());
        holder.nameText.setText(item.getProductName());
        holder.metaText.setText("EU " + item.getSize() + " · " + item.getColorName() + " · x" + item.getQuantity());
        holder.priceText.setText(CurrencyFormatUtil.formatVnd(item.getLineTotal()));

        if (interactionListener != null) {
            holder.rowContainer.setOnClickListener(v -> interactionListener.onItemClick(item));
        } else {
            holder.rowContainer.setOnClickListener(null);
        }

        if (reviewModeEnabled) {
            holder.reviewBtn.setVisibility(View.VISIBLE);
            boolean reviewed = reviewedProductIds.contains(item.getProductId());
            if (reviewed) {
                holder.reviewBtn.setEnabled(false);
                holder.reviewBtn.setText("Đã đánh giá");
                holder.reviewBtn.setAlpha(0.55f);
                holder.reviewBtn.setOnClickListener(null);
            } else {
                holder.reviewBtn.setEnabled(true);
                holder.reviewBtn.setText("Đánh giá");
                holder.reviewBtn.setAlpha(1.0f);
                holder.reviewBtn.setOnClickListener(v -> {
                    if (interactionListener != null) {
                        interactionListener.onReviewClick(item);
                    }
                });
            }
        } else {
            holder.reviewBtn.setVisibility(View.GONE);
            holder.reviewBtn.setOnClickListener(null);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        final View rowContainer;
        final ImageView imageView;
        final TextView nameText;
        final TextView metaText;
        final TextView priceText;
        final MaterialButton reviewBtn;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            rowContainer = itemView.findViewById(R.id.order_shoe_row);
            imageView = itemView.findViewById(R.id.order_shoe_image);
            nameText = itemView.findViewById(R.id.order_shoe_name);
            metaText = itemView.findViewById(R.id.order_shoe_meta);
            priceText = itemView.findViewById(R.id.order_shoe_price);
            reviewBtn = itemView.findViewById(R.id.order_shoe_review_btn);
        }
    }
}
