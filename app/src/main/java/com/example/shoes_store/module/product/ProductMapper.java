package com.example.shoes_store.module.product;

import androidx.annotation.NonNull;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CategoryStyleHelper;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Chuyển đổi dữ liệu Firestore cho Sản phẩm và Biến thể tồn kho.
 */
public final class ProductMapper {

    public static final String FIELD_PRODUCT_ID = "productId";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_BRAND = "brand";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_PRICE = "price";
    public static final String FIELD_ORIGINAL_PRICE = "originalPrice";
    public static final String FIELD_RATING_AVG = "ratingAvg";
    public static final String FIELD_REVIEW_COUNT = "reviewCount";
    public static final String FIELD_THUMBNAIL_URL = "thumbnailUrl";
    public static final String FIELD_GALLERY_URLS = "galleryUrls";
    public static final String FIELD_WEIGHT_GRAMS = "weightGrams";
    public static final String FIELD_HEEL_DROP_MM = "heelDropMm";
    public static final String FIELD_CUSHION_LEVEL = "cushionLevel";
    public static final String FIELD_SURFACE_TYPE = "surfaceType";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_COLOR_NAMES = "colorNames";
    public static final String FIELD_COLOR_HEXES = "colorHexes";
    public static final String FIELD_ALL_SIZES = "allSizes";
    public static final String FIELD_OUT_OF_STOCK_SIZES = "outOfStockSizes";
    public static final String FIELD_IS_ACTIVE = "isActive";
    public static final String FIELD_CREATED_AT = "createdAt";

    public static final String FIELD_VARIANT_ID = "variantId";
    public static final String FIELD_COLOR_NAME = "colorName";
    public static final String FIELD_COLOR_HEX = "colorHex";
    public static final String FIELD_IMAGE_URL = "imageUrl";
    public static final String FIELD_SIZE = "size";
    public static final String FIELD_STOCK_QTY = "stockQty";
    public static final String FIELD_SKU = "sku";

    public static final String DEFAULT_WEIGHT_GRAMS = "250g";
    public static final String DEFAULT_HEEL_DROP_MM = "8 mm";
    public static final String DEFAULT_CUSHION_LEVEL = "Cao";
    public static final String DEFAULT_SURFACE_TYPE = "Road";

    private ProductMapper() {
    }

    @NonNull
    public static ProductModel productFromSnapshot(@NonNull DocumentSnapshot doc) {
        List<String> galleryUrls = FirestoreReadUtil.readStringList(doc, FIELD_GALLERY_URLS);
        List<String> colorNames = FirestoreReadUtil.readStringList(doc, FIELD_COLOR_NAMES);
        List<String> colorHexes = FirestoreReadUtil.readStringList(doc, FIELD_COLOR_HEXES);
        List<String> allSizes = FirestoreReadUtil.readStringList(doc, FIELD_ALL_SIZES);
        Set<String> outSizes = new LinkedHashSet<>(FirestoreReadUtil.readStringList(doc, FIELD_OUT_OF_STOCK_SIZES));

        return new ProductModel(
                FirestoreReadUtil.readString(doc, FIELD_PRODUCT_ID, doc.getId()),
                FirestoreReadUtil.readString(doc, FIELD_NAME, ""),
                FirestoreReadUtil.readString(doc, FIELD_BRAND, ""),
                FirestoreReadUtil.readString(doc, FIELD_CATEGORY, CategoryStyleHelper.CAT_DAILY),
                FirestoreReadUtil.readLong(doc, FIELD_PRICE, 0L),
                FirestoreReadUtil.readLong(doc, FIELD_ORIGINAL_PRICE, 0L),
                FirestoreReadUtil.readDouble(doc, FIELD_RATING_AVG, 0.0),
                FirestoreReadUtil.readInt(doc, FIELD_REVIEW_COUNT, 0),
                R.drawable.img_shoe_asics_novablast,
                FirestoreReadUtil.readString(doc, FIELD_THUMBNAIL_URL, ""),
                galleryUrls,
                FirestoreReadUtil.readString(doc, FIELD_WEIGHT_GRAMS, ""),
                FirestoreReadUtil.readString(doc, FIELD_HEEL_DROP_MM, ""),
                FirestoreReadUtil.readString(doc, FIELD_CUSHION_LEVEL, ""),
                FirestoreReadUtil.readString(doc, FIELD_SURFACE_TYPE, DEFAULT_SURFACE_TYPE),
                FirestoreReadUtil.readString(doc, FIELD_DESCRIPTION, ""),
                colorNames,
                colorHexes,
                allSizes,
                outSizes,
                FirestoreReadUtil.readBoolean(doc, FIELD_IS_ACTIVE, true),
                FirestoreReadUtil.readMillis(doc, FIELD_CREATED_AT)
        );
    }

    @NonNull
    public static ProductVariantModel variantFromSnapshot(@NonNull DocumentSnapshot doc, @NonNull String fallbackProductId) {
        return new ProductVariantModel(
                FirestoreReadUtil.readString(doc, FIELD_VARIANT_ID, doc.getId()),
                FirestoreReadUtil.readString(doc, FIELD_PRODUCT_ID, fallbackProductId),
                FirestoreReadUtil.readString(doc, FIELD_COLOR_NAME, "Mặc định"),
                FirestoreReadUtil.readString(doc, FIELD_COLOR_HEX, "#111827"),
                FirestoreReadUtil.readString(doc, FIELD_IMAGE_URL, ""),
                FirestoreReadUtil.readString(doc, FIELD_SIZE, "42"),
                FirestoreReadUtil.readInt(doc, FIELD_STOCK_QTY, 0)
        );
    }

    @NonNull
    public static Map<String, Object> productWriteMap(
            @NonNull String productId,
            @NonNull String name,
            @NonNull String brand,
            @NonNull String category,
            long price,
            long originalPrice,
            @NonNull String thumbnailUrl,
            @NonNull String weightGrams,
            @NonNull String heelDropMm,
            @NonNull String cushionLevel,
            @NonNull String surfaceType,
            @NonNull String description,
            @NonNull List<String> colorNames,
            @NonNull List<String> colorHexes,
            @NonNull List<String> sizes,
            boolean isActive,
            boolean isNew,
            @NonNull Timestamp createdAt
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_PRODUCT_ID, productId);
        data.put(FIELD_NAME, name.trim());
        data.put(FIELD_BRAND, brand.trim());
        data.put(FIELD_CATEGORY, category.trim());
        data.put(FIELD_PRICE, price);
        data.put(FIELD_ORIGINAL_PRICE, originalPrice);
        data.put(FIELD_THUMBNAIL_URL, thumbnailUrl);
        data.put(FIELD_GALLERY_URLS, Collections.singletonList(thumbnailUrl));
        data.put(FIELD_WEIGHT_GRAMS, weightGrams.trim().isEmpty() ? DEFAULT_WEIGHT_GRAMS : weightGrams.trim());
        data.put(FIELD_HEEL_DROP_MM, heelDropMm.trim().isEmpty() ? DEFAULT_HEEL_DROP_MM : heelDropMm.trim());
        data.put(FIELD_CUSHION_LEVEL, cushionLevel.trim().isEmpty() ? DEFAULT_CUSHION_LEVEL : cushionLevel.trim());
        data.put(FIELD_SURFACE_TYPE, surfaceType.trim().isEmpty() ? DEFAULT_SURFACE_TYPE : surfaceType.trim());
        data.put(FIELD_DESCRIPTION, description.trim().isEmpty()
                ? "Giày chạy bộ chính hãng phân phối bởi Shoes Store."
                : description.trim());
        data.put(FIELD_COLOR_NAMES, colorNames);
        data.put(FIELD_COLOR_HEXES, colorHexes);
        data.put(FIELD_ALL_SIZES, sizes);
        data.put(FIELD_IS_ACTIVE, isActive);
        if (isNew) {
            data.put(FIELD_RATING_AVG, 0.0);
            data.put(FIELD_REVIEW_COUNT, 0);
            data.put(FIELD_CREATED_AT, createdAt);
        }
        return data;
    }

    @NonNull
    public static Map<String, Object> variantWriteMap(
            @NonNull String variantId,
            @NonNull String productId,
            @NonNull String colorName,
            @NonNull String colorHex,
            @NonNull String imageUrl,
            @NonNull String size,
            int stockQty,
            int colorIndex
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_VARIANT_ID, variantId);
        data.put(FIELD_PRODUCT_ID, productId);
        data.put(FIELD_COLOR_NAME, colorName);
        data.put(FIELD_COLOR_HEX, colorHex);
        data.put(FIELD_IMAGE_URL, imageUrl);
        data.put(FIELD_SIZE, size);
        data.put(FIELD_STOCK_QTY, stockQty);
        data.put(FIELD_SKU, productId.toUpperCase(Locale.ROOT) + "-" + colorIndex + "-" + size);
        return data;
    }
}
