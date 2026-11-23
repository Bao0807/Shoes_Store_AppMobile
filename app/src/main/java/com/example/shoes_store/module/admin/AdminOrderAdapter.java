package com.example.shoes_store.module.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.module.order.OrderHistoryAdapter;
import com.example.shoes_store.module.order.OrderItemModel;
import com.example.shoes_store.module.order.OrderModel;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách toàn bộ đơn hàng cho Admin kèm nút duyệt / giao / hủy theo trạng thái.
 */
public class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.OrderViewHolder> {

    public interface OnAdminOrderActionListener {
        void onOrderCardClick(@NonNull OrderModel order);

        void onAdvanceStatus(@NonNull OrderModel order, @NonNull OrderModel.Status nextStatus);

        void onCancelOrder(@NonNull OrderModel order);
    }

    private final List<OrderModel> orders = new ArrayList<>();
    private final OnAdminOrderActionListener listener;

    public AdminOrderAdapter(@NonNull OnAdminOrderActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<OrderModel> list) {
        orders.clear();
        if (list != null) {
            orders.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderModel order = orders.get(position);
        Context context = holder.itemView.getContext();

        holder.codeText.setText("Mã đơn #" + order.getOrderCode());
        holder.dateText.setText("Đặt lúc " + order.getCreatedAt());
        OrderHistoryAdapter.applyStatusStyle(holder.statusBadge, order.getStatus(), context);

        holder.receiverText.setText(order.getReceiverInfo());
        holder.addressText.setText(order.getAddressSummary());

        StringBuilder itemsBuilder = new StringBuilder();
        List<OrderItemModel> orderItems = order.getOrderItems();
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItemModel item = orderItems.get(i);
            if (i > 0) {
                itemsBuilder.append("\n");
            }
            itemsBuilder.append("• ")
                    .append(item.getProductName())
                    .append(" (EU ").append(item.getSize())
                    .append(" · ").append(item.getColorName())
                    .append(") x").append(item.getQuantity());
        }
        holder.itemsSummaryText.setText(itemsBuilder.length() > 0 ? itemsBuilder.toString() : "Không có chi tiết sản phẩm");

        String voucherNote = !order.getVoucherCode().isEmpty() ? " · Mã: " + order.getVoucherCode() : "";
        holder.paymentMetaText.setText(order.getShippingMethod() + " · " + order.getPaymentMethod() + voucherNote);
        holder.totalText.setText(CurrencyFormatUtil.formatVnd(order.getTotal()));

        if (order.getStatus() == OrderModel.Status.PENDING) {
            holder.actionsRow.setVisibility(View.VISIBLE);
            holder.cancelBtn.setVisibility(View.VISIBLE);
            holder.primaryBtn.setVisibility(View.VISIBLE);
            holder.primaryBtn.setText("Xác nhận & Giao");
            holder.cancelBtn.setOnClickListener(v -> listener.onCancelOrder(order));
            holder.primaryBtn.setOnClickListener(v -> listener.onAdvanceStatus(order, OrderModel.Status.SHIPPING));
        } else if (order.getStatus() == OrderModel.Status.SHIPPING) {
            holder.actionsRow.setVisibility(View.VISIBLE);
            holder.cancelBtn.setVisibility(View.GONE);
            holder.primaryBtn.setVisibility(View.VISIBLE);
            holder.primaryBtn.setText("Xác nhận Đã giao");
            holder.primaryBtn.setOnClickListener(v -> listener.onAdvanceStatus(order, OrderModel.Status.COMPLETED));
        } else {
            holder.actionsRow.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onOrderCardClick(order));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        final TextView codeText;
        final TextView dateText;
        final TextView statusBadge;
        final TextView receiverText;
        final TextView addressText;
        final TextView itemsSummaryText;
        final TextView paymentMetaText;
        final TextView totalText;
        final View actionsRow;
        final MaterialButton cancelBtn;
        final MaterialButton primaryBtn;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            codeText = itemView.findViewById(R.id.admin_order_code);
            dateText = itemView.findViewById(R.id.admin_order_date);
            statusBadge = itemView.findViewById(R.id.admin_order_status_badge);
            receiverText = itemView.findViewById(R.id.admin_order_receiver);
            addressText = itemView.findViewById(R.id.admin_order_address);
            itemsSummaryText = itemView.findViewById(R.id.admin_order_items_summary);
            paymentMetaText = itemView.findViewById(R.id.admin_order_payment_meta);
            totalText = itemView.findViewById(R.id.admin_order_total);
            actionsRow = itemView.findViewById(R.id.admin_order_actions_row);
            cancelBtn = itemView.findViewById(R.id.admin_order_cancel_btn);
            primaryBtn = itemView.findViewById(R.id.admin_order_primary_btn);
        }
    }
}
