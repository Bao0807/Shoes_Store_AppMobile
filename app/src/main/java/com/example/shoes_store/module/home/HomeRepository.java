package com.example.shoes_store.module.home;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.shoes_store.core.AppPrefsHelper;
import com.example.shoes_store.core.BaseFirestoreRepository;
import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.product.ProductRepository;
import com.example.shoes_store.module.profile.ProfileMapper;
import com.example.shoes_store.module.profile.ProfileRepository;
import com.example.shoes_store.module.voucher.VoucherRepository;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Repository sở hữu đồng bộ realtime Banner, lịch sử tìm kiếm và truy vấn lọc/sắp xếp Trang chủ.
 */
public class HomeRepository extends BaseFirestoreRepository {

    private static final String TAG = "HomeRepository";
    private static final String LISTENER_BANNERS = "banners";
    private static volatile HomeRepository instance;

    private final MutableLiveData<List<HomeBannerModel>> bannersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> recentSearchesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final Observer<List<String>> remoteSearchesObserver = remoteSearches -> {
        if (remoteSearches != null && !remoteSearches.isEmpty()) {
            applyRemoteRecentSearches(remoteSearches);
        }
    };
    private boolean observingRemoteSearches = false;

    private HomeRepository() {
    }

    public static HomeRepository getInstance() {
        if (instance == null) {
            synchronized (HomeRepository.class) {
                if (instance == null) {
                    instance = new HomeRepository();
                }
            }
        }
        return instance;
    }

    @Override
    public void onAppInit() {
        initLocalSearches();
        startPublicListening();
    }

    @Override
    public void onSessionStart(@NonNull String uid, @NonNull FirebaseUser authUser) {
        if (!observingRemoteSearches) {
            ProfileRepository.getInstance()
                    .getRemoteRecentSearchesLiveData()
                    .observeForever(remoteSearchesObserver);
            observingRemoteSearches = true;
        }
    }

    @Override
    public void onSessionEnd() {
        if (observingRemoteSearches) {
            ProfileRepository.getInstance()
                    .getRemoteRecentSearchesLiveData()
                    .removeObserver(remoteSearchesObserver);
            observingRemoteSearches = false;
        }
        resetRecentSearchesFromLocal();
        super.onSessionEnd();
    }

    // ===== 1. REALTIME =====

    public void initLocalSearches() {
        recentSearchesLiveData.setValue(AppPrefsHelper.getRecentSearches());
    }

    public void startPublicListening() {
        replacePublicListener(LISTENER_BANNERS, listenCollection(
                collection(FirebaseProviderHelper.COL_BANNERS),
                TAG,
                doc -> {
                    HomeBannerModel banner = HomeBannerMapper.fromSnapshot(doc);
                    return banner.isActive() ? banner : null;
                },
                Comparator.comparingInt(HomeBannerModel::getDisplayOrder),
                bannersLiveData::setValue
        ));
    }

    public void refreshPublicCatalog() {
        collection(FirebaseProviderHelper.COL_BANNERS)
                .get()
                .addOnFailureListener(e -> Log.w(TAG, "Refresh banners failed: " + e.getMessage()));
        ProductRepository.getInstance().refreshPublicCatalog();
        VoucherRepository.getInstance().refreshPublicCatalog();
    }

    private void applyRemoteRecentSearches(@NonNull List<String> remoteSearches) {
        if (!remoteSearches.isEmpty()) {
            AppPrefsHelper.replaceRecentSearches(remoteSearches);
            recentSearchesLiveData.setValue(remoteSearches);
        }
    }

    private void resetRecentSearchesFromLocal() {
        recentSearchesLiveData.setValue(AppPrefsHelper.getRecentSearches());
    }

    // ===== 2. ĐỌC =====

    public LiveData<List<HomeBannerModel>> getBannersLiveData() {
        return bannersLiveData;
    }

    public LiveData<List<String>> getRecentSearchesLiveData() {
        return recentSearchesLiveData;
    }

    @NonNull
    public List<String> getAvailableCategories() {
        return HomeSearchService.getAvailableCategories(ProductRepository.getInstance().getAllProducts());
    }

    @NonNull
    public List<String> getAvailableBrands() {
        return HomeSearchService.getAvailableBrands(ProductRepository.getInstance().getAllProducts());
    }

    @NonNull
    public List<ProductModel> filterAndSort(@NonNull HomeFilterState state) {
        return HomeSearchService.filterAndSort(ProductRepository.getInstance().getAllProducts(), state);
    }

    // ===== 3. GHI =====

    public void addRecentSearch(@NonNull String query) {
        List<String> updated = AppPrefsHelper.addRecentSearch(query);
        recentSearchesLiveData.setValue(updated);
        syncRecentSearchesToFirestore(updated);
    }

    public void removeRecentSearch(@NonNull String query) {
        List<String> updated = AppPrefsHelper.removeRecentSearch(query);
        recentSearchesLiveData.setValue(updated);
        syncRecentSearchesToFirestore(updated);
    }

    // ===== 4. PRIVATE HELPERS =====

    private void syncRecentSearchesToFirestore(@NonNull List<String> updated) {
        String uid = provider.getCurrentUid();
        if (uid == null) {
            return;
        }
        collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .update(ProfileMapper.FIELD_RECENT_SEARCHES, updated)
                .addOnFailureListener(e -> Log.w(TAG, "Sync recentSearches failed: " + e.getMessage()));
    }
}
