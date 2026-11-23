package com.example.shoes_store.module.notification;

import androidx.annotation.NonNull;

import com.example.shoes_store.core.FirestoreReadUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Chuyển đổi dữ liệu Firestore cho Thông báo (`users/{uid}/notifications`).
 */
public final class NotificationMapper {

    public static final String FIELD_NOTIFICATION_ID = "notificationId";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_TARGET_ID = "targetId";
    public static final String FIELD_UNREAD = "unread";
    public static final String FIELD_CREATED_AT = "createdAt";

    private NotificationMapper() {
    }

    @NonNull
    public static NotificationModel fromSnapshot(@NonNull DocumentSnapshot doc, @NonNull String uid) {
        return new NotificationModel(
                FirestoreReadUtil.readString(doc, FIELD_NOTIFICATION_ID, doc.getId()),
                FirestoreReadUtil.readString(doc, FIELD_USER_ID, uid),
                FirestoreReadUtil.readString(doc, FIELD_TITLE, ""),
                FirestoreReadUtil.readString(doc, FIELD_MESSAGE, ""),
                null,
                NotificationModel.Type.fromStorage(FirestoreReadUtil.readString(doc, FIELD_TYPE, "ORDER")),
                FirestoreReadUtil.readString(doc, FIELD_TARGET_ID, ""),
                FirestoreReadUtil.readMillis(doc, FIELD_CREATED_AT),
                FirestoreReadUtil.readBoolean(doc, FIELD_UNREAD, false)
        );
    }

    @NonNull
    public static Map<String, Object> toMap(
            @NonNull String notificationId,
            @NonNull String userId,
            @NonNull String title,
            @NonNull String message,
            @NonNull NotificationModel.Type type,
            @NonNull String targetId,
            @NonNull Timestamp now
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_NOTIFICATION_ID, notificationId);
        data.put(FIELD_USER_ID, userId);
        data.put(FIELD_TITLE, title);
        data.put(FIELD_MESSAGE, message);
        data.put(FIELD_TYPE, type.name());
        data.put(FIELD_TARGET_ID, targetId);
        data.put(FIELD_UNREAD, true);
        data.put(FIELD_CREATED_AT, now);
        return data;
    }
}
