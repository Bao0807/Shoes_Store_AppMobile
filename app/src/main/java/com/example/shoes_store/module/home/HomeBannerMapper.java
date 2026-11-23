package com.example.shoes_store.module.home;

import androidx.annotation.NonNull;

import com.example.shoes_store.R;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Chuyển đổi DocumentSnapshot của collection `banners` sang {@link HomeBannerModel}.
 */
public final class HomeBannerMapper {

    public static final String FIELD_BANNER_ID = "bannerId";
    public static final String FIELD_BADGE = "badge";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_SUBTITLE = "subtitle";
    public static final String FIELD_CTA_LABEL = "ctaLabel";
    public static final String FIELD_TARGET_CATEGORY = "targetCategory";
    public static final String FIELD_IMAGE_URL = "imageUrl";
    public static final String FIELD_DISPLAY_ORDER = "displayOrder";
    public static final String FIELD_IS_ACTIVE = "isActive";

    private HomeBannerMapper() {
    }

    @NonNull
    public static HomeBannerModel fromSnapshot(@NonNull DocumentSnapshot doc) {
        return new HomeBannerModel(
                FirestoreReadUtil.readString(doc, FIELD_BANNER_ID, doc.getId()),
                FirestoreReadUtil.readString(doc, FIELD_BADGE, ""),
                FirestoreReadUtil.readString(doc, FIELD_TITLE, ""),
                FirestoreReadUtil.readString(doc, FIELD_SUBTITLE, ""),
                FirestoreReadUtil.readString(doc, FIELD_CTA_LABEL, ""),
                FirestoreReadUtil.readString(doc, FIELD_TARGET_CATEGORY, "Daily"),
                FirestoreReadUtil.readString(doc, FIELD_IMAGE_URL, ""),
                R.drawable.img_banner_race,
                FirestoreReadUtil.readInt(doc, FIELD_DISPLAY_ORDER, 0),
                FirestoreReadUtil.readBoolean(doc, FIELD_IS_ACTIVE, true)
        );
    }
}
