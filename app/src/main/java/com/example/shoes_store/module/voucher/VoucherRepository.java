package com.example.shoes_store.module.voucher;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.BaseFirestoreRepository;
import com.example.shoes_store.core.FirebaseProviderHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Repository sở hữu đồng bộ realtime danh sách Voucher công khai từ collection `vouchers`.
 */
public class VoucherRepository extends BaseFirestoreRepository {

    private static final String TAG = "VoucherRepository";
    private static final String LISTENER_VOUCHERS = "vouchers";
    private static volatile VoucherRepository instance;

    private final MutableLiveData<List<VoucherModel>> vouchersLiveData = new MutableLiveData<>(new ArrayList<>());

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

    @Override
    public void onAppInit() {
        startPublicListening();
    }

    // ===== 1. REALTIME =====

    public void startPublicListening() {
        replacePublicListener(LISTENER_VOUCHERS, listenCollection(
                collection(FirebaseProviderHelper.COL_VOUCHERS),
                TAG,
                doc -> {
                    VoucherModel voucher = VoucherMapper.fromSnapshot(doc);
                    return voucher.isActive() ? voucher : null;
                },
                Comparator.comparingLong(VoucherModel::getMinOrderAmount),
                vouchersLiveData::setValue
        ));
    }

    public void refreshPublicCatalog() {
        collection(FirebaseProviderHelper.COL_VOUCHERS)
                .get()
                .addOnFailureListener(e -> Log.w(TAG, "Refresh vouchers failed: " + e.getMessage()));
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
        return VoucherService.findByCode(getVouchers(), code);
    }
}
