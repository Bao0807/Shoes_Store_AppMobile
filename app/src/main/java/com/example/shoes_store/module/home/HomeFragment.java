package com.example.shoes_store.module.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.shoes_store.MainActivity;
import com.example.shoes_store.R;
import com.example.shoes_store.core.AvatarImageHelper;
import com.example.shoes_store.core.CategoryStyleHelper;
import com.example.shoes_store.module.auth.AuthLoginActivity;
import com.example.shoes_store.module.product.ProductDetailActivity;
import com.example.shoes_store.module.product.ProductModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;

/**
 * Tab 1 - Trang chủ shop giày chạy bộ.
 */
public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;
    private HomeShoeAdapter shoeAdapter;
    private HomeBannerAdapter bannerAdapter;
    private NestedScrollView scrollView;
    private View refreshProgress;
    private ViewPager2 bannerPager;
    private LinearLayout bannerDotsLayout;
    private Chip chipCatAll;
    private Chip chipCatDaily;
    private Chip chipCatRace;
    private Chip chipCatTempo;
    private Chip chipCatTrail;
    private Chip chipCatRecovery;
    private TextView sectionTitleView;
    private TextView productCountView;
    private TextView clearFilterView;
    private View filterDotView;
    private View emptyStateView;
    private RecyclerView shoeRecyclerView;

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingRefreshTask;
    private boolean isRefreshing = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        ShapeableImageView avatarView = view.findViewById(R.id.home_avatar);
        View searchBar = view.findViewById(R.id.home_search_bar);
        View filterBtn = view.findViewById(R.id.home_filter_btn);
        scrollView = view.findViewById(R.id.home_scroll_view);
        refreshProgress = view.findViewById(R.id.home_refresh_progress);
        filterDotView = view.findViewById(R.id.home_filter_dot);
        bannerPager = view.findViewById(R.id.home_banner_pager);
        bannerDotsLayout = view.findViewById(R.id.home_banner_dots);
        chipCatAll = view.findViewById(R.id.chip_cat_all);
        chipCatDaily = view.findViewById(R.id.chip_cat_daily);
        chipCatRace = view.findViewById(R.id.chip_cat_race);
        chipCatTempo = view.findViewById(R.id.chip_cat_tempo);
        chipCatTrail = view.findViewById(R.id.chip_cat_trail);
        chipCatRecovery = view.findViewById(R.id.chip_cat_recovery);
        sectionTitleView = view.findViewById(R.id.home_section_title);
        productCountView = view.findViewById(R.id.home_product_count);
        clearFilterView = view.findViewById(R.id.home_clear_filter);
        shoeRecyclerView = view.findViewById(R.id.home_shoe_recycler);
        emptyStateView = view.findViewById(R.id.home_empty_state);

        avatarView.setOnClickListener(v -> {
            if (!viewModel.isSignedIn()) {
                Toast.makeText(requireContext(), R.string.auth_login_required, Toast.LENGTH_SHORT).show();
                AuthLoginActivity.openForTab(requireContext(), R.id.nav_profile);
                return;
            }
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_profile);
            }
        });

        searchBar.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HomeSearchActivity.class)));

        filterBtn.setOnClickListener(v -> openFilterSheet());
        clearFilterView.setOnClickListener(v -> viewModel.resetFilters());
        view.findViewById(R.id.home_empty_reset_btn).setOnClickListener(v -> viewModel.resetFilters());

        setupBanners();
        setupCategoryChips();
        setupShoesGrid();
        observeData(avatarView);
    }

    private void setupBanners() {
        bannerAdapter = new HomeBannerAdapter(null, banner ->
                viewModel.setCategory(banner.getTargetCategory()));
        bannerPager.setAdapter(bannerAdapter);
        bannerPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateBannerDots(bannerAdapter.getItemCount(), position);
            }
        });
    }

    private void updateBannerDots(int count, int activeIndex) {
        bannerDotsLayout.removeAllViews();
        int activeWidth = Math.round(18 * getResources().getDisplayMetrics().density);
        int dotSize = Math.round(6 * getResources().getDisplayMetrics().density);
        int margin = Math.round(3 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    i == activeIndex ? activeWidth : dotSize,
                    dotSize
            );
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == activeIndex ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
            bannerDotsLayout.addView(dot);
        }
    }

    private void setupCategoryChips() {
        chipCatAll.setOnClickListener(v -> viewModel.clearCategories());
        chipCatDaily.setOnClickListener(v -> viewModel.toggleCategory(CategoryStyleHelper.CAT_DAILY));
        chipCatRace.setOnClickListener(v -> viewModel.toggleCategory(CategoryStyleHelper.CAT_RACE));
        chipCatTempo.setOnClickListener(v -> viewModel.toggleCategory(CategoryStyleHelper.CAT_TEMPO));
        chipCatTrail.setOnClickListener(v -> viewModel.toggleCategory(CategoryStyleHelper.CAT_TRAIL));
        chipCatRecovery.setOnClickListener(v -> viewModel.toggleCategory(CategoryStyleHelper.CAT_RECOVERY));
    }

    private void setupShoesGrid() {
        shoeAdapter = new HomeShoeAdapter(new HomeShoeAdapter.OnShoeInteractionListener() {
            @Override
            public void onShoeClick(ProductModel product) {
                Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onFavoriteToggle(ProductModel product) {
                if (!viewModel.isSignedIn()) {
                    Toast.makeText(requireContext(), R.string.auth_login_required, Toast.LENGTH_SHORT).show();
                    AuthLoginActivity.open(requireContext());
                    return;
                }
                boolean added = viewModel.toggleFavorite(product.getId());
                String msg = added ? "Đã thêm vào Yêu thích" : "Đã bỏ khỏi Yêu thích";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
        shoeRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        shoeRecyclerView.setAdapter(shoeAdapter);
    }

    private void observeData(ShapeableImageView avatarView) {
        viewModel.getBannersLiveData().observe(getViewLifecycleOwner(), banners -> {
            bannerAdapter.submitList(banners);
            int count = banners != null ? banners.size() : 0;
            updateBannerDots(count, Math.min(bannerPager.getCurrentItem(), Math.max(0, count - 1)));
        });

        viewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                AvatarImageHelper.bindAvatar(avatarView, profile.getAvatarUri(), profile.getAvatarResId());
            } else {
                avatarView.setImageResource(R.drawable.img_avatar_runner);
            }
        });

        viewModel.getFavoriteIdsLiveData().observe(getViewLifecycleOwner(), ids ->
                shoeAdapter.setFavoriteIds(ids));

        viewModel.getDisplayedProductsLiveData().observe(getViewLifecycleOwner(), products -> {
            shoeAdapter.submitList(products);
            int count = products != null ? products.size() : 0;
            productCountView.setText(count + " sản phẩm");
            emptyStateView.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            shoeRecyclerView.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        });

        viewModel.getFilterStateLiveData().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            syncCategoryChips(state);
            filterDotView.setVisibility(state.hasAdvancedFilter() ? View.VISIBLE : View.GONE);
            clearFilterView.setVisibility(state.hasAnyFilter() ? View.VISIBLE : View.GONE);
            if (state.isAllCategoriesSelected()) {
                sectionTitleView.setText(R.string.home_section_suggest);
            } else {
                sectionTitleView.setText("Giày chạy " + TextUtils.join(", ", state.getSelectedCategories()));
            }
        });
    }

    private void syncCategoryChips(HomeFilterState state) {
        boolean allSelected = state.isAllCategoriesSelected();
        setChipChecked(chipCatAll, allSelected);
        setChipChecked(chipCatDaily, !allSelected && state.isCategorySelected(CategoryStyleHelper.CAT_DAILY));
        setChipChecked(chipCatRace, !allSelected && state.isCategorySelected(CategoryStyleHelper.CAT_RACE));
        setChipChecked(chipCatTempo, !allSelected && state.isCategorySelected(CategoryStyleHelper.CAT_TEMPO));
        setChipChecked(chipCatTrail, !allSelected && state.isCategorySelected(CategoryStyleHelper.CAT_TRAIL));
        setChipChecked(chipCatRecovery, !allSelected && state.isCategorySelected(CategoryStyleHelper.CAT_RECOVERY));
    }

    private void setChipChecked(Chip chip, boolean checked) {
        if (chip.isChecked() != checked) {
            chip.setChecked(checked);
        }
    }

    private void openFilterSheet() {
        HomeFilterBottomSheet sheet = HomeFilterBottomSheet.newInstance(
                viewModel.getCurrentFilterState(),
                updated -> viewModel.updateFilterState(updated)
        );
        sheet.show(getChildFragmentManager(), "home_filter_sheet");
    }

    public void scrollToTopAndReload() {
        if (getView() == null || viewModel == null) {
            return;
        }
        scrollView.smoothScrollTo(0, 0);
        if (bannerPager != null && bannerPager.getCurrentItem() != 0) {
            bannerPager.setCurrentItem(0, true);
        }
        if (isRefreshing) {
            return;
        }
        isRefreshing = true;
        refreshProgress.setVisibility(View.VISIBLE);
        shoeRecyclerView.animate().cancel();
        shoeRecyclerView.animate().alpha(0.45f).setDuration(150).start();

        pendingRefreshTask = () -> {
            if (!isAdded() || getView() == null) {
                isRefreshing = false;
                return;
            }
            viewModel.reload();
            refreshProgress.setVisibility(View.GONE);
            shoeRecyclerView.animate().alpha(1.0f).setDuration(180).start();
            isRefreshing = false;
        };
        refreshHandler.postDelayed(pendingRefreshTask, 450);
    }

    @Override
    public void onDestroyView() {
        if (pendingRefreshTask != null) {
            refreshHandler.removeCallbacks(pendingRefreshTask);
            pendingRefreshTask = null;
        }
        isRefreshing = false;
        super.onDestroyView();
    }
}
