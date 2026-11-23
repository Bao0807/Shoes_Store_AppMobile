package com.example.shoes_store.module.product;

import androidx.annotation.NonNull;

import com.example.shoes_store.R;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chuyển đổi dữ liệu Firestore cho Sản phẩm, Biến thể tồn kho và Đánh giá.
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

    public static final String FIELD_REVIEW_ID = "reviewId";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_ORDER_ID = "orderId";
    public static final String FIELD_REVIEWER_NAME = "reviewerName";
    public static final String FIELD_REVIEWER_AVATAR_URL = "reviewerAvatarUrl";
    public static final String FIELD_RATING = "rating";
    public static final String FIELD_COMMENT = "comment";

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
                FirestoreReadUtil.readString(doc, FIELD_CATEGORY, "Daily"),
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
                FirestoreReadUtil.readString(doc, FIELD_SURFACE_TYPE, "Road"),
                FirestoreReadUtil.readString(doc, FIELD_DESCRIPTION, ""),
                colorNames,
                colorHexes,
                allSizes,
                outSizes,
                new ArrayList<>(),
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
                FirestoreReadUtil.readInt(doc, FIELD_STOCK_QTY, 0),
                FirestoreReadUtil.readString(doc, FIELD_SKU, "")
        );
    }

    @NonNull
    public static ProductReviewModel reviewFromSnapshot(@NonNull DocumentSnapshot doc, @NonNull String fallbackProductId) {
        return new ProductReviewModel(
                FirestoreReadUtil.readString(doc, FIELD_REVIEW_ID, doc.getId()),
                FirestoreReadUtil.readString(doc, FIELD_PRODUCT_ID, fallbackProductId),
                FirestoreReadUtil.readString(doc, FIELD_USER_ID, ""),
                FirestoreReadUtil.readString(doc, FIELD_ORDER_ID, ""),
                FirestoreReadUtil.readString(doc, FIELD_REVIEWER_NAME, "Runner"),
                FirestoreReadUtil.readString(doc, FIELD_REVIEWER_AVATAR_URL, ""),
                (float) FirestoreReadUtil.readDouble(doc, FIELD_RATING, 5.0),
                "",
                FirestoreReadUtil.readString(doc, FIELD_COMMENT, ""),
                FirestoreReadUtil.readMillis(doc, FIELD_CREATED_AT)
        );
    }

    @NonNull
    public static Map<String, Object> reviewToMap(
            @NonNull String reviewId,
            @NonNull String productId,
            @NonNull String uid,
            @NonNull String orderId,
            @NonNull String reviewerName,
            @NonNull String reviewerAvatarUrl,
            float rating,
            @NonNull String comment,
            @NonNull Timestamp now
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_REVIEW_ID, reviewId);
        data.put(FIELD_PRODUCT_ID, productId);
        data.put(FIELD_USER_ID, uid);
        data.put(FIELD_ORDER_ID, orderId);
        data.put(FIELD_REVIEWER_NAME, reviewerName);
        data.put(FIELD_REVIEWER_AVATAR_URL, reviewerAvatarUrl);
        data.put(FIELD_RATING, (double) rating);
        data.put(FIELD_COMMENT, comment);
        data.put(FIELD_CREATED_AT, now);
        return data;
    }
}
