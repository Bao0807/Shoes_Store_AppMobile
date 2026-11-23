package com.example.shoes_store.module.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreStoreHelper;
import com.example.shoes_store.module.cart.CartVoucherModel;
import com.example.shoes_store.module.order.OrderModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository cho Hồ sơ runner và Sổ địa chỉ trên Cloud Firestore.
 */
public class ProfileRepository {

    private final FirestoreStoreHelper store = FirestoreStoreHelper.getInstance();
    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();

    public LiveData<ProfileModel> getProfileLiveData() {
        return store.getProfileLiveData();
    }

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return store.getOrdersLiveData();
    }

    public LiveData<List<ProfileAddressModel>> getAddressesLiveData() {
        return store.getAddressesLiveData();
    }

    public List<CartVoucherModel> getVouchers() {
        return store.getVouchers();
    }

    public void updateProfile(
            @NonNull String fullName,
            @NonNull String phone,
            @NonNull String email,
            @NonNull String preferredSize,
            @NonNull String runningGoal,
            @Nullable String avatarUri
    ) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            return;
        }
        FirebaseUser authUser = provider.getCurrentUser();
        String canonicalEmail = (authUser != null && authUser.getEmail() != null && !authUser.getEmail().isEmpty())
                ? authUser.getEmail()
                : email;
        Map<String, Object> updates = new HashMap<>();
        updates.put("uid", uid);
        updates.put("fullName", fullName);
        updates.put("phone", phone);
        updates.put("email", canonicalEmail);
        updates.put("preferredSize", preferredSize);
        updates.put("runningGoal", runningGoal);
        updates.put("avatarUrl", avatarUri != null ? avatarUri : "");
        updates.put("updatedAt", Timestamp.now());

        provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .set(updates, SetOptions.merge());
    }

    public void addOrUpdateAddress(
            @Nullable String editId,
            @NonNull String receiverName,
            @NonNull String phone,
            @NonNull String addressDetail,
            @NonNull String label,
            boolean isDefault
    ) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            return;
        }
        FirebaseFirestore db = provider.getFirestore();
        CollectionReference addrRef = db.collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_ADDRESSES);

        List<ProfileAddressModel> existingList = store.getAddressesLiveData().getValue();
        boolean makeDefault = isDefault || existingList == null || existingList.isEmpty();

        WriteBatch batch = db.batch();
        if (makeDefault && existingList != null) {
            for (ProfileAddressModel item : existingList) {
                if (!item.getId().equals(editId) && item.isDefaultAddress()) {
                    batch.update(addrRef.document(item.getId()), "isDefault", false);
                }
            }
        }

        String docId = (editId != null && !editId.isEmpty()) ? editId : addrRef.document().getId();
        Map<String, Object> data = new HashMap<>();
        data.put("addressId", docId);
        data.put("userId", uid);
        data.put("receiverName", receiverName);
        data.put("phone", phone);
        data.put("addressDetail", addressDetail);
        data.put("label", label);
        data.put("isDefault", makeDefault);
        batch.set(addrRef.document(docId), data);
        batch.commit();
    }

    public void setDefaultAddress(@NonNull String id) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            return;
        }
        FirebaseFirestore db = provider.getFirestore();
        CollectionReference addrRef = db.collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_ADDRESSES);

        List<ProfileAddressModel> existingList = store.getAddressesLiveData().getValue();
        if (existingList == null) {
            return;
        }
        WriteBatch batch = db.batch();
        for (ProfileAddressModel item : existingList) {
            batch.update(addrRef.document(item.getId()), "isDefault", id.equals(item.getId()));
        }
        batch.commit();
    }

    public void deleteAddress(@NonNull String id) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            return;
        }
        FirebaseFirestore db = provider.getFirestore();
        CollectionReference addrRef = db.collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .collection(FirebaseProviderHelper.SUB_ADDRESSES);

        List<ProfileAddressModel> existingList = store.getAddressesLiveData().getValue();
        WriteBatch batch = db.batch();
        batch.delete(addrRef.document(id));

        if (existingList != null) {
            boolean deletingDefault = false;
            String replacementId = null;
            for (ProfileAddressModel item : existingList) {
                if (id.equals(item.getId())) {
                    deletingDefault = item.isDefaultAddress();
                } else if (replacementId == null) {
                    replacementId = item.getId();
                }
            }
            if (deletingDefault && replacementId != null) {
                batch.update(addrRef.document(replacementId), "isDefault", true);
            }
        }
        batch.commit();
    }
}
