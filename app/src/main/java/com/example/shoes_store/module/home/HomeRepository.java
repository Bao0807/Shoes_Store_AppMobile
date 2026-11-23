package com.example.shoes_store.module.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.shoes_store.core.MockStoreHelper;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.profile.ProfileModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Repository cho màn Trang chủ và Tìm kiếm/Lọc sản phẩm.
 */
public class HomeRepository {

    private final MockStoreHelper store = MockStoreHelper.getInstance();

    public LiveData<List<ProductModel>> getProductsLiveData() {
        return store.getProductsLiveData();
    }

    public LiveData<Set<String>> getFavoriteIdsLiveData() {
        return store.getFavoriteIdsLiveData();
    }

    public LiveData<ProfileModel> getProfileLiveData() {
        return store.getProfileLiveData();
    }

    public LiveData<List<String>> getRecentSearchesLiveData() {
        return store.getRecentSearchesLiveData();
    }

    public List<HomeBannerModel> getBanners() {
        return store.getBanners();
    }

    public boolean toggleFavorite(@NonNull String productId) {
        return store.toggleFavorite(productId);
    }

    public void addRecentSearch(@NonNull String query) {
        store.addRecentSearch(query);
    }

    public void removeRecentSearch(@NonNull String query) {
        store.removeRecentSearch(query);
    }

    public List<ProductModel> filterAndSort(@NonNull HomeFilterState state) {
        List<ProductModel> source = store.getAllProducts();
        List<ProductModel> filtered = new ArrayList<>();
        String queryLower = state.getQuery().trim().toLowerCase(Locale.getDefault());

        for (ProductModel p : source) {
            if (!state.isAllCategoriesSelected() && !state.isCategorySelected(p.getCategory())) {
                continue;
            }
            if (!state.isAllBrandsSelected() && !state.isBrandSelected(p.getBrand())) {
                continue;
            }
            if (p.getPrice() < state.getMinPrice() || p.getPrice() > state.getMaxPrice()) {
                continue;
            }
            if (!queryLower.isEmpty()) {
                String haystack = (p.getName() + " " + p.getBrand() + " " + p.getCategory())
                        .toLowerCase(Locale.getDefault());
                if (!haystack.contains(queryLower)) {
                    continue;
                }
            }
            filtered.add(p);
        }

        switch (state.getSortOption()) {
            case PRICE_ASC:
                Collections.sort(filtered, Comparator.comparingLong(ProductModel::getPrice));
                break;
            case PRICE_DESC:
                Collections.sort(filtered, (a, b) -> Long.compare(b.getPrice(), a.getPrice()));
                break;
            case RATING_DESC:
                Collections.sort(filtered, (a, b) -> Double.compare(b.getRating(), a.getRating()));
                break;
            case POPULAR:
            default:
                Collections.sort(filtered, (a, b) -> Integer.compare(b.getReviewCount(), a.getReviewCount()));
                break;
        }
        return filtered;
    }
}
