package com.example.shoes_store.module.order;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseListAdapter;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.core.RemoteImageHelper;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Adapter danh sách đơn hàng theo trạng thái.
 */
public class OrderHistoryAdapter extends BaseListAdapter<OrderModel, OrderHistoryAdapter.OrderViewHolder> {

    public interface OnOrderActionListener {
        void onOrderClick(OrderModel order);

        void onOrderActionClick(OrderModel order);
    }

    private final OnOrderActionListener listener;

    public OrderHistoryAdapter(OnOrderActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OrderViewHolder(inflate(parent, R.layout.order_item_history));
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderModel order = getItem(position);

        holder.codeText.setText("Mã đơn #" + order.getOrderCode());
        holder.dateText.setText(order.getCreatedAt());
        holder.itemCountText.setText(order.getTotalQuantity() + " sản phẩm");
        holder.totalText.setText(CurrencyFormatUtil.formatVnd(order.getTotal()));

        List<OrderItemModel> orderItems = order.getOrderItems();
        if (!orderItems.isEmpty()) {
            OrderItemModel first = orderItems.get(0);
            RemoteImageHelper.bindImage(holder.shoeImage, first.getThumbnailUrl(), first.getFallbackImageResId());
            holder.shoeName.setText(first.getProductName());
            String extra = orderItems.size() > 1 ? " (+" + (orderItems.size() - 1) + " mẫu khác)" : "";
            holder.shoeMeta.setText("EU " + first.getSize() + " · " + first.getColorName() + " · x" + first.getQuantity() + extra);
        }

        OrderStatusHelper.applyStatusBadge(holder.statusBadge, order.getStatus());

        if (order.getStatus() == OrderModel.Status.PENDING) {
            holder.actionBtn.setVisibility(View.VISIBLE);
            holder.actionBtn.setText("Hủy đơn");
        } else if (order.getStatus() == OrderModel.Status.COMPLETED || order.getStatus() == OrderModel.Status.CANCELLED) {
            holder.actionBtn.setVisibility(View.VISIBLE);
            holder.actionBtn.setText("Mua lại");
        } else {
            holder.actionBtn.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
        });
        holder.actionBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderActionClick(order);
            }
        });
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        final TextView codeText;
        final TextView statusBadge;
        final ImageView shoeImage;
        final TextView shoeName;
        final TextView shoeMeta;
        final TextView dateText;
        final TextView itemCountText;
        final TextView totalText;
        final MaterialButton actionBtn;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            codeText = itemView.findViewById(R.id.history_order_code);
            statusBadge = itemView.findViewById(R.id.history_order_status);
            shoeImage = itemView.findViewById(R.id.history_shoe_image);
            shoeName = itemView.findViewById(R.id.history_shoe_name);
            shoeMeta = itemView.findViewById(R.id.history_shoe_meta);
            dateText = itemView.findViewById(R.id.history_order_date);
            itemCountText = itemView.findViewById(R.id.history_item_count);
            totalText = itemView.findViewById(R.id.history_total);
            actionBtn = itemView.findViewById(R.id.history_action_btn);
        }
    }
}
