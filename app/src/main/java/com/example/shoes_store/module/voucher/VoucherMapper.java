package com.example.shoes_store.module.voucher;

import androidx.annotation.NonNull;

import com.example.shoes_store.core.FirestoreReadUtil;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Chuyển đổi dữ liệu Firestore collection `vouchers` sang {@link VoucherModel}.
 */
public final class VoucherMapper {

    public static final String FIELD_CODE = "code";
    public static final String FIELD_SHORT_BADGE = "shortBadge";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_DISCOUNT_AMOUNT = "discountAmount";
    public static final String FIELD_MIN_ORDER_AMOUNT = "minOrderAmount";
    public static final String FIELD_USAGE_LIMIT = "usageLimit";
    public static final String FIELD_USED_COUNT = "usedCount";
    public static final String FIELD_EXPIRY_DATE = "expiryDate";
    public static final String FIELD_IS_ACTIVE = "isActive";

    private VoucherMapper() {
    }

    @NonNull
    public static VoucherModel fromSnapshot(@NonNull DocumentSnapshot doc) {
        return new VoucherModel(
                FirestoreReadUtil.readString(doc, FIELD_CODE, doc.getId()),
                FirestoreReadUtil.readString(doc, FIELD_SHORT_BADGE, ""),
                FirestoreReadUtil.readString(doc, FIELD_TITLE, ""),
                FirestoreReadUtil.readString(doc, FIELD_DESCRIPTION, ""),
                FirestoreReadUtil.readLong(doc, FIELD_DISCOUNT_AMOUNT, 0L),
                FirestoreReadUtil.readLong(doc, FIELD_MIN_ORDER_AMOUNT, 0L),
                "",
                FirestoreReadUtil.readInt(doc, FIELD_USAGE_LIMIT, 0),
                FirestoreReadUtil.readInt(doc, FIELD_USED_COUNT, 0),
                FirestoreReadUtil.readMillis(doc, FIELD_EXPIRY_DATE),
                FirestoreReadUtil.readBoolean(doc, FIELD_IS_ACTIVE, true)
        );
    }
}
