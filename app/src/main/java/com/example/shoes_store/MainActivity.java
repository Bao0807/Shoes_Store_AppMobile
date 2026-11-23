package com.example.shoes_store;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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

/**
 * Activity chính chứa khung điều hướng Floating Bottom Navigation với thanh trượt mượt.
 */
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_TAB = "extra_target_tab";
    private static final String STATE_ACTIVE_TAG = "state_active_tag";

    private static final String TAG_HOME = "tab_home";
    private static final String TAG_WISHLIST = "tab_wishlist";
    private static final String TAG_CART = "tab_cart";
    private static final String TAG_NOTIFICATION = "tab_notification";
    private static final String TAG_PROFILE = "tab_profile";

    private static final int[] TAB_IDS = {
            R.id.nav_home,
            R.id.nav_wishlist,
            R.id.nav_cart,
            R.id.nav_notification,
            R.id.nav_profile
    };

    private final PathInterpolator pillGlideInterpolator = new PathInterpolator(0.22f, 1.0f, 0.36f, 1.0f);
    private final OvershootInterpolator iconPopInterpolator = new OvershootInterpolator(1.6f);
    private final DecelerateInterpolator iconEaseInterpolator = new DecelerateInterpolator();
    private final ValueAnimator[] colorAnimators = new ValueAnimator[TAB_IDS.length];

    private View slidingIndicator;
    private View[] tabContainers;
    private ImageView[] tabIcons;
    private TextView cartBadgeView;
    private TextView notificationBadgeView;
    private String activeTag = TAG_HOME;
    private int activeIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);

        UiInsetHelper.applySystemBarInsets(findViewById(R.id.main_fragment_container));

        setupFloatingNavBar();

        if (savedInstanceState != null) {
            activeTag = savedInstanceState.getString(STATE_ACTIVE_TAG, TAG_HOME);
            selectTab(menuIdForTag(activeTag), false);
        } else {
            selectTab(R.id.nav_home, false);
        }

        observeBadges();
        handleTargetTabIntent(getIntent());
    }

    private void setupFloatingNavBar() {
        View navBar = findViewById(R.id.main_bottom_nav_bar);
        View navItemsRow = findViewById(R.id.main_bottom_nav_items);
        slidingIndicator = findViewById(R.id.nav_sliding_indicator);

        ViewGroup.MarginLayoutParams barParams = (ViewGroup.MarginLayoutParams) navBar.getLayoutParams();
        final int baseBottomMargin = barParams.bottomMargin;
        final int baseLeftMargin = barParams.leftMargin;
        final int baseRightMargin = barParams.rightMargin;

        ViewCompat.setOnApplyWindowInsetsListener(navBar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.bottomMargin = baseBottomMargin + systemBars.bottom;
            lp.leftMargin = baseLeftMargin + systemBars.left;
            lp.rightMargin = baseRightMargin + systemBars.right;
            v.setLayoutParams(lp);
            return insets;
        });

        tabContainers = new View[]{
                findViewById(R.id.nav_home),
                findViewById(R.id.nav_wishlist),
                findViewById(R.id.nav_cart),
                findViewById(R.id.nav_notification),
                findViewById(R.id.nav_profile)
        };
        tabIcons = new ImageView[]{
                findViewById(R.id.nav_icon_home),
                findViewById(R.id.nav_icon_wishlist),
                findViewById(R.id.nav_icon_cart),
                findViewById(R.id.nav_icon_notification),
                findViewById(R.id.nav_icon_profile)
        };
        cartBadgeView = findViewById(R.id.nav_badge_cart);
        notificationBadgeView = findViewById(R.id.nav_badge_notification);

        navItemsRow.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                syncIndicatorPosition(false));

        for (int i = 0; i < TAB_IDS.length; i++) {
            final int menuId = TAB_IDS[i];
            tabContainers[i].setOnClickListener(v -> selectTab(menuId, true));
        }
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
        selectTab(menuItemId, true);
    }

    private void selectTab(int menuItemId, boolean animate) {
        int targetIndex = indexForMenuId(menuItemId);
        String targetTag = tagForMenuId(menuItemId);
        boolean fragmentMissing = getSupportFragmentManager().findFragmentByTag(targetTag) == null;

        if (animate && targetIndex == activeIndex && !fragmentMissing) {
            if (menuItemId == R.id.nav_home) {
                ImageView homeIcon = tabIcons[targetIndex];
                homeIcon.animate().cancel();
                homeIcon.setScaleX(0.90f);
                homeIcon.setScaleY(0.90f);
                homeIcon.animate()
                        .scaleX(1.14f)
                        .scaleY(1.14f)
                        .setDuration(200)
                        .setInterpolator(iconPopInterpolator)
                        .start();
                Fragment homeFragment = getSupportFragmentManager().findFragmentByTag(TAG_HOME);
                if (homeFragment instanceof HomeFragment) {
                    ((HomeFragment) homeFragment).scrollToTopAndReload();
                }
            }
            return;
        }

        int previousIndex = activeIndex;
        activeIndex = targetIndex;

        if (!targetTag.equals(activeTag) || fragmentMissing) {
            switchTab(targetTag, animate);
        }

        syncIndicatorPosition(animate);
        animateTabIcons(previousIndex, targetIndex, animate);
    }

    private void syncIndicatorPosition(boolean animate) {
        if (slidingIndicator == null || tabContainers == null) {
            return;
        }
        View targetSlot = tabContainers[activeIndex];
        int slotWidth = targetSlot.getWidth();
        if (slotWidth <= 0) {
            return;
        }

        ViewGroup.LayoutParams lp = slidingIndicator.getLayoutParams();
        if (lp.width != slotWidth) {
            lp.width = slotWidth;
            slidingIndicator.setLayoutParams(lp);
        }

        float targetX = targetSlot.getLeft();
        slidingIndicator.animate().cancel();
        if (animate) {
            slidingIndicator.animate()
                    .translationX(targetX)
                    .setDuration(260)
                    .setInterpolator(pillGlideInterpolator)
                    .start();
        } else {
            slidingIndicator.setTranslationX(targetX);
        }
    }

    private void animateTabIcons(int previousIndex, int targetIndex, boolean animate) {
        int activeColor = ContextCompat.getColor(this, R.color.brand_accent);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        for (int i = 0; i < TAB_IDS.length; i++) {
            boolean isActive = i == targetIndex;
            ImageView icon = tabIcons[i];
            icon.animate().cancel();

            if (!animate) {
                if (colorAnimators[i] != null) {
                    colorAnimators[i].cancel();
                }
                icon.setColorFilter(isActive ? activeColor : inactiveColor);
                float scale = isActive ? 1.14f : 1.0f;
                icon.setScaleX(scale);
                icon.setScaleY(scale);
                continue;
            }

            if (isActive) {
                animateIconColor(i, inactiveColor, activeColor, 220);
                icon.setScaleX(0.88f);
                icon.setScaleY(0.88f);
                icon.animate()
                        .scaleX(1.14f)
                        .scaleY(1.14f)
                        .setDuration(240)
                        .setInterpolator(iconPopInterpolator)
                        .start();
            } else {
                if (i == previousIndex) {
                    animateIconColor(i, activeColor, inactiveColor, 180);
                } else {
                    if (colorAnimators[i] != null) {
                        colorAnimators[i].cancel();
                    }
                    icon.setColorFilter(inactiveColor);
                }
                icon.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(180)
                        .setInterpolator(iconEaseInterpolator)
                        .start();
            }
        }
    }

    private void animateIconColor(int slotIndex, int fromColor, int toColor, long durationMs) {
        if (colorAnimators[slotIndex] != null) {
            colorAnimators[slotIndex].cancel();
        }
        ImageView icon = tabIcons[slotIndex];
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
        animator.setDuration(durationMs);
        animator.addUpdateListener(anim -> icon.setColorFilter((int) anim.getAnimatedValue()));
        colorAnimators[slotIndex] = animator;
        animator.start();
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
        store.getCartTotalCountLiveData().observe(this, count -> updateBadgeView(cartBadgeView, count));
        store.getUnreadNotificationCountLiveData().observe(this, unread -> updateBadgeView(notificationBadgeView, unread));
    }

    private void updateBadgeView(TextView badgeView, Integer count) {
        if (count != null && count > 0) {
            badgeView.setVisibility(View.VISIBLE);
            badgeView.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            badgeView.setVisibility(View.GONE);
        }
    }

    private void switchTab(@NonNull String targetTag, boolean animate) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();

        Fragment current = fm.findFragmentByTag(activeTag);
        if (animate && current != null && current.isAdded() && !targetTag.equals(activeTag)) {
            tx.setCustomAnimations(R.anim.tab_fade_in, R.anim.tab_fade_out);
        }

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

    private int indexForMenuId(int menuId) {
        for (int i = 0; i < TAB_IDS.length; i++) {
            if (TAB_IDS[i] == menuId) {
                return i;
            }
        }
        return 0;
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

    private int menuIdForTag(@NonNull String tag) {
        switch (tag) {
            case TAG_WISHLIST:
                return R.id.nav_wishlist;
            case TAG_CART:
                return R.id.nav_cart;
            case TAG_NOTIFICATION:
                return R.id.nav_notification;
            case TAG_PROFILE:
                return R.id.nav_profile;
            case TAG_HOME:
            default:
                return R.id.nav_home;
        }
    }
}
