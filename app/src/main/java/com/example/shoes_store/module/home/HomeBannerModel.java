package com.example.shoes_store.module.home;

/**
 * Model cho banner quảng bá bộ sưu tập giày chạy trên Trang chủ.
 */
public class HomeBannerModel {

    private final String badge;
    private final String title;
    private final String subtitle;
    private final String ctaLabel;
    private final String targetCategory;
    private final int imageResId;

    public HomeBannerModel(
            String badge,
            String title,
            String subtitle,
            String ctaLabel,
            String targetCategory,
            int imageResId
    ) {
        this.badge = badge;
        this.title = title;
        this.subtitle = subtitle;
        this.ctaLabel = ctaLabel;
        this.targetCategory = targetCategory;
        this.imageResId = imageResId;
    }

    public String getBadge() {
        return badge;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getCtaLabel() {
        return ctaLabel;
    }

    public String getTargetCategory() {
        return targetCategory;
    }

    public int getImageResId() {
        return imageResId;
    }
}
