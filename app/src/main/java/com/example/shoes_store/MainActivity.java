package com.example.shoes_store;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.shoes_store.core.MockStoreHelper;
import com.example.shoes_store.core.UiInsetHelper;
import com.example.shoes_store.module.cart.CartFragment;
import com.example.shoes_store.module.home.HomeFragment;
import com.example.shoes_store.module.notification.NotificationFragment;
import com.example.shoes_store.module.profile.ProfileFragment;
import com.example.shoes_store.module.wishlist.WishlistFragment;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Activity chính chứa khung điều hướng Bottom Navigation 5 tab.
 */
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_TAB = "extra_target_tab";
    private static final String STATE_ACTIVE_TAG = "state_active_tag";

    private static final String TAG_HOME = "tab_home";
    private static final String TAG_WISHLIST = "tab_wishlist";
    private static final String TAG_CART = "tab_cart";
    private static final String TAG_NOTIFICATION = "tab_notification";
    private static final String TAG_PROFILE = "tab_profile";

    private BottomNavigationView bottomNav;
    private String activeTag = TAG_HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);
        UiInsetHelper.applyTopAndSideInsets(findViewById(R.id.main));

        bottomNav = findViewById(R.id.main_bottom_nav);

        if (savedInstanceState != null) {
            activeTag = savedInstanceState.getString(STATE_ACTIVE_TAG, TAG_HOME);
        } else {
            switchTab(TAG_HOME);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            String targetTag = tagForMenuId(item.getItemId());
            if (!targetTag.equals(activeTag)) {
                switchTab(targetTag);
            }
            return true;
        });

        observeBadges();
        handleTargetTabIntent(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleTargetTabIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ACTIVE_TAG, activeTag);
    }

    public void navigateToTab(int menuItemId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(menuItemId);
        }
    }

    private void handleTargetTabIntent(Intent intent) {
        if (intent != null && intent.hasExtra(EXTRA_TARGET_TAB)) {
            int menuId = intent.getIntExtra(EXTRA_TARGET_TAB, R.id.nav_home);
            intent.removeExtra(EXTRA_TARGET_TAB);
            navigateToTab(menuId);
        }
    }

    private void observeBadges() {
        MockStoreHelper store = MockStoreHelper.getInstance();
        int accentColor = ContextCompat.getColor(this, R.color.brand_accent);
        int whiteColor = ContextCompat.getColor(this, R.color.text_on_dark);

        store.getCartTotalCountLiveData().observe(this, count -> {
            BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_cart);
            badge.setBackgroundColor(accentColor);
            badge.setBadgeTextColor(whiteColor);
            if (count != null && count > 0) {
                badge.setVisible(true);
                badge.setNumber(count);
            } else {
                badge.setVisible(false);
                badge.clearNumber();
            }
        });

        store.getUnreadNotificationCountLiveData().observe(this, unread -> {
            BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_notification);
            badge.setBackgroundColor(accentColor);
            badge.setBadgeTextColor(whiteColor);
            if (unread != null && unread > 0) {
                badge.setVisible(true);
                badge.setNumber(unread);
            } else {
                badge.setVisible(false);
                badge.clearNumber();
            }
        });
    }

    private void switchTab(@NonNull String targetTag) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();

        Fragment current = fm.findFragmentByTag(activeTag);
        if (current != null && current.isAdded()) {
            tx.hide(current);
        }

        Fragment target = fm.findFragmentByTag(targetTag);
        if (target == null) {
            target = createFragmentForTag(targetTag);
            tx.add(R.id.main_fragment_container, target, targetTag);
        } else {
            tx.show(target);
        }

        tx.commit();
        activeTag = targetTag;
    }

    @NonNull
    private Fragment createFragmentForTag(@NonNull String tag) {
        switch (tag) {
            case TAG_WISHLIST:
                return new WishlistFragment();
            case TAG_CART:
                return new CartFragment();
            case TAG_NOTIFICATION:
                return new NotificationFragment();
            case TAG_PROFILE:
                return new ProfileFragment();
            case TAG_HOME:
            default:
                return new HomeFragment();
        }
    }

    @NonNull
    private String tagForMenuId(int menuId) {
        if (menuId == R.id.nav_wishlist) {
            return TAG_WISHLIST;
        } else if (menuId == R.id.nav_cart) {
            return TAG_CART;
        } else if (menuId == R.id.nav_notification) {
            return TAG_NOTIFICATION;
        } else if (menuId == R.id.nav_profile) {
            return TAG_PROFILE;
        }
        return TAG_HOME;
    }
}
