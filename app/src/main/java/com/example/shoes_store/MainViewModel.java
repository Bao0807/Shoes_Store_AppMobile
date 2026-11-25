package com.example.shoes_store;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.SessionSyncHelper;
import com.example.shoes_store.module.cart.CartRepository;
import com.example.shoes_store.module.notification.NotificationRepository;
import com.example.shoes_store.module.profile.ProfileModel;
import com.example.shoes_store.module.profile.ProfileRepository;

/**
 * ViewModel cho khung điều hướng chính {@link MainActivity}.
 */
public class MainViewModel extends ViewModel {

    private final CartRepository cartRepository;
    private final NotificationRepository notificationRepository;
    private final ProfileRepository profileRepository;
    private final SessionSyncHelper sessionSyncHelper;
    private final FirebaseProviderHelper firebaseProvider;

    public MainViewModel() {
        this(
                CartRepository.getInstance(),
                NotificationRepository.getInstance(),
                ProfileRepository.getInstance(),
                SessionSyncHelper.getInstance(),
                FirebaseProviderHelper.getInstance()
        );
    }

    public MainViewModel(
            @NonNull CartRepository cartRepository,
            @NonNull NotificationRepository notificationRepository,
            @NonNull ProfileRepository profileRepository,
            @NonNull SessionSyncHelper sessionSyncHelper,
            @NonNull FirebaseProviderHelper firebaseProvider
    ) {
        this.cartRepository = cartRepository;
        this.notificationRepository = notificationRepository;
        this.profileRepository = profileRepository;
        this.sessionSyncHelper = sessionSyncHelper;
        this.firebaseProvider = firebaseProvider;
    }

    public LiveData<Integer> getCartTotalCountLiveData() {
        return cartRepository.getCartTotalCountLiveData();
    }

    public LiveData<Integer> getUnreadNotificationCountLiveData() {
        return notificationRepository.getUnreadNotificationCountLiveData();
    }

    public LiveData<Boolean> getSignedInLiveData() {
        return sessionSyncHelper.getSignedInLiveData();
    }

    public LiveData<ProfileModel> getProfileLiveData() {
        return profileRepository.getProfileLiveData();
    }

    public boolean isSignedIn() {
        return firebaseProvider.isSignedIn();
    }

    public boolean tryMarkAdminAutoRouted(@Nullable String uid) {
        return sessionSyncHelper.tryMarkAdminAutoRouted(uid);
    }
}
