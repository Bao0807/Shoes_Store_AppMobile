package com.example.shoes_store.module.notification;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Repository sở hữu Thông báo người dùng (`users/{uid}/notifications`) trên Cloud Firestore.
 */
public class NotificationRepository {

    private static final String TAG = "NotificationRepository";
    private static volatile NotificationRepository instance;

    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();
    private final MutableLiveData<List<NotificationModel>> notificationsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> unreadNotificationCountLiveData = new MutableLiveData<>(0);
    @Nullable
    private ListenerRegistration notificationsRegistration;

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

    // ===== 1. REALTIME =====

    public void startUserListening(@NonNull String uid) {
        stopUserListening();
        notificationsRegistration = provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_NOTIFICATIONS)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Notifications listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<NotificationModel> list = new ArrayList<>();
                    int unread = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        NotificationModel item = NotificationMapper.fromSnapshot(doc, uid);
                        list.add(item);
                        if (item.isUnread()) {
                            unread++;
                        }
                    }
                    Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()));
                    notificationsLiveData.setValue(list);
                    unreadNotificationCountLiveData.setValue(unread);
                });
    }

    public void stopUserListening() {
        if (notificationsRegistration != null) {
            notificationsRegistration.remove();
            notificationsRegistration = null;
        }
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
        CollectionReference ref = userNotificationsRef();
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
        CollectionReference ref = userNotificationsRef();
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
        WriteBatch batch = provider.getFirestore().batch();
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

    // ===== 4. PRIVATE HELPERS =====

    @Nullable
    private CollectionReference userNotificationsRef() {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            return null;
        }
        return provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_NOTIFICATIONS);
    }
}
