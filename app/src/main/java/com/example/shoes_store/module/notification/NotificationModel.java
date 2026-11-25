package com.example.shoes_store.module.notification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.core.DateFormatUtil;

/**
 * Model thông báo đơn hàng, ưu đãi và giày mới ra mắt.
 */
public class NotificationModel {

    public enum Type {
        ORDER,
        PROMO,
        DROP;

        @NonNull
        public static Type fromStorage(@Nullable String value) {
            if (value == null) {
                return ORDER;
            }
            for (Type t : values()) {
                if (t.name().equalsIgnoreCase(value)) {
                    return t;
                }
            }
            return ORDER;
        }
    }

    private final String id;
    private final String title;
    private final String message;
    private final String timeAgo;
    private final Type type;
    private final String targetId;
    private final long createdAtMillis;
    private final boolean unread;

    public NotificationModel(
            @Nullable String id,
            @Nullable String title,
            @Nullable String message,
            @Nullable String timeAgo,
            @NonNull Type type,
            @Nullable String targetId,
            long createdAtMillis,
            boolean unread
    ) {
        this.id = id != null ? id : "";
        this.title = title != null ? title : "";
        this.message = message != null ? message : "";
        this.type = type;
        this.targetId = targetId != null ? targetId : "";
        this.createdAtMillis = createdAtMillis;
        this.unread = unread;
        if (timeAgo != null && !timeAgo.trim().isEmpty()) {
            this.timeAgo = timeAgo;
        } else {
            this.timeAgo = formatRelativeTime(createdAtMillis);
        }
    }

    @NonNull
    public static String formatRelativeTime(long timestampMillis) {
        if (timestampMillis <= 0L) {
            return "Vừa xong";
        }
        long diffMs = Math.max(0L, System.currentTimeMillis() - timestampMillis);
        long minutes = diffMs / 60_000L;
        if (minutes < 1L) {
            return "Vừa xong";
        }
        if (minutes < 60L) {
            return minutes + " phút trước";
        }
        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours + " giờ trước";
        }
        long days = hours / 24L;
        if (days < 7L) {
            return days + " ngày trước";
        }
        return DateFormatUtil.formatDate(timestampMillis);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public Type getType() {
        return type;
    }

    @NonNull
    public String getTargetId() {
        return targetId;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public boolean isUnread() {
        return unread;
    }
}
