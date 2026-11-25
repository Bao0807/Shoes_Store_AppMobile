package com.example.shoes_store.core;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Cung cấp FirebaseAuth, FirebaseFirestore và hằng số đường dẫn collection thống nhất.
 */
public final class FirebaseProviderHelper {

    public static final String COL_USERS = "users";
    public static final String COL_BANNERS = "banners";
    public static final String COL_PRODUCTS = "products";
    public static final String COL_VOUCHERS = "vouchers";
    public static final String COL_ORDERS = "orders";
    public static final String COL_PAYMENTS = "payments";

    public static final String SUB_ADDRESSES = "addresses";
    public static final String SUB_WISHLIST = "wishlist";
    public static final String SUB_CART = "cart";
    public static final String SUB_NOTIFICATIONS = "notifications";
    public static final String SUB_VARIANTS = "variants";
    public static final String SUB_REVIEWS = "reviews";

    private static volatile FirebaseProviderHelper instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    private FirebaseProviderHelper() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public static FirebaseProviderHelper getInstance() {
        if (instance == null) {
            synchronized (FirebaseProviderHelper.class) {
                if (instance == null) {
                    instance = new FirebaseProviderHelper();
                }
            }
        }
        return instance;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    @Nullable
    public String getCurrentUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }
}
