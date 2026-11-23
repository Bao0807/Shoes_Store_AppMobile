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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
        ProductModel shoe = new ProductModel(
                "p1", "Novablast 5", "ASICS", "Daily", 3_490_000L, 3_490_000L,
                0.0, 0, 0, "", Collections.emptyList(), "255g", "8mm", "Cao", "Road",
                "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptySet(), Collections.emptyList(), true, 1000L
        );
        List<CartItemModel> items = Collections.singletonList(
                new CartItemModel("c1", shoe, "v_0_42", "42", "Cam", 2, true, 1000L)
        );
        assertEquals(2, CartService.getQuantityForVariant(items, "p1", "v_0_42", "42", "Cam"));
        assertEquals(0, CartService.getQuantityForVariant(items, "p1", "v_0_43", "43", "Cam"));

        List<ProductVariantModel> variants = Arrays.asList(
                new ProductVariantModel("v1", "p1", "Cam", "#FF5A36", "", "41", 3, "SKU1"),
                new ProductVariantModel("v2", "p1", "Cam", "#FF5A36", "", "42", 0, "SKU2")
        );
        ProductVariantModel chosen = CartService.chooseQuickAddVariant(variants, "42");
        assertNotNull(chosen);
        assertEquals("v1", chosen.getVariantId());
    }
}
