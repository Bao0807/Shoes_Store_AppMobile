package com.example.shoes_store.module.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.module.product.ProductModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Nghiệp vụ thuần Java cho lọc, sắp xếp và trích xuất danh mục/thương hiệu sản phẩm trên Trang chủ & Tìm kiếm.
 */
public final class HomeSearchService {

    private HomeSearchService() {
    }

    @NonNull
    public static List<String> getAvailableCategories(@Nullable List<ProductModel> products) {
        Set<String> seen = new LinkedHashSet<>();
        seen.add(HomeFilterState.ALL);
        if (products == null || products.isEmpty()) {
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
    public static List<String> getAvailableBrands(@Nullable List<ProductModel> products) {
        Set<String> seen = new LinkedHashSet<>();
        seen.add(HomeFilterState.ALL);
        if (products == null || products.isEmpty()) {
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

    @NonNull
    public static List<ProductModel> filterAndSort(
            @Nullable List<ProductModel> source,
            @NonNull HomeFilterState state
    ) {
        List<ProductModel> filtered = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            return filtered;
        }
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
