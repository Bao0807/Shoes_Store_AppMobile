package com.example.shoes_store.module.product;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseActivity;
import com.example.shoes_store.module.auth.AuthLoginActivity;
import com.example.shoes_store.module.order.OrderCheckoutActivity;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.review.ReviewAdapter;
import com.example.shoes_store.module.review.ReviewDialog;
import com.example.shoes_store.module.review.ReviewModel;
import com.example.shoes_store.module.review.ReviewService;
import com.example.shoes_store.module.review.ReviewViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Màn hình Chi tiết giày chạy bộ (Gallery, Màu, Size EU theo biến thể tồn kho thực, Đánh giá người mua thực).
 */
public class ProductDetailActivity extends BaseActivity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private ProductViewModel viewModel;
    private ReviewViewModel reviewViewModel;
    private ProductDetailUiHelper uiHelper;
    private ProductModel product;
    private ProductGalleryAdapter galleryAdapter;
    private ProductSizeAdapter sizeAdapter;
    private ReviewAdapter reviewAdapter;

    private final List<ProductVariantModel> colorReps = new ArrayList<>();
    private List<String> currentGalleryUrls = new ArrayList<>();
    private List<OrderModel> latestOrders = new ArrayList<>();
    private List<ReviewModel> latestReviews = new ArrayList<>();
    @Nullable
    private String eligibleReviewOrderId;
    private String selectedColorName = "";
    private int selectedColorIndex = 0;
    private boolean variantsLoaded = false;
    private boolean reviewsLoaded = false;

    private NestedScrollView scrollView;
    private TabLayout infoTabs;
    private ViewPager2 galleryPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContent(R.layout.product_detail_activity, R.id.detail_top_bar, R.id.detail_bottom_bar);

        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        reviewViewModel = new ViewModelProvider(this).get(ReviewViewModel.class);
        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        product = viewModel.getProduct(productId);
        if (product == null) {
            finish();
            return;
        }

        uiHelper = new ProductDetailUiHelper(findViewById(R.id.product_detail_root));
        scrollView = findViewById(R.id.detail_scroll_view);
        infoTabs = findViewById(R.id.detail_info_tabs);
        galleryPager = findViewById(R.id.detail_gallery_pager);

        bindBackButton(R.id.detail_back_btn);
        findViewById(R.id.detail_favorite_btn).setOnClickListener(v -> {
            if (!ensureSignedIn()) {
                return;
            }
            boolean added = viewModel.toggleFavorite(product.getId());
            showToast(added ? "Đã thêm vào Yêu thích" : "Đã bỏ khỏi Yêu thích");
        });

        findViewById(R.id.detail_size_guide_btn).setOnClickListener(v ->
                new ProductSizeGuideBottomSheet().show(getSupportFragmentManager(), "size_guide"));

        findViewById(R.id.detail_rating_row).setOnClickListener(v -> jumpToInfoTab(3));
        findViewById(R.id.detail_write_review_btn).setOnClickListener(v -> openWriteReviewDialog());

        View.OnClickListener openDetailSpecs = v -> jumpToInfoTab(1);
        findViewById(R.id.detail_specs_more_btn).setOnClickListener(openDetailSpecs);
        findViewById(R.id.detail_highlight_specs_row).setOnClickListener(openDetailSpecs);

        uiHelper.bindProductInfo(product, reviewsLoaded, latestReviews);
        setupGallery();
        setupColorSwatches();
        setupSizeGrid();
        setupReviews();
        setupInfoTabs();
        setupBottomActions();
        observeData(product.getId());
        refreshReviewEligibility();
    }

    private boolean ensureSignedIn() {
        if (viewModel.isSignedIn()) {
            return true;
        }
        showToast(R.string.auth_login_required);
        AuthLoginActivity.open(this);
        return false;
    }

    private void observeData(@NonNull String productId) {
        viewModel.getProductsLiveData().observe(this, products -> {
            if (products == null) {
                return;
            }
            for (ProductModel item : products) {
                if (productId.equals(item.getId())) {
                    List<ProductVariantModel> currentVariants = new ArrayList<>(product.getVariants());
                    product = item;
                    if (!currentVariants.isEmpty() && product.getVariants().isEmpty()) {
                        product.setVariants(currentVariants);
                    }
                    uiHelper.bindProductInfo(product, reviewsLoaded, latestReviews);
                    syncGalleryIfChanged();
                    refreshReviewEligibility();
                    break;
                }
            }
        });

        viewModel.getFavoriteIdsLiveData().observe(this, ids ->
                uiHelper.bindFavoriteState(ids != null && ids.contains(productId)));

        viewModel.loadVariants(productId).observe(this, variants -> {
            if (variants == null) {
                return;
            }
            variantsLoaded = true;
            product.setVariants(variants);
            setupColorSwatches();
            refreshSizesForSelectedColor();
            uiHelper.bindStockStatus(variantsLoaded, getSelectedVariantOrNull());
            uiHelper.updateSpecSizeRange(product);
        });

        viewModel.getOrdersLiveData().observe(this, orders -> {
            latestOrders = orders != null ? orders : new ArrayList<>();
            refreshReviewEligibility();
        });

        reviewViewModel.loadReviews(productId).observe(this, loadedReviews -> {
            reviewsLoaded = true;
            latestReviews = loadedReviews != null ? loadedReviews : new ArrayList<>();
            reviewAdapter.submitList(latestReviews);
            uiHelper.bindReviewsEmpty(latestReviews.isEmpty());
            uiHelper.updateRatingSummary(
                    ReviewService.calculateAverageRatingFromReviews(latestReviews),
                    latestReviews.size()
            );
            refreshReviewEligibility();
        });
    }

    private void refreshReviewEligibility() {
        String uid = viewModel.getCurrentUid();
        eligibleReviewOrderId = ReviewService.findEligibleOrderIdForReview(
                uid,
                product.getId(),
                latestOrders,
                latestReviews
        );
        uiHelper.bindReviewEligibility(
                eligibleReviewOrderId,
                ReviewService.hasUserReviewedProduct(uid, latestReviews)
        );
    }

    private void jumpToInfoTab(int tabIndex) {
        TabLayout.Tab targetTab = infoTabs.getTabAt(tabIndex);
        if (targetTab != null) {
            targetTab.select();
        }
        scrollView.post(() -> scrollView.smoothScrollTo(0, infoTabs.getTop()));
    }

    private void setupInfoTabs() {
        uiHelper.showInfoTab(0);
        infoTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                uiHelper.showInfoTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void syncGalleryIfChanged() {
        if (galleryAdapter == null) {
            return;
        }
        List<String> newUrls = ProductDetailUiHelper.buildGalleryUrls(product);
        if (newUrls.equals(currentGalleryUrls)) {
            return;
        }
        currentGalleryUrls = newUrls;
        galleryAdapter.submitList(newUrls);
        int count = galleryAdapter.getItemCount();
        uiHelper.renderGalleryDots(count, Math.min(galleryPager.getCurrentItem(), Math.max(0, count - 1)));
    }

    private void setupGallery() {
        currentGalleryUrls = ProductDetailUiHelper.buildGalleryUrls(product);
        galleryAdapter = new ProductGalleryAdapter(currentGalleryUrls, product.getImageResId());
        galleryPager.setAdapter(galleryAdapter);
        uiHelper.renderGalleryDots(galleryAdapter.getItemCount(), 0);
        galleryPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                uiHelper.renderGalleryDots(galleryAdapter.getItemCount(), position);
            }
        });
    }

    private void setupColorSwatches() {
        colorReps.clear();
        colorReps.addAll(product.getColorRepresentatives());
        if (colorReps.isEmpty()) {
            return;
        }
        if (selectedColorIndex >= colorReps.size()) {
            selectedColorIndex = 0;
        }
        selectedColorName = colorReps.get(selectedColorIndex).getColorName();
        renderColorSwatches();
    }

    private void renderColorSwatches() {
        uiHelper.renderColorSwatches(colorReps, selectedColorIndex, selectedColorName, (index, rep) -> {
            selectedColorIndex = index;
            selectedColorName = rep.getColorName();
            renderColorSwatches();
            refreshSizesForSelectedColor();
            if (!rep.getImageUrl().isEmpty() && galleryAdapter.getItemCount() > 0) {
                int matchedPage = currentGalleryUrls.indexOf(rep.getImageUrl());
                int targetPage = matchedPage >= 0
                        ? matchedPage
                        : Math.min(index, galleryAdapter.getItemCount() - 1);
                galleryPager.setCurrentItem(targetPage, true);
            }
        });
    }

    private void setupSizeGrid() {
        List<String> sizes = product.getSizesForColor(selectedColorName);
        Set<String> outOfStock = product.getOutOfStockSizesForColor(selectedColorName);
        String defaultSize = ProductDetailUiHelper.findFirstAvailableSize(sizes, outOfStock);
        RecyclerView sizeRecycler = findViewById(R.id.detail_size_recycler);
        sizeAdapter = new ProductSizeAdapter(
                sizes,
                outOfStock,
                defaultSize,
                size -> uiHelper.bindStockStatus(variantsLoaded, getSelectedVariantOrNull())
        );
        sizeRecycler.setLayoutManager(new GridLayoutManager(this, 4));
        sizeRecycler.setAdapter(sizeAdapter);
        uiHelper.bindStockStatus(variantsLoaded, getSelectedVariantOrNull());
    }

    private void refreshSizesForSelectedColor() {
        if (sizeAdapter == null) {
            return;
        }
        List<String> sizes = product.getSizesForColor(selectedColorName);
        Set<String> outOfStock = product.getOutOfStockSizesForColor(selectedColorName);
        String current = sizeAdapter.getSelectedSize();
        String nextSelected = (current != null && sizes.contains(current) && !outOfStock.contains(current))
                ? current
                : ProductDetailUiHelper.findFirstAvailableSize(sizes, outOfStock);
        sizeAdapter.updateSizes(sizes, outOfStock, nextSelected);
        uiHelper.bindStockStatus(variantsLoaded, getSelectedVariantOrNull());
    }

    private void setupReviews() {
        RecyclerView reviewRecycler = findViewById(R.id.detail_reviews_recycler);
        reviewRecycler.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter(Collections.emptyList());
        reviewRecycler.setAdapter(reviewAdapter);
    }

    private void openWriteReviewDialog() {
        if (!ensureSignedIn()) {
            return;
        }
        if (eligibleReviewOrderId == null) {
            showToast("Chỉ khách hàng đã mua và hoàn tất đơn hàng mới có thể đánh giá");
            return;
        }
        ReviewDialog.show(
                this,
                reviewViewModel,
                product.getId(),
                product.getName(),
                eligibleReviewOrderId,
                null
        );
    }

    @Nullable
    private ProductVariantModel getSelectedVariantOrNull() {
        String size = sizeAdapter != null ? sizeAdapter.getSelectedSize() : "42";
        return product.findVariant(selectedColorName, size);
    }

    @Nullable
    private ProductVariantModel resolveActionableVariant() {
        if (!ensureSignedIn()) {
            return null;
        }
        if (!variantsLoaded) {
            showToast("Đang tải tồn kho, vui lòng đợi giây lát...");
            return null;
        }
        ProductVariantModel variant = getSelectedVariantOrNull();
        if (variant == null) {
            showToast("Phân loại màu/size này hiện không khả dụng");
            return null;
        }
        if (!variant.isInStock()) {
            showToast("Size này hiện đã hết hàng");
            return null;
        }
        return variant;
    }

    private void setupBottomActions() {
        findViewById(R.id.detail_add_cart_btn).setOnClickListener(v -> {
            ProductVariantModel variant = resolveActionableVariant();
            if (variant == null) {
                return;
            }
            String size = sizeAdapter.getSelectedSize();
            boolean added = viewModel.addToCart(product, variant, size, selectedColorName, 1);
            if (!added) {
                showToast("Giỏ hàng đã đạt tối đa tồn kho (" + variant.getStockQty() + " đôi)");
            } else {
                showToast("Đã thêm " + product.getName() + " (EU " + size + ") vào giỏ");
            }
        });

        findViewById(R.id.detail_buy_now_btn).setOnClickListener(v -> {
            ProductVariantModel variant = resolveActionableVariant();
            if (variant == null) {
                return;
            }
            Intent intent = new Intent(this, OrderCheckoutActivity.class);
            intent.putExtra(OrderCheckoutActivity.EXTRA_DIRECT_PRODUCT_ID, product.getId());
            intent.putExtra(OrderCheckoutActivity.EXTRA_DIRECT_SIZE, sizeAdapter.getSelectedSize());
            intent.putExtra(OrderCheckoutActivity.EXTRA_DIRECT_COLOR, selectedColorName);
            intent.putExtra(OrderCheckoutActivity.EXTRA_DIRECT_VARIANT_ID, variant.getVariantId());
            startActivity(intent);
        });
    }
}
