package com.example.shoes_store.module.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.core.ResourceCallback;
import com.google.firebase.auth.FirebaseUser;

/**
 * ViewModel cho màn hình Đăng nhập và Đăng ký.
 */
public class AuthViewModel extends ViewModel {

    private final AuthRepository repository = new AuthRepository();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    public void login(
            @NonNull String email,
            @NonNull String password,
            @NonNull ResourceCallback<FirebaseUser> callback
    ) {
        loadingLiveData.setValue(true);
        repository.login(email, password, new ResourceCallback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser data) {
                loadingLiveData.setValue(false);
                callback.onSuccess(data);
            }

            @Override
            public void onError(String message) {
                loadingLiveData.setValue(false);
                callback.onError(message);
            }
        });
    }

    public void register(
            @NonNull String fullName,
            @NonNull String phone,
            @NonNull String email,
            @NonNull String password,
            @NonNull ResourceCallback<FirebaseUser> callback
    ) {
        loadingLiveData.setValue(true);
        repository.register(fullName, phone, email, password, new ResourceCallback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser data) {
                loadingLiveData.setValue(false);
                callback.onSuccess(data);
            }

            @Override
            public void onError(String message) {
                loadingLiveData.setValue(false);
                callback.onError(message);
            }
        });
    }

    public void sendPasswordReset(@NonNull String email, @NonNull ResourceCallback<Void> callback) {
        loadingLiveData.setValue(true);
        repository.sendPasswordReset(email, new ResourceCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                loadingLiveData.setValue(false);
                callback.onSuccess(data);
            }

            @Override
            public void onError(String message) {
                loadingLiveData.setValue(false);
                callback.onError(message);
            }
        });
    }

    public void logout() {
        repository.logout();
    }
}
