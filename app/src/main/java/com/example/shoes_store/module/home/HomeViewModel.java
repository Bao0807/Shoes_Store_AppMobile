package com.example.shoes_store.module.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.profile.ProfileModel;

import java.util.List;
import java.util.Set;

/**
 * ViewModel dùng chung cho HomeFragment và HomeSearchActivity.
 */
public class HomeViewModel extends ViewModel {

    private final HomeRepository repository = new HomeRepository();
    private final MutableLiveData<HomeFilterState> filterStateLiveData = new MutableLiveData<>(new HomeFilterState());
    private final MediatorLiveData<List<ProductModel>> displayedProductsLiveData = new MediatorLiveData<>();

    public HomeViewModel() {
        displayedProductsLiveData.addSource(repository.getProductsLiveData(), products -> applyCurrentFilter());
        applyCurrentFilter();
    }

    public LiveData<List<ProductModel>> getDisplayedProductsLiveData() {
        return displayedProductsLiveData;
    }

    public LiveData<List<HomeBannerModel>> getBannersLiveData() {
        return repository.getBannersLiveData();
    }

    public LiveData<HomeFilterState> getFilterStateLiveData() {
        return filterStateLiveData;
    }

    public LiveData<Set<String>> getFavoriteIdsLiveData() {
        return repository.getFavoriteIdsLiveData();
    }

    public LiveData<ProfileModel> getProfileLiveData() {
        return repository.getProfileLiveData();
    }

    public LiveData<List<String>> getRecentSearchesLiveData() {
        return repository.getRecentSearchesLiveData();
    }

    @NonNull
    public HomeFilterState getCurrentFilterState() {
        HomeFilterState current = filterStateLiveData.getValue();
        return current != null ? new HomeFilterState(current) : new HomeFilterState();
    }

    public void setCategory(@NonNull String category) {
        HomeFilterState next = getCurrentFilterState();
        next.setSingleCategory(category);
        updateFilterState(next);
    }

    public void toggleCategory(@NonNull String category) {
        HomeFilterState next = getCurrentFilterState();
        next.toggleCategory(category);
        updateFilterState(next);
    }

    public void clearCategories() {
        HomeFilterState next = getCurrentFilterState();
        next.clearCategories();
        updateFilterState(next);
    }

    public void setQuery(@NonNull String query) {
        HomeFilterState next = getCurrentFilterState();
        next.setQuery(query);
        updateFilterState(next);
    }

    public void setSortOption(@NonNull HomeFilterState.SortOption sortOption) {
        HomeFilterState next = getCurrentFilterState();
        next.setSortOption(sortOption);
        updateFilterState(next);
    }

    public void updateFilterState(@NonNull HomeFilterState nextState) {
        filterStateLiveData.setValue(nextState);
        displayedProductsLiveData.setValue(repository.filterAndSort(nextState));
    }

    public void resetFilters() {
        HomeFilterState next = getCurrentFilterState();
        next.resetFiltersOnly();
        updateFilterState(next);
    }

    public void reload() {
        repository.refreshPublicCatalog();
        applyCurrentFilter();
    }

    public boolean toggleFavorite(@NonNull String productId) {
        return repository.toggleFavorite(productId);
    }

    public void addRecentSearch(@NonNull String query) {
        repository.addRecentSearch(query);
    }

    public void removeRecentSearch(@NonNull String query) {
        repository.removeRecentSearch(query);
    }

    private void applyCurrentFilter() {
        HomeFilterState state = getCurrentFilterState();
        displayedProductsLiveData.setValue(repository.filterAndSort(state));
    }
}
