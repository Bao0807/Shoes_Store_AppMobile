package com.example.shoes_store.module.voucher;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Repository sở hữu đồng bộ realtime danh sách Voucher công khai từ collection `vouchers`.
 */
public class VoucherRepository {

    private static final String TAG = "VoucherRepository";
    private static volatile VoucherRepository instance;

    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();
    private final MutableLiveData<List<VoucherModel>> vouchersLiveData = new MutableLiveData<>(new ArrayList<>());
    @Nullable
    private ListenerRegistration vouchersRegistration;

    private VoucherRepository() {
    }

    public static VoucherRepository getInstance() {
        if (instance == null) {
            synchronized (VoucherRepository.class) {
                if (instance == null) {
                    instance = new VoucherRepository();
                }
            }
        }
        return instance;
    }

    // ===== 1. REALTIME =====

    public void startPublicListening() {
        if (vouchersRegistration != null) {
            vouchersRegistration.remove();
        }
        vouchersRegistration = provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_VOUCHERS)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Vouchers listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<VoucherModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        VoucherModel voucher = VoucherMapper.fromSnapshot(doc);
                        if (voucher.isActive()) {
                            list.add(voucher);
                        }
                    }
                    Collections.sort(list, Comparator.comparingLong(VoucherModel::getMinOrderAmount));
                    vouchersLiveData.setValue(list);
                });
    }

    public void refreshPublicCatalog() {
        provider.getFirestore().collection(FirebaseProviderHelper.COL_VOUCHERS).get();
    }

    // ===== 2. ĐỌC =====

    public LiveData<List<VoucherModel>> getVouchersLiveData() {
        return vouchersLiveData;
    }

    @NonNull
    public List<VoucherModel> getVouchers() {
        List<VoucherModel> list = vouchersLiveData.getValue();
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    @Nullable
    public VoucherModel findByCode(@Nullable String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        for (VoucherModel voucher : getVouchers()) {
            if (voucher.getCode().equalsIgnoreCase(code.trim())) {
                return voucher;
            }
        }
        return null;
    }
}
