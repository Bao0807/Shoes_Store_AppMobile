package com.example.shoes_store.core;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Điều phối vòng đời đồng bộ Cloud Firestore khi mở app và khi đăng nhập/đăng xuất qua {@link SessionSyncCallback}.
 */
public final class SessionSyncHelper {

    private static volatile SessionSyncHelper instance;

    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();
    private final MutableLiveData<Boolean> signedInLiveData = new MutableLiveData<>(false);
    private final List<SessionSyncCallback> callbacks = new ArrayList<>();
    private boolean initialized = false;
    @Nullable
    private String activeUid;
    private String lastAutoRoutedAdminUid = "";

    private SessionSyncHelper() {
    }

    public static SessionSyncHelper getInstance() {
        if (instance == null) {
            synchronized (SessionSyncHelper.class) {
                if (instance == null) {
                    instance = new SessionSyncHelper();
                }
            }
        }
        return instance;
    }

    public synchronized void register(@NonNull SessionSyncCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    public synchronized void init(@NonNull Context context) {
        if (initialized) {
            return;
        }
        initialized = true;

        for (SessionSyncCallback callback : callbacks) {
            callback.onAppInit();
        }

        provider.getAuth().addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            String uid = user != null ? user.getUid() : null;
            if (uid == null) {
                activeUid = null;
                lastAutoRoutedAdminUid = "";
                signedInLiveData.setValue(false);
                for (SessionSyncCallback callback : callbacks) {
                    callback.onSessionEnd();
                }
            } else if (!uid.equals(activeUid)) {
                activeUid = uid;
                signedInLiveData.setValue(true);
                for (SessionSyncCallback callback : callbacks) {
                    callback.onSessionStart(uid, user);
                }
            }
        });
    }

    public LiveData<Boolean> getSignedInLiveData() {
        return signedInLiveData;
    }

    public void markAdminAutoRouted() {
        String uid = provider.getCurrentUid();
        if (uid != null) {
            lastAutoRoutedAdminUid = uid;
        }
    }

    public boolean tryMarkAdminAutoRouted(@Nullable String uid) {
        if (uid == null || uid.isEmpty() || uid.equals(lastAutoRoutedAdminUid)) {
            return false;
        }
        lastAutoRoutedAdminUid = uid;
        return true;
    }
}
