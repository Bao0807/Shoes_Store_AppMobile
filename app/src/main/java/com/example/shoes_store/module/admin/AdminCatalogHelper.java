package com.example.shoes_store.module.admin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.product.ProductMapper;
import com.example.shoes_store.module.voucher.VoucherMapper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Helper xử lý lưu/cập nhật hàng loạt Sản phẩm, Biến thể tồn kho và Voucher cho kênh Quản trị.
 */
public final class AdminCatalogHelper {

    private static final String DEFAULT_THUMBNAIL_URL =
            "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=1000&q=85";

    private AdminCatalogHelper() {
    }

    static void saveProductWithVariants(
            @NonNull FirebaseFirestore db,
            @Nullable String existingProductId,
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
            @Nullable Integer stockOverride,
            boolean isActive,
            @NonNull ResourceCallback<Void> callback
    ) {
        String error = AdminService.validateProductForm(name, brand, category, price, originalPrice, stockOverride);
        if (error != null) {
            callback.onError(error);
            return;
        }

        boolean isNew = existingProductId == null || existingProductId.trim().isEmpty();
        String productId = isNew
                ? "p_" + System.currentTimeMillis()
                : existingProductId.trim();
        DocumentReference productRef = db.collection(FirebaseProviderHelper.COL_PRODUCTS).document(productId);
        String cleanThumb = thumbnailUrl.trim().isEmpty() ? DEFAULT_THUMBNAIL_URL : thumbnailUrl.trim();

        Map<String, Object> productData = buildProductData(
                productId, name, brand, category, price, originalPrice, cleanThumb,
                weightGrams, heelDropMm, cushionLevel, surfaceType, description,
                colorNames, colorHexes, sizes, isActive, isNew
        );

        if (isNew) {
            commitProductAndVariantsBatch(
                    db,
                    productRef,
                    productId,
                    productData,
                    colorNames,
                    colorHexes,
                    sizes,
                    cleanThumb,
                    stockOverride,
                    Collections.emptyMap(),
                    Collections.emptyList(),
                    callback
            );
            return;
        }

        productRef.collection(FirebaseProviderHelper.SUB_VARIANTS)
                .get()
                .addOnSuccessListener(snap -> {
                    Map<String, Integer> existingStockByVariantId = new HashMap<>();
                    List<DocumentSnapshot> existingDocs = snap.getDocuments();
                    for (DocumentSnapshot doc : existingDocs) {
                        existingStockByVariantId.put(
                                doc.getId(),
                                FirestoreReadUtil.readInt(doc, ProductMapper.FIELD_STOCK_QTY, 0)
                        );
                    }
                    commitProductAndVariantsBatch(
                            db,
                            productRef,
                            productId,
                            productData,
                            colorNames,
                            colorHexes,
                            sizes,
                            cleanThumb,
                            stockOverride,
                            existingStockByVariantId,
                            existingDocs,
                            callback
                    );
                })
                .addOnFailureListener(e -> callback.onError("Không thể đọc tồn kho biến thể hiện tại"));
    }

    @NonNull
    private static Map<String, Object> buildProductData(
            @NonNull String productId,
            @NonNull String name,
            @NonNull String brand,
            @NonNull String category,
            long price,
            long originalPrice,
            @NonNull String cleanThumb,
            @NonNull String weightGrams,
            @NonNull String heelDropMm,
            @NonNull String cushionLevel,
            @NonNull String surfaceType,
            @NonNull String description,
            @NonNull List<String> colorNames,
            @NonNull List<String> colorHexes,
            @NonNull List<String> sizes,
            boolean isActive,
            boolean isNew
    ) {
        long finalOriginalPrice = originalPrice > price ? originalPrice : price;
        Map<String, Object> productData = new HashMap<>();
        productData.put(ProductMapper.FIELD_PRODUCT_ID, productId);
        productData.put(ProductMapper.FIELD_NAME, name.trim());
        productData.put(ProductMapper.FIELD_BRAND, brand.trim());
        productData.put(ProductMapper.FIELD_CATEGORY, category.trim());
        productData.put(ProductMapper.FIELD_PRICE, price);
        productData.put(ProductMapper.FIELD_ORIGINAL_PRICE, finalOriginalPrice);
        productData.put(ProductMapper.FIELD_THUMBNAIL_URL, cleanThumb);
        productData.put(ProductMapper.FIELD_GALLERY_URLS, Collections.singletonList(cleanThumb));
        productData.put(ProductMapper.FIELD_WEIGHT_GRAMS,
                weightGrams.trim().isEmpty() ? ProductMapper.DEFAULT_WEIGHT_GRAMS : weightGrams.trim());
        productData.put(ProductMapper.FIELD_HEEL_DROP_MM,
                heelDropMm.trim().isEmpty() ? ProductMapper.DEFAULT_HEEL_DROP_MM : heelDropMm.trim());
        productData.put(ProductMapper.FIELD_CUSHION_LEVEL,
                cushionLevel.trim().isEmpty() ? ProductMapper.DEFAULT_CUSHION_LEVEL : cushionLevel.trim());
        productData.put(ProductMapper.FIELD_SURFACE_TYPE,
                surfaceType.trim().isEmpty() ? ProductMapper.DEFAULT_SURFACE_TYPE : surfaceType.trim());
        productData.put(ProductMapper.FIELD_DESCRIPTION, description.trim().isEmpty()
                ? "Giày chạy bộ chính hãng phân phối bởi Shoes Store."
                : description.trim());
        productData.put(ProductMapper.FIELD_COLOR_NAMES, colorNames);
        productData.put(ProductMapper.FIELD_COLOR_HEXES, colorHexes);
        productData.put(ProductMapper.FIELD_ALL_SIZES, sizes);
        productData.put(ProductMapper.FIELD_IS_ACTIVE, isActive);
        if (isNew) {
            productData.put(ProductMapper.FIELD_RATING_AVG, 0.0);
            productData.put(ProductMapper.FIELD_REVIEW_COUNT, 0);
            productData.put(ProductMapper.FIELD_CREATED_AT, Timestamp.now());
        }
        return productData;
    }

    private static void commitProductAndVariantsBatch(
            @NonNull FirebaseFirestore db,
            @NonNull DocumentReference productRef,
            @NonNull String productId,
            @NonNull Map<String, Object> productData,
            @NonNull List<String> colorNames,
            @NonNull List<String> colorHexes,
            @NonNull List<String> sizes,
            @NonNull String cleanThumb,
            @Nullable Integer stockOverride,
            @NonNull Map<String, Integer> existingStockByVariantId,
            @NonNull List<DocumentSnapshot> existingVariantDocs,
            @NonNull ResourceCallback<Void> callback
    ) {
        WriteBatch batch = db.batch();
        Set<String> keepVariantIds = new HashSet<>();
        Map<String, Integer> maxStockBySize = new HashMap<>();

        for (int cIdx = 0; cIdx < colorNames.size(); cIdx++) {
            String cName = colorNames.get(cIdx);
            String cHex = cIdx < colorHexes.size() ? colorHexes.get(cIdx) : "#111827";
            for (String size : sizes) {
                String variantId = "v_" + cIdx + "_" + size.replace(".", "_");
                keepVariantIds.add(variantId);
                int finalStock = stockOverride != null
                        ? stockOverride
                        : existingStockByVariantId.getOrDefault(variantId, 10);
                int currentMax = maxStockBySize.getOrDefault(size, 0);
                maxStockBySize.put(size, Math.max(currentMax, Math.max(0, finalStock)));

                DocumentReference vRef = productRef
                        .collection(FirebaseProviderHelper.SUB_VARIANTS)
                        .document(variantId);
                Map<String, Object> vData = new HashMap<>();
                vData.put(ProductMapper.FIELD_VARIANT_ID, variantId);
                vData.put(ProductMapper.FIELD_PRODUCT_ID, productId);
                vData.put(ProductMapper.FIELD_COLOR_NAME, cName);
                vData.put(ProductMapper.FIELD_COLOR_HEX, cHex);
                vData.put(ProductMapper.FIELD_IMAGE_URL, cleanThumb);
                vData.put(ProductMapper.FIELD_SIZE, size);
                vData.put(ProductMapper.FIELD_STOCK_QTY, finalStock);
                vData.put(ProductMapper.FIELD_SKU, productId.toUpperCase(Locale.ROOT) + "-" + cIdx + "-" + size);
                batch.set(vRef, vData, SetOptions.merge());
            }
        }

        for (DocumentSnapshot oldDoc : existingVariantDocs) {
            if (!keepVariantIds.contains(oldDoc.getId())) {
                batch.delete(oldDoc.getReference());
            }
        }

        productData.put(
                ProductMapper.FIELD_OUT_OF_STOCK_SIZES,
                AdminService.computeOutOfStockSizes(sizes, maxStockBySize)
        );
        batch.set(productRef, productData, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Không thể lưu sản phẩm và tồn kho"
                ));
    }

    static void saveVoucher(
            @NonNull FirebaseFirestore db,
            @NonNull String code,
            @NonNull String shortBadge,
            @NonNull String title,
            @NonNull String description,
            long discountAmount,
            long minOrderAmount,
            int usageLimit,
            int validDays,
            boolean isActive,
            @NonNull ResourceCallback<Void> callback
    ) {
        String error = AdminService.validateVoucherForm(code, title, discountAmount, minOrderAmount, usageLimit);
        if (error != null) {
            callback.onError(error);
            return;
        }
        String cleanCode = code.trim().toUpperCase(Locale.ROOT);
        String cleanBadge = shortBadge.trim().isEmpty()
                ? "-" + (discountAmount / 1000L) + "K"
                : shortBadge.trim();
        long expiryMillis = System.currentTimeMillis() + Math.max(1, validDays) * 86_400_000L;

        Map<String, Object> data = new HashMap<>();
        data.put(VoucherMapper.FIELD_CODE, cleanCode);
        data.put(VoucherMapper.FIELD_SHORT_BADGE, cleanBadge);
        data.put(VoucherMapper.FIELD_TITLE, title.trim());
        data.put(VoucherMapper.FIELD_DESCRIPTION, description.trim().isEmpty()
                ? "Áp dụng cho mọi dòng giày chạy bộ đủ điều kiện"
                : description.trim());
        data.put(VoucherMapper.FIELD_DISCOUNT_AMOUNT, discountAmount);
        data.put(VoucherMapper.FIELD_MIN_ORDER_AMOUNT, minOrderAmount);
        data.put(VoucherMapper.FIELD_USAGE_LIMIT, usageLimit);
        data.put(VoucherMapper.FIELD_EXPIRY_DATE, new Timestamp(new Date(expiryMillis)));
        data.put(VoucherMapper.FIELD_IS_ACTIVE, isActive);

        DocumentReference ref = db.collection(FirebaseProviderHelper.COL_VOUCHERS).document(cleanCode);
        ref.get().addOnSuccessListener(existing -> {
            if (!existing.exists()) {
                data.put(VoucherMapper.FIELD_USED_COUNT, 0);
            }
            ref.set(data, SetOptions.merge())
                    .addOnSuccessListener(unused -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onError("Không thể lưu mã giảm giá"));
        }).addOnFailureListener(e -> callback.onError("Không thể kiểm tra mã giảm giá"));
    }
}
