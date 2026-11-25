package com.example.shoes_store.module.notification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.core.ResourceCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel cho tab Thông báo.
 */
public class NotificationViewModel extends ViewModel {

    public enum Filter {
        ALL,
        ORDER,
        PROMO
    }

    private final NotificationRepository repository = NotificationRepository.getInstance();
    private Filter activeFilter = Filter.ALL;

    public LiveData<List<NotificationModel>> getNotificationsLiveData() {
        return repository.getNotificationsLiveData();
    }

    public void setFilter(@NonNull Filter filter) {
        this.activeFilter = filter;
    }

    public List<NotificationModel> filterList(List<NotificationModel> source) {
        List<NotificationModel> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (NotificationModel item : source) {
            if (activeFilter == Filter.ORDER && item.getType() != NotificationModel.Type.ORDER) {
                continue;
            }
            if (activeFilter == Filter.PROMO && item.getType() == NotificationModel.Type.ORDER) {
                continue;
            }
            result.add(item);
        }
        return result;
    }

    public void markRead(@NonNull String id, @Nullable ResourceCallback<Void> callback) {
        repository.markRead(id, callback);
    }

    public void markAllRead(@Nullable ResourceCallback<Void> callback) {
        repository.markAllRead(callback);
    }
}
