package com.example.shoes_store;

import com.example.shoes_store.module.home.HomeFilterState;

import org.junit.Test;

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
}
