package com.example.shoes_store.module.product;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CategoryStyleHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Model sản phẩm giày chạy bộ kèm thông số chuyên biệt và danh sách biến thể màu/size.
 */
public class ProductModel {

    public static final String DEFAULT_EU_SIZE_RANGE = "EU 38 - 44";

    private final String id;
    private final String name;
    private final String brand;
    private final String category;
    private final long price;
    private final long originalPrice;
    private final double rating;
    private final int reviewCount;
    private final int imageResId;
    private final String thumbnailUrl;
    private final List<String> galleryUrls;
    private final String weightGrams;
    private final String heelDropMm;
    private final String cushionLevel;
    private final String surfaceType;
    private final String description;
    private final List<String> colorNames;
    private final List<String> colorHexes;
    private final List<String> allSizes;
    private final Set<String> outOfStockSizes;
    private final boolean active;
    private final long createdAtMillis;
    private final List<ProductVariantModel> variants = new ArrayList<>();

    public ProductModel(
            String id,
            String name,
            String brand,
            String category,
            long price,
            long originalPrice,
            double rating,
            int reviewCount,
            int imageResId,
            String thumbnailUrl,
            List<String> galleryUrls,
            String weightGrams,
            String heelDropMm,
            String cushionLevel,
            String surfaceType,
            String description,
            List<String> colorNames,
            List<String> colorHexes,
            List<String> allSizes,
            Set<String> outOfStockSizes,
            boolean active,
            long createdAtMillis
    ) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.brand = brand != null ? brand : "";
        this.category = category != null ? category : CategoryStyleHelper.CAT_DAILY;
        this.price = price;
        this.originalPrice = originalPrice;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.imageResId = imageResId != 0 ? imageResId : R.drawable.img_shoe_asics_novablast;
        this.thumbnailUrl = thumbnailUrl != null ? thumbnailUrl : "";
        this.galleryUrls = galleryUrls != null ? new ArrayList<>(galleryUrls) : new ArrayList<>();
        this.weightGrams = weightGrams != null ? weightGrams : "";
        this.heelDropMm = heelDropMm != null ? heelDropMm : "";
        this.cushionLevel = cushionLevel != null ? cushionLevel : "";
        this.surfaceType = surfaceType != null ? surfaceType : "";
        this.description = description != null ? description : "";
        this.colorNames = colorNames != null ? colorNames : new ArrayList<>();
        this.colorHexes = colorHexes != null ? colorHexes : new ArrayList<>();
        this.allSizes = allSizes != null ? allSizes : new ArrayList<>();
        this.outOfStockSizes = outOfStockSizes != null ? outOfStockSizes : new LinkedHashSet<>();
        this.active = active;
        this.createdAtMillis = createdAtMillis;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public String getCategory() {
        return category;
    }

    public long getPrice() {
        return price;
    }

    public long getOriginalPrice() {
        return originalPrice;
    }

    public boolean hasDiscount() {
        return originalPrice > price;
    }

    public int getDiscountPercent() {
        if (!hasDiscount()) {
            return 0;
        }
        return (int) Math.round(((double) (originalPrice - price) / originalPrice) * 100);
    }

    public double getRating() {
        return rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public int getImageResId() {
        return imageResId;
    }

    @NonNull
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    @NonNull
    public List<String> getGalleryUrls() {
        return galleryUrls;
    }

    public String getWeightGrams() {
        return weightGrams;
    }

    public String getHeelDropMm() {
        return heelDropMm;
    }

    public String getCushionLevel() {
        return cushionLevel;
    }

    public String getSurfaceType() {
        return surfaceType;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getColorNames() {
        return colorNames;
    }

    public List<String> getColorHexes() {
        return colorHexes;
    }

    public List<String> getAllSizes() {
        return allSizes;
    }

    public Set<String> getOutOfStockSizes() {
        return outOfStockSizes;
    }

    public boolean isActive() {
        return active;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    @NonNull
    public List<ProductVariantModel> getVariants() {
        return variants;
    }

    public void setVariants(@Nullable List<ProductVariantModel> loadedVariants) {
        variants.clear();
        if (loadedVariants != null) {
            variants.addAll(loadedVariants);
        }
    }

    @NonNull
    public List<ProductVariantModel> getColorRepresentatives() {
        if (variants.isEmpty()) {
            List<ProductVariantModel> fallback = new ArrayList<>();
            for (int i = 0; i < colorNames.size(); i++) {
                String cName = colorNames.get(i);
                String cHex = i < colorHexes.size() ? colorHexes.get(i) : "#111827";
                fallback.add(new ProductVariantModel("", id, cName, cHex, thumbnailUrl, "", 1));
            }
            return fallback;
        }
        Map<String, ProductVariantModel> map = new LinkedHashMap<>();
        for (ProductVariantModel v : variants) {
            if (!map.containsKey(v.getColorName())) {
                map.put(v.getColorName(), v);
            }
        }
        return new ArrayList<>(map.values());
    }

    @NonNull
    public List<String> getSizesForColor(@Nullable String colorName) {
        if (variants.isEmpty()) {
            return new ArrayList<>(allSizes);
        }
        List<String> sizes = new ArrayList<>();
        for (ProductVariantModel v : variants) {
            if (colorName == null || colorName.isEmpty() || v.getColorName().equalsIgnoreCase(colorName)) {
                if (!sizes.contains(v.getSize())) {
                    sizes.add(v.getSize());
                }
            }
        }
        return sizes.isEmpty() ? new ArrayList<>(allSizes) : sizes;
    }

    @NonNull
    public Set<String> getOutOfStockSizesForColor(@Nullable String colorName) {
        if (variants.isEmpty()) {
            return new LinkedHashSet<>(outOfStockSizes);
        }
        Set<String> out = new LinkedHashSet<>();
        for (ProductVariantModel v : variants) {
            if ((colorName == null || colorName.isEmpty() || v.getColorName().equalsIgnoreCase(colorName))
                    && !v.isInStock()) {
                out.add(v.getSize());
            }
        }
        return out;
    }

    @Nullable
    public ProductVariantModel findVariant(@Nullable String colorName, @Nullable String size) {
        if (size == null) {
            return null;
        }
        for (ProductVariantModel v : variants) {
            boolean colorMatch = colorName == null || colorName.isEmpty() || v.getColorName().equalsIgnoreCase(colorName);
            if (colorMatch && v.getSize().equalsIgnoreCase(size)) {
                return v;
            }
        }
        return null;
    }

    @NonNull
    public String getEuSizeRange() {
        List<String> sizes;
        if (!variants.isEmpty()) {
            Set<String> distinct = new LinkedHashSet<>();
            for (ProductVariantModel v : variants) {
                if (!v.getSize().trim().isEmpty()) {
                    distinct.add(v.getSize().trim());
                }
            }
            sizes = new ArrayList<>(distinct);
            Collections.sort(sizes, Comparator.comparingDouble(s -> {
                try {
                    return Double.parseDouble(s);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }));
        } else {
            sizes = allSizes;
        }
        return (sizes != null && !sizes.isEmpty())
                ? "EU " + sizes.get(0) + " - " + sizes.get(sizes.size() - 1)
                : DEFAULT_EU_SIZE_RANGE;
    }

    @NonNull
    public ProductModel withReviewStats(double nextRating, int nextReviewCount) {
        ProductModel copy = new ProductModel(
                id,
                name,
                brand,
                category,
                price,
                originalPrice,
                nextRating,
                Math.max(0, nextReviewCount),
                imageResId,
                thumbnailUrl,
                galleryUrls,
                weightGrams,
                heelDropMm,
                cushionLevel,
                surfaceType,
                description,
                colorNames,
                colorHexes,
                allSizes,
                outOfStockSizes,
                active,
                createdAtMillis
        );
        if (!variants.isEmpty()) {
            copy.setVariants(variants);
        }
        return copy;
    }
}
