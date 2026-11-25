package com.example.shoes_store.module.notification;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseListAdapter;
import com.example.shoes_store.core.StatusBadgeHelper;

/**
 * Adapter hiển thị danh sách thông báo đơn hàng, ưu đãi và sản phẩm mới.
 */
public class NotificationAdapter extends BaseListAdapter<NotificationModel, NotificationAdapter.NotifViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationModel item);
    }

    private final OnNotificationClickListener listener;

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NotifViewHolder(inflate(parent, R.layout.notification_item_row));
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
        NotificationModel item = getItem(position);
        Context context = holder.itemView.getContext();

        holder.titleText.setText(item.getTitle());
        holder.messageText.setText(item.getMessage());
        holder.timeText.setText(item.getTimeAgo());
        holder.unreadDot.setVisibility(item.isUnread() ? View.VISIBLE : View.GONE);

        int bgRes = item.isUnread() ? R.color.brand_accent_soft : R.color.bg_screen;
        StatusBadgeHelper.applyBackgroundTint(holder.rootView, bgRes);

        int iconRes;
        int iconTint;
        int badgeTint;
        if (item.getType() == NotificationModel.Type.ORDER) {
            iconRes = R.drawable.ic_notification_order;
            iconTint = R.color.status_info;
            badgeTint = R.color.status_info_soft;
        } else if (item.getType() == NotificationModel.Type.DROP) {
            iconRes = R.drawable.ic_notification_shoe;
            iconTint = R.color.status_success;
            badgeTint = R.color.status_success_soft;
        } else {
            iconRes = R.drawable.ic_notification_promo;
            iconTint = R.color.brand_accent;
            badgeTint = R.color.brand_accent_soft;
        }
        holder.iconView.setImageResource(iconRes);
        holder.iconView.setColorFilter(ContextCompat.getColor(context, iconTint));
        StatusBadgeHelper.applyBackgroundTint(holder.iconBg, badgeTint);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(item);
            }
        });
    }

    static class NotifViewHolder extends RecyclerView.ViewHolder {
        final View rootView;
        final View iconBg;
        final ImageView iconView;
        final TextView titleText;
        final TextView messageText;
        final TextView timeText;
        final View unreadDot;

        NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            rootView = itemView.findViewById(R.id.notif_item_root);
            iconBg = itemView.findViewById(R.id.notif_icon_bg);
            iconView = itemView.findViewById(R.id.notif_icon);
            titleText = itemView.findViewById(R.id.notif_item_title);
            messageText = itemView.findViewById(R.id.notif_item_message);
            timeText = itemView.findViewById(R.id.notif_item_time);
            unreadDot = itemView.findViewById(R.id.notif_unread_dot);
        }
    }
}
