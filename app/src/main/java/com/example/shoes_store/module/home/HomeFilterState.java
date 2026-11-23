package com.example.shoes_store.module.home;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Trạng thái lọc và sắp xếp danh sách giày chạy bộ.
 * Tập hợp rỗng ở danh mục hoặc thương hiệu tương ứng với lựa chọn "Tất cả".
 */
public class HomeFilterState {

    public static final String ALL = "Tất cả";
    public static final long DEFAULT_MIN_PRICE = 1_000_000L;
    public static final long DEFAULT_MAX_PRICE = 8_000_000L;

    public enum SortOption {
        POPULAR,
        PRICE_ASC,
        PRICE_DESC,
        RATING_DESC
    }

    private String query = "";
    private final LinkedHashSet<String> selectedCategories = new LinkedHashSet<>();
    private final LinkedHashSet<String> selectedBrands = new LinkedHashSet<>();
    private long minPrice = DEFAULT_MIN_PRICE;
    private long maxPrice = DEFAULT_MAX_PRICE;
    private SortOption sortOption = SortOption.POPULAR;

    public HomeFilterState() {
    }

    public HomeFilterState(HomeFilterState other) {
        if (other == null) {
            return;
        }
        this.query = other.query;
        this.selectedCategories.addAll(other.selectedCategories);
        this.selectedBrands.addAll(other.selectedBrands);
        this.minPrice = other.minPrice;
        this.maxPrice = other.maxPrice;
        this.sortOption = other.sortOption;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
    }

    public Set<String> getSelectedCategories() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(selectedCategories));
    }

    public Set<String> getSelectedBrands() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(selectedBrands));
    }

    public boolean isAllCategoriesSelected() {
        return selectedCategories.isEmpty();
    }

    public boolean isAllBrandsSelected() {
        return selectedBrands.isEmpty();
    }

    public boolean isCategorySelected(String category) {
        return containsIgnoreCase(selectedCategories, category);
    }

    public boolean isBrandSelected(String brand) {
        return containsIgnoreCase(selectedBrands, brand);
    }

    public void setSingleCategory(String category) {
        selectedCategories.clear();
        if (category == null) {
            return;
        }
        String trimmed = category.trim();
        if (!trimmed.isEmpty() && !ALL.equalsIgnoreCase(trimmed)) {
            selectedCategories.add(trimmed);
        }
    }

    public void toggleCategory(String category) {
        if (category == null) {
            return;
        }
        String trimmed = category.trim();
        if (trimmed.isEmpty() || ALL.equalsIgnoreCase(trimmed)) {
            clearCategories();
            return;
        }
        if (!removeIgnoreCase(selectedCategories, trimmed)) {
            selectedCategories.add(trimmed);
        }
    }

    public void clearCategories() {
        selectedCategories.clear();
    }

    public void toggleBrand(String brand) {
        if (brand == null) {
            return;
        }
        String trimmed = brand.trim();
        if (trimmed.isEmpty() || ALL.equalsIgnoreCase(trimmed)) {
            clearBrands();
            return;
        }
        if (!removeIgnoreCase(selectedBrands, trimmed)) {
            selectedBrands.add(trimmed);
        }
    }

    public void clearBrands() {
        selectedBrands.clear();
    }

    public long getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(long minPrice) {
        this.minPrice = minPrice;
    }

    public long getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(long maxPrice) {
        this.maxPrice = maxPrice;
    }

    public SortOption getSortOption() {
        return sortOption;
    }

    public void setSortOption(SortOption sortOption) {
        this.sortOption = sortOption == null ? SortOption.POPULAR : sortOption;
    }

    public boolean hasPriceFilter() {
        return minPrice > DEFAULT_MIN_PRICE || maxPrice < DEFAULT_MAX_PRICE;
    }

    public boolean hasAdvancedFilter() {
        return !selectedBrands.isEmpty()
                || selectedCategories.size() > 1
                || hasPriceFilter();
    }

    public boolean hasAnyFilter() {
        return !selectedCategories.isEmpty()
                || !selectedBrands.isEmpty()
                || hasPriceFilter();
    }

    public void resetFiltersOnly() {
        this.selectedCategories.clear();
        this.selectedBrands.clear();
        this.minPrice = DEFAULT_MIN_PRICE;
        this.maxPrice = DEFAULT_MAX_PRICE;
    }

    private boolean containsIgnoreCase(Set<String> items, String target) {
        if (target == null) {
            return false;
        }
        for (String item : items) {
            if (target.equalsIgnoreCase(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean removeIgnoreCase(Set<String> items, String target) {
        if (target == null) {
            return false;
        }
        Iterator<String> iterator = items.iterator();
        while (iterator.hasNext()) {
            if (target.equalsIgnoreCase(iterator.next())) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }
}
