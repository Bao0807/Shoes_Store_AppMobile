package com.example.shoes_store.module.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CategoryStyleHelper;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.example.shoes_store.core.UserRoleHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chuyển đổi dữ liệu Firestore cho Hồ sơ người dùng (`users/{uid}`) và Sổ địa chỉ (`addresses`).
 */
public final class ProfileMapper {

    public static final String FIELD_UID = "uid";
    public static final String FIELD_FULL_NAME = "fullName";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PHONE = "phone";
    public static final String FIELD_AVATAR_URL = "avatarUrl";
    public static final String FIELD_PREFERRED_SIZE = "preferredSize";
    public static final String FIELD_RUNNING_GOAL = "runningGoal";
    public static final String FIELD_ROLE = "role";
    public static final String FIELD_RECENT_SEARCHES = "recentSearches";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";

    public static final String FIELD_ADDRESS_ID = "addressId";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_RECEIVER_NAME = "receiverName";
    public static final String FIELD_ADDRESS_DETAIL = "addressDetail";
    public static final String FIELD_LABEL = "label";
    public static final String FIELD_IS_DEFAULT = "isDefault";

    private ProfileMapper() {
    }

    @NonNull
    public static ProfileModel profileFromSnapshot(@NonNull DocumentSnapshot doc, @NonNull String uid) {
        String avatarUrl = FirestoreReadUtil.readString(doc, FIELD_AVATAR_URL, "");
        String email = FirestoreReadUtil.readString(doc, FIELD_EMAIL, "");
        String rawRole = FirestoreReadUtil.readString(doc, FIELD_ROLE, UserRoleHelper.ROLE_USERS);
        String resolvedRole = UserRoleHelper.resolveRole(rawRole, email);
        return new ProfileModel(
                uid,
                FirestoreReadUtil.readString(doc, FIELD_FULL_NAME, "Runner"),
                email,
                FirestoreReadUtil.readString(doc, FIELD_PHONE, ""),
                FirestoreReadUtil.readString(doc, FIELD_PREFERRED_SIZE, "42"),
                FirestoreReadUtil.readString(doc, FIELD_RUNNING_GOAL, CategoryStyleHelper.CAT_DAILY),
                resolvedRole,
                R.drawable.img_avatar_runner,
                avatarUrl.isEmpty() ? null : avatarUrl
        );
    }

    @NonNull
    public static ProfileModel defaultProfile(@NonNull String uid, @NonNull FirebaseUser authUser) {
        String email = authUser.getEmail() != null ? authUser.getEmail() : "";
        return new ProfileModel(
                uid,
                resolveFallbackName(authUser),
                email,
                "",
                "42",
                CategoryStyleHelper.CAT_DAILY,
                UserRoleHelper.resolveRole(null, email),
                R.drawable.img_avatar_runner,
                null
        );
    }

    @NonNull
    public static String resolveFallbackName(@NonNull FirebaseUser authUser) {
        String display = authUser.getDisplayName();
        if (display != null && !display.trim().isEmpty()) {
            return display.trim();
        }
        String email = authUser.getEmail();
        if (email != null && email.contains("@")) {
            String prefix = email.substring(0, email.indexOf('@')).trim();
            if (!prefix.isEmpty()) {
                return prefix.substring(0, 1).toUpperCase(Locale.ROOT) + prefix.substring(1);
            }
        }
        return "Runner";
    }

    @NonNull
    public static Map<String, Object> newUserToMap(
            @NonNull String uid,
            @NonNull String fullName,
            @NonNull String email,
            @NonNull String phone,
            @NonNull List<String> recentSearches,
            @NonNull Timestamp now
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_UID, uid);
        data.put(FIELD_FULL_NAME, fullName);
        data.put(FIELD_EMAIL, email);
        data.put(FIELD_PHONE, phone);
        data.put(FIELD_AVATAR_URL, "");
        data.put(FIELD_PREFERRED_SIZE, "42");
        data.put(FIELD_RUNNING_GOAL, CategoryStyleHelper.CAT_DAILY);
        data.put(FIELD_ROLE, UserRoleHelper.resolveRole(null, email));
        data.put(FIELD_RECENT_SEARCHES, new ArrayList<>(recentSearches));
        data.put(FIELD_CREATED_AT, now);
        data.put(FIELD_UPDATED_AT, now);
        return data;
    }

    @NonNull
    public static Map<String, Object> profileUpdateToMap(
            @NonNull String uid,
            @NonNull String fullName,
            @NonNull String phone,
            @NonNull String email,
            @NonNull String preferredSize,
            @NonNull String runningGoal,
            @Nullable String avatarUrl,
            @NonNull Timestamp now
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_UID, uid);
        updates.put(FIELD_FULL_NAME, fullName);
        updates.put(FIELD_PHONE, phone);
        updates.put(FIELD_EMAIL, email);
        updates.put(FIELD_PREFERRED_SIZE, preferredSize);
        updates.put(FIELD_RUNNING_GOAL, runningGoal);
        updates.put(FIELD_AVATAR_URL, avatarUrl != null ? avatarUrl : "");
        updates.put(FIELD_UPDATED_AT, now);
        return updates;
    }

    @NonNull
    public static ProfileAddressModel addressFromSnapshot(@NonNull DocumentSnapshot doc) {
        return new ProfileAddressModel(
                FirestoreReadUtil.readString(doc, FIELD_ADDRESS_ID, doc.getId()),
                FirestoreReadUtil.readString(doc, FIELD_RECEIVER_NAME, ""),
                FirestoreReadUtil.readString(doc, FIELD_PHONE, ""),
                FirestoreReadUtil.readString(doc, FIELD_ADDRESS_DETAIL, ""),
                FirestoreReadUtil.readString(doc, FIELD_LABEL, "Nhà riêng"),
                FirestoreReadUtil.readBoolean(doc, FIELD_IS_DEFAULT, false)
        );
    }

    @NonNull
    public static Map<String, Object> addressToMap(
            @NonNull String addressId,
            @NonNull String uid,
            @NonNull String receiverName,
            @NonNull String phone,
            @NonNull String addressDetail,
            @NonNull String label,
            boolean isDefault
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_ADDRESS_ID, addressId);
        data.put(FIELD_USER_ID, uid);
        data.put(FIELD_RECEIVER_NAME, receiverName);
        data.put(FIELD_PHONE, phone);
        data.put(FIELD_ADDRESS_DETAIL, addressDetail);
        data.put(FIELD_LABEL, label);
        data.put(FIELD_IS_DEFAULT, isDefault);
        return data;
    }
}
