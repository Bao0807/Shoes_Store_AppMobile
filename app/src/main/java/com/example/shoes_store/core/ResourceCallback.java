package com.example.shoes_store.core;

/**
 * Callback chuẩn cho các thao tác bất đồng bộ Firebase.
 */
public interface ResourceCallback<T> {
    void onSuccess(T data);

    void onError(String message);
}
