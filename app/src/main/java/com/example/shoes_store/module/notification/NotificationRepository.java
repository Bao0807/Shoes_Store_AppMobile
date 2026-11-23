package com.example.shoes_store.module.notification;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.shoes_store.core.MockStoreHelper;

import java.util.List;

/**
 * Repository cho màn Thông báo.
 */
public class NotificationRepository {

    private final MockStoreHelper store = MockStoreHelper.getInstance();

    public LiveData<List<NotificationModel>> getNotificationsLiveData() {
        return store.getNotificationsLiveData();
    }

    public void markRead(@NonNull String id) {
        store.markNotificationRead(id);
    }

    public void markAllRead() {
        store.markAllNotificationsRead();
    }
}
