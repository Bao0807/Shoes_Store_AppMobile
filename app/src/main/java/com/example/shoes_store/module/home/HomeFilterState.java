package com.example.shoes_store.module.home;

/**
 * Trạng thái lọc và sắp xếp danh sách giày chạy bộ.
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
    private String category = ALL;
    private String brand = ALL;
    private long minPrice = DEFAULT_MIN_PRICE;
    private long maxPrice = DEFAULT_MAX_PRICE;
    private SortOption sortOption = SortOption.POPULAR;

    public HomeFilterState() {
    }

    public HomeFilterState(HomeFilterState other) {
        this.query = other.query;
        this.category = other.category;
        this.brand = other.brand;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category == null ? ALL : category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand == null ? ALL : brand;
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

    public boolean hasAdvancedFilter() {
        return !ALL.equals(brand)
                || minPrice > DEFAULT_MIN_PRICE
                || maxPrice < DEFAULT_MAX_PRICE;
    }

    public boolean hasAnyFilter() {
        return !ALL.equals(category) || hasAdvancedFilter();
    }

    public void resetFiltersOnly() {
        this.category = ALL;
        this.brand = ALL;
        this.minPrice = DEFAULT_MIN_PRICE;
        this.maxPrice = DEFAULT_MAX_PRICE;
    }
}
