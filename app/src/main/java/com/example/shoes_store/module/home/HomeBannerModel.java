package com.example.shoes_store.module.home;

import androidx.annotation.DrawableRes;

import com.example.shoes_store.R;

/**
 * Model cho banner quảng bá bộ sưu tập giày chạy trên Trang chủ.
 */
public class HomeBannerModel {

    private final String bannerId;
    private final String badge;
    private final String title;
    private final String subtitle;
    private final String ctaLabel;
    private final String targetCategory;
    private final String imageUrl;
    @DrawableRes
    private final int imageResId;
    private final int displayOrder;
    private final boolean active;

    public HomeBannerModel(
            String badge,
            String title,
            String subtitle,
            String ctaLabel,
            String targetCategory,
            int imageResId
    ) {
        this("", badge, title, subtitle, ctaLabel, targetCategory, "", imageResId, 0, true);
    }

    public HomeBannerModel(
            String bannerId,
            String badge,
            String title,
            String subtitle,
            String ctaLabel,
            String targetCategory,
            String imageUrl,
            int imageResId,
            int displayOrder,
            boolean active
    ) {
        this.bannerId = bannerId != null ? bannerId : "";
        this.badge = badge != null ? badge : "";
        this.title = title != null ? title : "";
        this.subtitle = subtitle != null ? subtitle : "";
        this.ctaLabel = ctaLabel != null ? ctaLabel : "";
        this.targetCategory = targetCategory != null ? targetCategory : "";
        this.imageUrl = imageUrl != null ? imageUrl : "";
        this.imageResId = imageResId != 0 ? imageResId : R.drawable.img_banner_race;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public String getBannerId() {
        return bannerId;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public int getImageResId() {
        return imageResId;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }
}
