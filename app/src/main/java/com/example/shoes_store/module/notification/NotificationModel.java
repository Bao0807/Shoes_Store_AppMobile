package com.example.shoes_store.module.notification;

/**
 * Model thông báo đơn hàng, ưu đãi và giày mới ra mắt.
 */
public class NotificationModel {

    public enum Type {
        ORDER,
        PROMO,
        DROP
    }

    private final String id;
    private final String title;
    private final String message;
    private final String timeAgo;
    private final Type type;
    private boolean unread;

    public NotificationModel(
            String id,
            String title,
            String message,
            String timeAgo,
            Type type,
            boolean unread
    ) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timeAgo = timeAgo;
        this.type = type;
        this.unread = unread;
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

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }
}
