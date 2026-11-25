package com.example.shoes_store.core;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Lớp nền cho các Repository dùng Cloud Firestore SnapshotListener.
 * Tách biệt vòng đời listener công khai (public) và listener theo phiên người dùng (user)
 * để khi đăng xuất chỉ gỡ user listeners mà không làm mất dữ liệu công khai.
 */
public abstract class BaseFirestoreRepository implements SessionSyncCallback {

    @FunctionalInterface
    protected interface DocumentMapper<T> {
        @Nullable
        T map(@NonNull DocumentSnapshot doc);
    }

    protected final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();
    private final Map<String, ListenerRegistration> publicListeners = new HashMap<>();
    private final Map<String, ListenerRegistration> userListeners = new HashMap<>();

    @NonNull
    protected FirebaseFirestore db() {
        return provider.getFirestore();
    }

    @NonNull
    protected CollectionReference collection(@NonNull String name) {
        return db().collection(name);
    }

    @NonNull
    protected CollectionReference userSubCollection(@NonNull String uid, @NonNull String subCollection) {
        return db().collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(subCollection);
    }

    @Nullable
    protected CollectionReference currentUserSubCollection(@NonNull String subCollection) {
        String uid = provider.getCurrentUid();
        return uid != null ? userSubCollection(uid, subCollection) : null;
    }

    protected void replacePublicListener(@NonNull String key, @Nullable ListenerRegistration registration) {
        ListenerRegistration previous = publicListeners.remove(key);
        if (previous != null) {
            previous.remove();
        }
        if (registration != null) {
            publicListeners.put(key, registration);
        }
    }

    protected void clearPublicListener(@NonNull String key) {
        ListenerRegistration previous = publicListeners.remove(key);
        if (previous != null) {
            previous.remove();
        }
    }

    protected void clearPublicListeners() {
        for (ListenerRegistration reg : publicListeners.values()) {
            if (reg != null) {
                reg.remove();
            }
        }
        publicListeners.clear();
    }

    protected void replaceUserListener(@NonNull String key, @Nullable ListenerRegistration registration) {
        ListenerRegistration previous = userListeners.remove(key);
        if (previous != null) {
            previous.remove();
        }
        if (registration != null) {
            userListeners.put(key, registration);
        }
    }

    protected void clearUserListener(@NonNull String key) {
        ListenerRegistration previous = userListeners.remove(key);
        if (previous != null) {
            previous.remove();
        }
    }

    protected void clearUserListeners() {
        for (ListenerRegistration reg : userListeners.values()) {
            if (reg != null) {
                reg.remove();
            }
        }
        userListeners.clear();
    }

    protected void clearAllListeners() {
        clearUserListeners();
        clearPublicListeners();
    }

    @Override
    public void onSessionEnd() {
        clearUserListeners();
    }

    @NonNull
    protected <T> ListenerRegistration listenCollection(
            @NonNull Query query,
            @NonNull String logTag,
            @NonNull DocumentMapper<T> mapper,
            @Nullable Comparator<T> comparator,
            @NonNull Consumer<List<T>> onResult
    ) {
        return query.addSnapshotListener((snap, error) -> {
            if (error != null) {
                Log.w(logTag, "Snapshot listener error: " + error.getMessage());
            }
            if (snap == null) {
                return;
            }
            List<T> list = new ArrayList<>();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                T mapped = mapper.map(doc);
                if (mapped != null) {
                    list.add(mapped);
                }
            }
            if (comparator != null) {
                Collections.sort(list, comparator);
            }
            onResult.accept(list);
        });
    }
}
