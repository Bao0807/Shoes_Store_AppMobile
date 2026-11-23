package com.example.shoes_store.module.notification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreStoreHelper;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

/**
 * Repository cho màn Thông báo trên Cloud Firestore.
 */
public class NotificationRepository {

    private final FirestoreStoreHelper store = FirestoreStoreHelper.getInstance();
    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();

    public LiveData<List<NotificationModel>> getNotificationsLiveData() {
        return store.getNotificationsLiveData();
    }

    public void markRead(@NonNull String id) {
        CollectionReference ref = userNotificationsRef();
        if (ref == null || id.isEmpty()) {
            return;
        }
        ref.document(id).update("unread", false);
    }

    public void markAllRead() {
        CollectionReference ref = userNotificationsRef();
        List<NotificationModel> list = store.getNotificationsLiveData().getValue();
        if (ref == null || list == null || list.isEmpty()) {
            return;
        }
        WriteBatch batch = provider.getFirestore().batch();
        boolean hasUnread = false;
        for (NotificationModel item : list) {
            if (item.isUnread()) {
                batch.update(ref.document(item.getId()), "unread", false);
                hasUnread = true;
            }
        }
        if (hasUnread) {
            batch.commit();
        }
    }

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
