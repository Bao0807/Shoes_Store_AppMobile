package com.example.shoes_store.module.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.shoes_store.MainActivity;
import com.example.shoes_store.R;
import com.example.shoes_store.module.product.ProductDetailActivity;
import com.example.shoes_store.module.product.ProductModel;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

/**
 * Tab 1 - Trang chủ shop giày chạy bộ.
 */
public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;
    private HomeShoeAdapter shoeAdapter;
    private ViewPager2 bannerPager;
    private LinearLayout bannerDotsLayout;
    private ChipGroup categoryChipGroup;
    private TextView sectionTitleView;
    private TextView productCountView;
    private TextView clearFilterView;
    private View filterDotView;
    private View emptyStateView;
    private RecyclerView shoeRecyclerView;

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
        filterDotView = view.findViewById(R.id.home_filter_dot);
        bannerPager = view.findViewById(R.id.home_banner_pager);
        bannerDotsLayout = view.findViewById(R.id.home_banner_dots);
        categoryChipGroup = view.findViewById(R.id.home_category_chip_group);
        sectionTitleView = view.findViewById(R.id.home_section_title);
        productCountView = view.findViewById(R.id.home_product_count);
        clearFilterView = view.findViewById(R.id.home_clear_filter);
        shoeRecyclerView = view.findViewById(R.id.home_shoe_recycler);
        emptyStateView = view.findViewById(R.id.home_empty_state);

        avatarView.setOnClickListener(v -> {
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
        List<HomeBannerModel> banners = viewModel.getBanners();
        HomeBannerAdapter bannerAdapter = new HomeBannerAdapter(banners, banner ->
                viewModel.setCategory(banner.getTargetCategory()));
        bannerPager.setAdapter(bannerAdapter);
        updateBannerDots(banners.size(), 0);
        bannerPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateBannerDots(banners.size(), position);
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
        categoryChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chip_cat_daily) {
                viewModel.setCategory("Daily");
            } else if (id == R.id.chip_cat_race) {
                viewModel.setCategory("Race");
            } else if (id == R.id.chip_cat_tempo) {
                viewModel.setCategory("Tempo");
            } else if (id == R.id.chip_cat_trail) {
                viewModel.setCategory("Trail");
            } else if (id == R.id.chip_cat_recovery) {
                viewModel.setCategory("Recovery");
            } else {
                viewModel.setCategory(HomeFilterState.ALL);
            }
        });
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
                boolean added = viewModel.toggleFavorite(product.getId());
                String msg = added ? "Đã thêm vào Yêu thích" : "Đã bỏ khỏi Yêu thích";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
        shoeRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        shoeRecyclerView.setAdapter(shoeAdapter);
    }

    private void observeData(ShapeableImageView avatarView) {
        viewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                avatarView.setImageResource(profile.getAvatarResId());
            }
        });

        viewModel.getFavoriteIdsLiveData().observe(getViewLifecycleOwner(), ids ->
                shoeAdapter.setFavoriteIds(ids));

        viewModel.getDisplayedProductsLiveData().observe(getViewLifecycleOwner(), products -> {
            shoeAdapter.submitProducts(products);
            int count = products != null ? products.size() : 0;
            productCountView.setText(count + " sản phẩm");
            emptyStateView.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            shoeRecyclerView.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        });

        viewModel.getFilterStateLiveData().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            syncCategoryChip(state.getCategory());
            filterDotView.setVisibility(state.hasAdvancedFilter() ? View.VISIBLE : View.GONE);
            clearFilterView.setVisibility(state.hasAnyFilter() ? View.VISIBLE : View.GONE);
            if (HomeFilterState.ALL.equals(state.getCategory())) {
                sectionTitleView.setText(R.string.home_section_suggest);
            } else {
                sectionTitleView.setText("Giày chạy " + state.getCategory());
            }
        });
    }

    private void syncCategoryChip(String category) {
        int targetChipId = R.id.chip_cat_all;
        if ("Daily".equalsIgnoreCase(category)) {
            targetChipId = R.id.chip_cat_daily;
        } else if ("Race".equalsIgnoreCase(category)) {
            targetChipId = R.id.chip_cat_race;
        } else if ("Tempo".equalsIgnoreCase(category)) {
            targetChipId = R.id.chip_cat_tempo;
        } else if ("Trail".equalsIgnoreCase(category)) {
            targetChipId = R.id.chip_cat_trail;
        } else if ("Recovery".equalsIgnoreCase(category)) {
            targetChipId = R.id.chip_cat_recovery;
        }
        if (categoryChipGroup.getCheckedChipId() != targetChipId) {
            categoryChipGroup.check(targetChipId);
        }
    }

    private void openFilterSheet() {
        HomeFilterBottomSheet sheet = HomeFilterBottomSheet.newInstance(
                viewModel.getCurrentFilterState(),
                updated -> viewModel.updateFilterState(updated)
        );
        sheet.show(getChildFragmentManager(), "home_filter_sheet");
    }
}
