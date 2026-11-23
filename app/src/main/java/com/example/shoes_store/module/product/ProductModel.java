package com.example.shoes_store.module.product;

import java.util.List;
import java.util.Set;

/**
 * Model sản phẩm giày chạy bộ kèm thông số chuyên biệt cho runner.
 */
public class ProductModel {

    private final String id;
    private final String name;
    private final String brand;
    private final String category;
    private final long price;
    private final long originalPrice;
    private final double rating;
    private final int reviewCount;
    private final int imageResId;
    private final int[] galleryResIds;
    private final String weightGrams;
    private final String heelDropMm;
    private final String cushionLevel;
    private final String surfaceType;
    private final String description;
    private final List<String> colorNames;
    private final List<String> colorHexes;
    private final List<String> allSizes;
    private final Set<String> outOfStockSizes;
    private final List<ProductReviewModel> reviews;

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
            int[] galleryResIds,
            String weightGrams,
            String heelDropMm,
            String cushionLevel,
            String surfaceType,
            String description,
            List<String> colorNames,
            List<String> colorHexes,
            List<String> allSizes,
            Set<String> outOfStockSizes,
            List<ProductReviewModel> reviews
    ) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.price = price;
        this.originalPrice = originalPrice;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.imageResId = imageResId;
        this.galleryResIds = galleryResIds;
        this.weightGrams = weightGrams;
        this.heelDropMm = heelDropMm;
        this.cushionLevel = cushionLevel;
        this.surfaceType = surfaceType;
        this.description = description;
        this.colorNames = colorNames;
        this.colorHexes = colorHexes;
        this.allSizes = allSizes;
        this.outOfStockSizes = outOfStockSizes;
        this.reviews = reviews;
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

    public int[] getGalleryResIds() {
        return galleryResIds;
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

    public List<ProductReviewModel> getReviews() {
        return reviews;
    }
}
