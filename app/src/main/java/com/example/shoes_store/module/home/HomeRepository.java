package com.example.shoes_store.module.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.shoes_store.core.FirestoreStoreHelper;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.profile.ProfileModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Repository cho màn Trang chủ và Tìm kiếm/Lọc sản phẩm đọc từ Cloud Firestore.
 */
public class HomeRepository {

    private final FirestoreStoreHelper store = FirestoreStoreHelper.getInstance();

    public LiveData<List<HomeBannerModel>> getBannersLiveData() {
        return store.getBannersLiveData();
    }

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

    public void refreshPublicCatalog() {
        store.refreshPublicCatalog();
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

    @NonNull
    public List<String> getAvailableCategories() {
        Set<String> seen = new LinkedHashSet<>();
        seen.add(HomeFilterState.ALL);
        List<ProductModel> products = store.getAllProducts();
        if (products.isEmpty()) {
            seen.addAll(Arrays.asList("Daily", "Race", "Tempo", "Trail", "Recovery"));
        } else {
            for (ProductModel p : products) {
                String cat = p.getCategory().trim();
                if (!cat.isEmpty()) {
                    seen.add(cat);
                }
            }
        }
        return new ArrayList<>(seen);
    }

    @NonNull
    public List<String> getAvailableBrands() {
        Set<String> seen = new LinkedHashSet<>();
        seen.add(HomeFilterState.ALL);
        List<ProductModel> products = store.getAllProducts();
        if (products.isEmpty()) {
            seen.addAll(Arrays.asList("Nike", "ASICS", "HOKA", "Adidas", "Saucony", "Brooks"));
        } else {
            for (ProductModel p : products) {
                String brand = p.getBrand().trim();
                if (!brand.isEmpty()) {
                    seen.add(brand);
                }
            }
        }
        return new ArrayList<>(seen);
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
