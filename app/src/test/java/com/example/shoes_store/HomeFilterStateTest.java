package com.example.shoes_store;

import com.example.shoes_store.module.home.HomeFilterState;
import com.example.shoes_store.module.home.HomeSearchService;
import com.example.shoes_store.module.product.ProductModel;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HomeFilterStateTest {

    @Test
    public void multiSelectCategoriesAndBrandsWorkCorrectly() {
        HomeFilterState state = new HomeFilterState();
        assertTrue(state.isAllCategoriesSelected());
        assertFalse(state.hasAnyFilter());

        state.toggleCategory("Daily");
        state.toggleCategory("Race");
        state.toggleBrand("Nike");
        state.toggleBrand("ASICS");

        assertEquals(2, state.getSelectedCategories().size());
        assertEquals(2, state.getSelectedBrands().size());
        assertTrue(state.isCategorySelected("daily"));
        assertTrue(state.isBrandSelected("NIKE"));
        assertTrue(state.hasAdvancedFilter());
        assertTrue(state.hasAnyFilter());

        state.toggleCategory("Daily");
        assertEquals(1, state.getSelectedCategories().size());
        assertFalse(state.isCategorySelected("Daily"));

        state.resetFiltersOnly();
        assertTrue(state.isAllCategoriesSelected());
        assertTrue(state.isAllBrandsSelected());
        assertFalse(state.hasAnyFilter());
    }

    @Test
    public void homeSearchServiceFiltersByQueryCategoryPriceAndSorts() {
        List<ProductModel> products = Arrays.asList(
                buildShoe("p1", "Novablast 5", "ASICS", "Daily", 3_500_000L, 4.9, 5),
                buildShoe("p2", "Alphafly 3", "Nike", "Race", 7_500_000L, 5.0, 2),
                buildShoe("p3", "Pegasus 41", "Nike", "Daily", 3_200_000L, 4.5, 10)
        );

        assertEquals(Arrays.asList("Tất cả", "Daily", "Race"), HomeSearchService.getAvailableCategories(products));
        assertEquals(Arrays.asList("Tất cả", "ASICS", "Nike"), HomeSearchService.getAvailableBrands(products));

        HomeFilterState state = new HomeFilterState();
        state.toggleBrand("Nike");
        state.setSortOption(HomeFilterState.SortOption.PRICE_ASC);
        List<ProductModel> nikeSorted = HomeSearchService.filterAndSort(products, state);
        assertEquals(2, nikeSorted.size());
        assertEquals("p3", nikeSorted.get(0).getId());
        assertEquals("p2", nikeSorted.get(1).getId());

        state.resetFiltersOnly();
        state.setQuery("nova");
        List<ProductModel> byQuery = HomeSearchService.filterAndSort(products, state);
        assertEquals(1, byQuery.size());
        assertEquals("p1", byQuery.get(0).getId());
    }

    private ProductModel buildShoe(String id, String name, String brand, String cat, long price, double rating, int reviews) {
        return new ProductModel(
                id, name, brand, cat, price, price, rating, reviews, 0, "",
                Collections.emptyList(), "250g", "8mm", "Cao", "Road", "",
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptySet(), true, 1000L
        );
    }
}
