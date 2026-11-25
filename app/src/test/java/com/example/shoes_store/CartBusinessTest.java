package com.example.shoes_store;

import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.cart.CartService;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.product.ProductVariantModel;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CartBusinessTest {

    @Test
    public void cartServiceValidatesStockForAddAndNextQuantity() {
        assertNull(CartService.validateStockForAdd(1, 2, 3));
        assertNotNull(CartService.validateStockForAdd(2, 2, 3));
        assertNotNull(CartService.validateStockForAdd(0, 1, 0));

        assertNull(CartService.validateStockForNextQuantity(3, 3));
        assertNotNull(CartService.validateStockForNextQuantity(4, 3));
        assertNotNull(CartService.validateStockForNextQuantity(1, 0));
    }

    @Test
    public void cartServiceFindsMatchingItemAndQuickAddVariant() {
        ProductModel shoe = buildShoe();
        List<CartItemModel> items = Collections.singletonList(
                new CartItemModel("c1", shoe, "v_0_42", "42", "Cam", 2, true, 1000L)
        );
        assertEquals(2, CartService.getQuantityForVariant(items, "p1", "v_0_42", "42", "Cam"));
        assertEquals(0, CartService.getQuantityForVariant(items, "p1", "v_0_43", "43", "Cam"));

        List<ProductVariantModel> variants = Arrays.asList(
                new ProductVariantModel("v1", "p1", "Cam", "#FF5A36", "", "41", 3),
                new ProductVariantModel("v2", "p1", "Cam", "#FF5A36", "", "42", 0)
        );
        ProductVariantModel chosen = CartService.chooseQuickAddVariant(variants, "42");
        assertNotNull(chosen);
        assertEquals("v1", chosen.getVariantId());
    }

    @Test
    public void cartServiceSummarizesEmptyPartialAndAllSelectedItems() {
        CartService.CartSummary nullSummary = CartService.summarize(null);
        assertTrue(nullSummary.isEmpty());
        assertFalse(nullSummary.isAllSelected());
        assertEquals(0, nullSummary.getTotalQuantity());
        assertEquals(0, nullSummary.getSelectedQuantity());
        assertEquals(0L, nullSummary.getSelectedSubtotal());

        CartService.CartSummary emptySummary = CartService.summarize(Collections.emptyList());
        assertTrue(emptySummary.isEmpty());
        assertFalse(emptySummary.isAllSelected());

        ProductModel shoe = buildShoe();
        List<CartItemModel> partial = Arrays.asList(
                new CartItemModel("c1", shoe, "v1", "41", "Cam", 2, true, 1000L),
                new CartItemModel("c2", shoe, "v2", "42", "Đen", 1, false, 1000L)
        );
        CartService.CartSummary partialSummary = CartService.summarize(partial);
        assertFalse(partialSummary.isEmpty());
        assertFalse(partialSummary.isAllSelected());
        assertEquals(3, partialSummary.getTotalQuantity());
        assertEquals(2, partialSummary.getSelectedQuantity());
        assertEquals(6_980_000L, partialSummary.getSelectedSubtotal());

        List<CartItemModel> allChecked = Arrays.asList(
                new CartItemModel("c1", shoe, "v1", "41", "Cam", 1, true, 1000L),
                new CartItemModel("c2", shoe, "v2", "42", "Đen", 2, true, 1000L)
        );
        CartService.CartSummary fullSummary = CartService.summarize(allChecked);
        assertFalse(fullSummary.isEmpty());
        assertTrue(fullSummary.isAllSelected());
        assertEquals(3, fullSummary.getTotalQuantity());
        assertEquals(3, fullSummary.getSelectedQuantity());
        assertEquals(10_470_000L, fullSummary.getSelectedSubtotal());
    }

    @Test
    public void cartServiceCalculateFinalTotalNeverNegative() {
        assertEquals(3_340_000L, CartService.calculateFinalTotal(3_490_000L, 150_000L));
        assertEquals(0L, CartService.calculateFinalTotal(100_000L, 500_000L));
        assertEquals(0L, CartService.calculateFinalTotal(-50_000L, 10_000L));
        assertEquals(100_000L, CartService.calculateFinalTotal(100_000L, -20_000L));
    }

    private ProductModel buildShoe() {
        return new ProductModel(
                "p1", "Novablast 5", "ASICS", "Daily", 3_490_000L, 3_490_000L,
                0.0, 0, 0, "", Collections.emptyList(), "255g", "8mm", "Cao", "Road",
                "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptySet(), true, 1000L
        );
    }
}
