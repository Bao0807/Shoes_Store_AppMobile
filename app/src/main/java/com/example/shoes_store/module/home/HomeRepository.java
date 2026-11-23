package com.example.shoes_store.module.home;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.AppPrefsHelper;
import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.product.ProductRepository;
import com.example.shoes_store.module.profile.ProfileMapper;
import com.example.shoes_store.module.profile.ProfileModel;
import com.example.shoes_store.module.profile.ProfileRepository;
import com.example.shoes_store.module.voucher.VoucherRepository;
import com.example.shoes_store.module.wishlist.WishlistRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Repository sở hữu đồng bộ realtime Banner, lịch sử tìm kiếm và truy vấn lọc/sắp xếp Trang chủ.
 */
public class HomeRepository {

    private static final String TAG = "HomeRepository";
    private static volatile HomeRepository instance;

    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();
    private final MutableLiveData<List<HomeBannerModel>> bannersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> recentSearchesLiveData = new MutableLiveData<>(new ArrayList<>());
    @Nullable
    private ListenerRegistration bannersRegistration;

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

    // ===== 1. REALTIME =====

    public void initLocalSearches() {
        recentSearchesLiveData.setValue(AppPrefsHelper.getRecentSearches());
    }

    public void startPublicListening() {
        if (bannersRegistration != null) {
            bannersRegistration.remove();
        }
        bannersRegistration = provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_BANNERS)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Banners listener error: " + error.getMessage());
                    }
                    if (snap == null) {
                        return;
                    }
                    List<HomeBannerModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        HomeBannerModel banner = HomeBannerMapper.fromSnapshot(doc);
                        if (banner.isActive()) {
                            list.add(banner);
                        }
                    }
                    Collections.sort(list, Comparator.comparingInt(HomeBannerModel::getDisplayOrder));
                    bannersLiveData.setValue(list);
                });
    }

    public void refreshPublicCatalog() {
        provider.getFirestore().collection(FirebaseProviderHelper.COL_BANNERS).get();
        ProductRepository.getInstance().refreshPublicCatalog();
        VoucherRepository.getInstance().refreshPublicCatalog();
    }

    public void applyRemoteRecentSearches(@NonNull List<String> remoteSearches) {
        if (!remoteSearches.isEmpty()) {
            AppPrefsHelper.replaceRecentSearches(remoteSearches);
            recentSearchesLiveData.setValue(remoteSearches);
        }
    }

    public void resetRecentSearchesFromLocal() {
        recentSearchesLiveData.setValue(AppPrefsHelper.getRecentSearches());
    }

    // ===== 2. ĐỌC =====

    public LiveData<List<HomeBannerModel>> getBannersLiveData() {
        return bannersLiveData;
    }

    public LiveData<List<ProductModel>> getProductsLiveData() {
        return ProductRepository.getInstance().getProductsLiveData();
    }

    public LiveData<Set<String>> getFavoriteIdsLiveData() {
        return WishlistRepository.getInstance().getFavoriteIdsLiveData();
    }

    public LiveData<ProfileModel> getProfileLiveData() {
        return ProfileRepository.getInstance().getProfileLiveData();
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

    public boolean toggleFavorite(@NonNull String productId) {
        return WishlistRepository.getInstance().toggleFavorite(productId);
    }

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
        provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .update(ProfileMapper.FIELD_RECENT_SEARCHES, updated)
                .addOnFailureListener(e -> Log.w(TAG, "Sync recentSearches failed: " + e.getMessage()));
    }
}
