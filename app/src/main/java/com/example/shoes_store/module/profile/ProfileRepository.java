package com.example.shoes_store.module.profile;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.AppPrefsHelper;
import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.home.HomeRepository;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.order.OrderRepository;
import com.example.shoes_store.module.voucher.VoucherModel;
import com.example.shoes_store.module.voucher.VoucherRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Repository sở hữu Hồ sơ runner (`users/{uid}`) và Sổ địa chỉ (`users/{uid}/addresses`).
 */
public class ProfileRepository {

    private static final String TAG = "ProfileRepository";
    private static volatile ProfileRepository instance;

    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();
    private final MutableLiveData<ProfileModel> profileLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ProfileAddressModel>> addressesLiveData = new MutableLiveData<>(new ArrayList<>());
    @Nullable
    private ListenerRegistration profileRegistration;
    @Nullable
    private ListenerRegistration addressesRegistration;

    private ProfileRepository() {
    }

    public static ProfileRepository getInstance() {
        if (instance == null) {
            synchronized (ProfileRepository.class) {
                if (instance == null) {
                    instance = new ProfileRepository();
                }
            }
        }
        return instance;
    }

    // ===== 1. REALTIME =====

    public void startUserListening(@NonNull String uid, @NonNull FirebaseUser authUser) {
        stopUserListening();
        FirebaseFirestore db = provider.getFirestore();

        profileRegistration = db.collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        Log.w(TAG, "User profile listener error: " + error.getMessage());
                    }
                    if (doc == null || !doc.exists()) {
                        profileLiveData.setValue(ProfileMapper.defaultProfile(uid, authUser));
                        return;
                    }
                    profileLiveData.setValue(ProfileMapper.profileFromSnapshot(doc, uid));
                    List<String> remoteSearches = FirestoreReadUtil.readStringList(doc, ProfileMapper.FIELD_RECENT_SEARCHES);
                    if (!remoteSearches.isEmpty()) {
                        HomeRepository.getInstance().applyRemoteRecentSearches(remoteSearches);
                    }
                });

        addressesRegistration = db.collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_ADDRESSES)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Addresses listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<ProfileAddressModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        list.add(ProfileMapper.addressFromSnapshot(doc, uid));
                    }
                    Collections.sort(list, (a, b) -> Boolean.compare(b.isDefaultAddress(), a.isDefaultAddress()));
                    addressesLiveData.setValue(list);
                });
    }

    public void stopUserListening() {
        if (profileRegistration != null) {
            profileRegistration.remove();
            profileRegistration = null;
        }
        if (addressesRegistration != null) {
            addressesRegistration.remove();
            addressesRegistration = null;
        }
        profileLiveData.setValue(null);
        addressesLiveData.setValue(new ArrayList<>());
    }

    public void ensureUserDocumentExists(@NonNull String uid, @NonNull FirebaseUser authUser) {
        FirebaseFirestore db = provider.getFirestore();
        db.collection(FirebaseProviderHelper.COL_USERS).document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        return;
                    }
                    String fullName = ProfileMapper.resolveFallbackName(authUser);
                    String email = authUser.getEmail() != null ? authUser.getEmail() : "";
                    Map<String, Object> userData = ProfileMapper.newUserToMap(
                            uid, fullName, email, "", AppPrefsHelper.getRecentSearches(), Timestamp.now()
                    );
                    db.collection(FirebaseProviderHelper.COL_USERS).document(uid)
                            .set(userData)
                            .addOnFailureListener(e -> Log.w(TAG, "Ensure user doc failed: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.w(TAG, "Check user doc failed: " + e.getMessage()));
    }

    // ===== 2. ĐỌC =====

    public LiveData<ProfileModel> getProfileLiveData() {
        return profileLiveData;
    }

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return OrderRepository.getInstance().getOrdersLiveData();
    }

    public LiveData<List<ProfileAddressModel>> getAddressesLiveData() {
        return addressesLiveData;
    }

    public List<VoucherModel> getVouchers() {
        return VoucherRepository.getInstance().getVouchers();
    }

    @Nullable
    public ProfileModel getProfile() {
        return profileLiveData.getValue();
    }

    @Nullable
    public ProfileAddressModel getDefaultAddress() {
        List<ProfileAddressModel> list = addressesLiveData.getValue();
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (ProfileAddressModel address : list) {
            if (address.isDefaultAddress()) {
                return address;
            }
        }
        return list.get(0);
    }

    @Nullable
    public ProfileAddressModel findAddressById(@Nullable String addressId) {
        List<ProfileAddressModel> list = addressesLiveData.getValue();
        if (addressId == null || list == null) {
            return null;
        }
        for (ProfileAddressModel address : list) {
            if (addressId.equals(address.getId())) {
                return address;
            }
        }
        return null;
    }

    // ===== 3. GHI =====

    public void updateProfile(
            @NonNull String fullName,
            @NonNull String phone,
            @NonNull String email,
            @NonNull String preferredSize,
            @NonNull String runningGoal,
            @Nullable String avatarUri,
            @Nullable ResourceCallback<Void> callback
    ) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            notifyError(callback, "Vui lòng đăng nhập");
            return;
        }
        FirebaseUser authUser = provider.getCurrentUser();
        String canonicalEmail = (authUser != null && authUser.getEmail() != null && !authUser.getEmail().isEmpty())
                ? authUser.getEmail()
                : email;
        Map<String, Object> updates = ProfileMapper.profileUpdateToMap(
                uid, fullName, phone, canonicalEmail, preferredSize, runningGoal, avatarUri, Timestamp.now()
        );

        provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> notifySuccess(callback))
                .addOnFailureListener(e -> notifyError(callback, "Không thể lưu hồ sơ chạy bộ"));
    }

    public void addOrUpdateAddress(
            @Nullable String editId,
            @NonNull String receiverName,
            @NonNull String phone,
            @NonNull String addressDetail,
            @NonNull String label,
            boolean isDefault,
            @Nullable ResourceCallback<Void> callback
    ) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            notifyError(callback, "Vui lòng đăng nhập");
            return;
        }
        FirebaseFirestore db = provider.getFirestore();
        CollectionReference addrRef = db.collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_ADDRESSES);

        List<ProfileAddressModel> existingList = addressesLiveData.getValue();
        boolean makeDefault = ProfileAddressService.shouldMarkAsDefault(isDefault, existingList);

        WriteBatch batch = db.batch();
        if (makeDefault && existingList != null) {
            for (ProfileAddressModel item : existingList) {
                if (!item.getId().equals(editId) && item.isDefaultAddress()) {
                    batch.update(addrRef.document(item.getId()), ProfileMapper.FIELD_IS_DEFAULT, false);
                }
            }
        }

        String docId = (editId != null && !editId.isEmpty()) ? editId : addrRef.document().getId();
        batch.set(addrRef.document(docId), ProfileMapper.addressToMap(
                docId, uid, receiverName, phone, addressDetail, label, makeDefault
        ));
        batch.commit()
                .addOnSuccessListener(unused -> notifySuccess(callback))
                .addOnFailureListener(e -> notifyError(callback, "Không thể lưu địa chỉ giao hàng"));
    }

    public void setDefaultAddress(@NonNull String id, @Nullable ResourceCallback<Void> callback) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            notifyError(callback, "Vui lòng đăng nhập");
            return;
        }
        List<ProfileAddressModel> existingList = addressesLiveData.getValue();
        if (existingList == null) {
            return;
        }
        FirebaseFirestore db = provider.getFirestore();
        CollectionReference addrRef = db.collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_ADDRESSES);

        WriteBatch batch = db.batch();
        for (ProfileAddressModel item : existingList) {
            batch.update(addrRef.document(item.getId()), ProfileMapper.FIELD_IS_DEFAULT, id.equals(item.getId()));
        }
        batch.commit()
                .addOnSuccessListener(unused -> notifySuccess(callback))
                .addOnFailureListener(e -> notifyError(callback, "Không thể đổi địa chỉ mặc định"));
    }

    public void deleteAddress(@NonNull String id, @Nullable ResourceCallback<Void> callback) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            notifyError(callback, "Vui lòng đăng nhập");
            return;
        }
        FirebaseFirestore db = provider.getFirestore();
        CollectionReference addrRef = db.collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_ADDRESSES);

        List<ProfileAddressModel> existingList = addressesLiveData.getValue();
        WriteBatch batch = db.batch();
        batch.delete(addrRef.document(id));

        String replacementId = ProfileAddressService.findReplacementDefaultId(id, existingList);
        if (replacementId != null) {
            batch.update(addrRef.document(replacementId), ProfileMapper.FIELD_IS_DEFAULT, true);
        }
        batch.commit()
                .addOnSuccessListener(unused -> notifySuccess(callback))
                .addOnFailureListener(e -> notifyError(callback, "Không thể xóa địa chỉ"));
    }

    // ===== 4. PRIVATE HELPERS =====

    private static void notifySuccess(@Nullable ResourceCallback<Void> callback) {
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    private static void notifyError(@Nullable ResourceCallback<Void> callback, @NonNull String message) {
        if (callback != null) {
            callback.onError(message);
        }
    }
}
