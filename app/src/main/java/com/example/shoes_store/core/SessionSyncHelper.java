package com.example.shoes_store.core;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.module.cart.CartRepository;
import com.example.shoes_store.module.home.HomeRepository;
import com.example.shoes_store.module.notification.NotificationRepository;
import com.example.shoes_store.module.order.OrderRepository;
import com.example.shoes_store.module.product.ProductRepository;
import com.example.shoes_store.module.profile.ProfileRepository;
import com.example.shoes_store.module.voucher.VoucherRepository;
import com.example.shoes_store.module.wishlist.WishlistRepository;
import com.google.firebase.auth.FirebaseUser;

/**
 * Điều phối vòng đời đồng bộ Cloud Firestore khi mở app và khi đăng nhập/đăng xuất.
 */
public final class SessionSyncHelper {

    private static volatile SessionSyncHelper instance;

    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();
    private final MutableLiveData<Boolean> signedInLiveData = new MutableLiveData<>(false);
    private boolean initialized = false;
    @Nullable
    private String activeUid;

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

    public synchronized void init(@NonNull Context context) {
        if (initialized) {
            return;
        }
        initialized = true;
        AppPrefsHelper.init(context);

        HomeRepository.getInstance().initLocalSearches();
        HomeRepository.getInstance().startPublicListening();
        ProductRepository.getInstance().startPublicListening();
        VoucherRepository.getInstance().startPublicListening();

        provider.getAuth().addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            String uid = user != null ? user.getUid() : null;
            if (uid == null) {
                activeUid = null;
                signedInLiveData.setValue(false);
                stopAllUserListeners();
            } else if (!uid.equals(activeUid)) {
                activeUid = uid;
                signedInLiveData.setValue(true);
                startAllUserListeners(uid, user);
                ProfileRepository.getInstance().ensureUserDocumentExists(uid, user);
                ProductRepository.getInstance().reconcileAllRatingsOnSignIn();
            }
        });
    }

    public LiveData<Boolean> getSignedInLiveData() {
        return signedInLiveData;
    }

    private void startAllUserListeners(@NonNull String uid, @NonNull FirebaseUser authUser) {
        ProfileRepository.getInstance().startUserListening(uid, authUser);
        WishlistRepository.getInstance().startUserListening(uid);
        CartRepository.getInstance().startUserListening(uid);
        NotificationRepository.getInstance().startUserListening(uid);
        OrderRepository.getInstance().startUserListening(uid);
    }

    private void stopAllUserListeners() {
        ProfileRepository.getInstance().stopUserListening();
        WishlistRepository.getInstance().stopUserListening();
        CartRepository.getInstance().stopUserListening();
        NotificationRepository.getInstance().stopUserListening();
        OrderRepository.getInstance().stopUserListening();
        HomeRepository.getInstance().resetRecentSearchesFromLocal();
    }
}
