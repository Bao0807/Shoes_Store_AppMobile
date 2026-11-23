package com.example.shoes_store.module.notification;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

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

    private final NotificationRepository repository = new NotificationRepository();
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

    public void markRead(@NonNull String id) {
        repository.markRead(id);
    }

    public void markAllRead() {
        repository.markAllRead();
    }
}
