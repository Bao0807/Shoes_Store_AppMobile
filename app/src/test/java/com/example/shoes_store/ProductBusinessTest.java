package com.example.shoes_store;

import static org.junit.Assert.assertEquals;

import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.product.ProductVariantModel;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Kiểm thử đơn vị cho logic thuần trên ProductModel (bao gồm giải dải size EU).
 */
public class ProductBusinessTest {

    @Test
    public void euSizeRangeHandlesZeroOneManyAndHalfSizesFromVariants() {
        ProductModel emptyProduct = buildProduct(Collections.emptyList());
        assertEquals(ProductModel.DEFAULT_EU_SIZE_RANGE, emptyProduct.getEuSizeRange());

        ProductModel blankVariantsProduct = buildProduct(Collections.emptyList());
        blankVariantsProduct.setVariants(Collections.singletonList(
                new ProductVariantModel("v1", "p1", "Đen", "#111827", "", "   ", 5)
        ));
        assertEquals(ProductModel.DEFAULT_EU_SIZE_RANGE, blankVariantsProduct.getEuSizeRange());

        ProductModel singleSizeProduct = buildProduct(Collections.emptyList());
        singleSizeProduct.setVariants(Collections.singletonList(
                new ProductVariantModel("v1", "p1", "Đen", "#111827", "", "42", 5)
        ));
        assertEquals("EU 42 - 42", singleSizeProduct.getEuSizeRange());

        ProductModel halfAndUnsortedProduct = buildProduct(Collections.emptyList());
        halfAndUnsortedProduct.setVariants(Arrays.asList(
                new ProductVariantModel("v1", "p1", "Cam", "#FF5A36", "", "42.5", 2),
                new ProductVariantModel("v2", "p1", "Cam", "#FF5A36", "", "40.5", 3),
                new ProductVariantModel("v3", "p1", "Đen", "#111827", "", "41", 1),
                new ProductVariantModel("v4", "p1", "Đen", "#111827", "", " 39 ", 4),
                new ProductVariantModel("v5", "p1", "Đen", "#111827", "", "40.5", 2)
        ));
        assertEquals("EU 39 - 42.5", halfAndUnsortedProduct.getEuSizeRange());

        ProductModel onlyHalfSizesProduct = buildProduct(Collections.emptyList());
        onlyHalfSizesProduct.setVariants(Arrays.asList(
                new ProductVariantModel("v1", "p1", "Cam", "#FF5A36", "", "41.5", 2),
                new ProductVariantModel("v2", "p1", "Cam", "#FF5A36", "", "40.5", 3)
        ));
        assertEquals("EU 40.5 - 41.5", onlyHalfSizesProduct.getEuSizeRange());
    }

    @Test
    public void euSizeRangeFallsBackToAllSizesWhenVariantsEmpty() {
        ProductModel multiAllSizes = buildProduct(Arrays.asList("38", "40.5", "43"));
        assertEquals("EU 38 - 43", multiAllSizes.getEuSizeRange());

        ProductModel singleAllSize = buildProduct(Collections.singletonList("41.5"));
        assertEquals("EU 41.5 - 41.5", singleAllSize.getEuSizeRange());
    }

    private ProductModel buildProduct(List<String> allSizes) {
        return new ProductModel(
                "p1", "Novablast 5", "ASICS", "Daily",
                3_490_000L, 3_890_000L, 4.9, 10, 0, "",
                Collections.emptyList(), "250g", "8 mm", "Cao", "Road", "Mô tả",
                Collections.singletonList("Đen"), Collections.singletonList("#111827"),
                allSizes, Collections.emptySet(), true, 1000L
        );
    }
}
