package com.example.shoes_store.module.notification;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.BaseFirestoreRepository;
import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Repository sở hữu Thông báo người dùng (`users/{uid}/notifications`) trên Cloud Firestore.
 */
public class NotificationRepository extends BaseFirestoreRepository {

    private static final String TAG = "NotificationRepository";
    private static final String LISTENER_NOTIFICATIONS = "notifications";
    private static volatile NotificationRepository instance;

    private final MutableLiveData<List<NotificationModel>> notificationsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> unreadNotificationCountLiveData = new MutableLiveData<>(0);

    private NotificationRepository() {
    }

    public static NotificationRepository getInstance() {
        if (instance == null) {
            synchronized (NotificationRepository.class) {
                if (instance == null) {
                    instance = new NotificationRepository();
                }
            }
        }
        return instance;
    }

    @Override
    public void onSessionStart(@NonNull String uid, @NonNull FirebaseUser authUser) {
        startUserListening(uid);
    }

    @Override
    public void onSessionEnd() {
        stopUserListening();
    }

    // ===== 1. REALTIME =====

    public void startUserListening(@NonNull String uid) {
        stopUserListening();
        replaceUserListener(LISTENER_NOTIFICATIONS, listenCollection(
                userSubCollection(uid, FirebaseProviderHelper.SUB_NOTIFICATIONS),
                TAG,
                NotificationMapper::fromSnapshot,
                (a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()),
                list -> {
                    int unread = 0;
                    for (NotificationModel item : list) {
                        if (item.isUnread()) {
                            unread++;
                        }
                    }
                    notificationsLiveData.setValue(list);
                    unreadNotificationCountLiveData.setValue(unread);
                }
        ));
    }

    public void stopUserListening() {
        clearUserListeners();
        notificationsLiveData.setValue(new ArrayList<>());
        unreadNotificationCountLiveData.setValue(0);
    }

    // ===== 2. ĐỌC =====

    public LiveData<List<NotificationModel>> getNotificationsLiveData() {
        return notificationsLiveData;
    }

    public LiveData<Integer> getUnreadNotificationCountLiveData() {
        return unreadNotificationCountLiveData;
    }

    // ===== 3. GHI =====

    public void markRead(@NonNull String id, @Nullable ResourceCallback<Void> callback) {
        CollectionReference ref = currentUserSubCollection(FirebaseProviderHelper.SUB_NOTIFICATIONS);
        if (ref == null || id.isEmpty()) {
            return;
        }
        ref.document(id).update(NotificationMapper.FIELD_UNREAD, false)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Mark read failed: " + e.getMessage());
                    if (callback != null) {
                        callback.onError("Không thể cập nhật trạng thái thông báo");
                    }
                });
    }

    public void markAllRead(@Nullable ResourceCallback<Void> callback) {
        CollectionReference ref = currentUserSubCollection(FirebaseProviderHelper.SUB_NOTIFICATIONS);
        List<NotificationModel> list = notificationsLiveData.getValue();
        if (ref == null) {
            if (callback != null) {
                callback.onError("Vui lòng đăng nhập");
            }
            return;
        }
        if (list == null || list.isEmpty()) {
            if (callback != null) {
                callback.onSuccess(null);
            }
            return;
        }
        WriteBatch batch = db().batch();
        boolean hasUnread = false;
        for (NotificationModel item : list) {
            if (item.isUnread()) {
                batch.update(ref.document(item.getId()), NotificationMapper.FIELD_UNREAD, false);
                hasUnread = true;
            }
        }
        if (!hasUnread) {
            if (callback != null) {
                callback.onSuccess(null);
            }
            return;
        }
        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError("Không thể đánh dấu tất cả thông báo");
                    }
                });
    }

    @NonNull
    public static Map<String, Object> buildOrderNotificationData(
            @NonNull String notificationId,
            @NonNull String uid,
            @NonNull String title,
            @NonNull String message,
            @NonNull String orderCode,
            @NonNull Timestamp now
    ) {
        return NotificationMapper.toMap(
                notificationId, uid, title, message, NotificationModel.Type.ORDER, orderCode, now
        );
    }
}
