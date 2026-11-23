package com.example.shoes_store.module.product;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CategoryStyleHelper;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.core.UiInsetHelper;
import com.example.shoes_store.module.auth.AuthLoginActivity;
import com.example.shoes_store.module.order.OrderCheckoutActivity;
import com.example.shoes_store.module.order.OrderModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Màn hình Chi tiết giày chạy bộ (Gallery, Màu, Size EU theo biến thể tồn kho thực, Đánh giá người mua thực).
 */
public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private ProductViewModel viewModel;
    private ProductModel product;
    private ProductGalleryAdapter galleryAdapter;
    private ProductSizeAdapter sizeAdapter;
    private ProductReviewAdapter reviewAdapter;

    private final List<ProductVariantModel> colorReps = new ArrayList<>();
    private List<String> currentGalleryUrls = new ArrayList<>();
    private List<OrderModel> latestOrders = new ArrayList<>();
    private List<ProductReviewModel> latestReviews = new ArrayList<>();
    @Nullable
    private String eligibleReviewOrderId;
    private String selectedColorName = "";
    private int selectedColorIndex = 0;
    private boolean reviewsLoaded = false;

    private NestedScrollView scrollView;
    private TabLayout infoTabs;
    private View panelDesc;
    private View panelSpecs;
    private View panelReturn;
    private View panelReviews;
    private ViewPager2 galleryPager;
    private LinearLayout galleryDotsLayout;
    private LinearLayout colorContainer;
    private TextView selectedColorText;
    private TextView stockStatusText;
    private TextView ratingHeaderText;
    private TextView reviewsScoreText;
    private TextView reviewsSummaryText;
    private TextView reviewEligibilityHint;
    private TextView reviewsEmptyText;
    private MaterialButton writeReviewBtn;
    private ImageView favoriteIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.product_detail_activity);

        UiInsetHelper.applyTopInset(findViewById(R.id.detail_top_bar));
        UiInsetHelper.applyBottomInset(findViewById(R.id.detail_bottom_bar));

        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        product = viewModel.getProduct(productId);
        if (product == null) {
            finish();
            return;
        }

        scrollView = findViewById(R.id.detail_scroll_view);
        infoTabs = findViewById(R.id.detail_info_tabs);
        panelDesc = findViewById(R.id.detail_panel_desc);
        panelSpecs = findViewById(R.id.detail_panel_specs);
        panelReturn = findViewById(R.id.detail_panel_return);
        panelReviews = findViewById(R.id.detail_panel_reviews);
        galleryPager = findViewById(R.id.detail_gallery_pager);
        galleryDotsLayout = findViewById(R.id.detail_gallery_dots);
        colorContainer = findViewById(R.id.detail_color_container);
        selectedColorText = findViewById(R.id.detail_selected_color_name);
        stockStatusText = findViewById(R.id.detail_stock_status);
        ratingHeaderText = findViewById(R.id.detail_rating);
        reviewsScoreText = findViewById(R.id.detail_reviews_score);
        reviewsSummaryText = findViewById(R.id.detail_reviews_summary);
        reviewEligibilityHint = findViewById(R.id.detail_review_eligibility_hint);
        reviewsEmptyText = findViewById(R.id.detail_reviews_empty);
        writeReviewBtn = findViewById(R.id.detail_write_review_btn);
        favoriteIcon = findViewById(R.id.detail_favorite_icon);

        findViewById(R.id.detail_back_btn).setOnClickListener(v -> finish());
        findViewById(R.id.detail_favorite_btn).setOnClickListener(v -> {
            if (!ensureSignedIn()) {
                return;
            }
            boolean added = viewModel.toggleFavorite(product.getId());
            Toast.makeText(
                    this,
                    added ? "Đã thêm vào Yêu thích" : "Đã bỏ khỏi Yêu thích",
                    Toast.LENGTH_SHORT
            ).show();
        });

        findViewById(R.id.detail_size_guide_btn).setOnClickListener(v ->
                new ProductSizeGuideBottomSheet().show(getSupportFragmentManager(), "size_guide"));

        findViewById(R.id.detail_rating_row).setOnClickListener(v -> jumpToInfoTab(3));
        writeReviewBtn.setOnClickListener(v -> openWriteReviewDialog());

        View.OnClickListener openDetailSpecs = v -> jumpToInfoTab(1);
        findViewById(R.id.detail_specs_more_btn).setOnClickListener(openDetailSpecs);
        findViewById(R.id.detail_highlight_specs_row).setOnClickListener(openDetailSpecs);

        bindProductInfo();
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
        if (FirebaseProviderHelper.getInstance().isSignedIn()) {
            return true;
        }
        Toast.makeText(this, R.string.auth_login_required, Toast.LENGTH_SHORT).show();
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
                    bindProductInfo();
                    syncGalleryIfChanged();
                    refreshReviewEligibility();
                    break;
                }
            }
        });

        viewModel.getFavoriteIdsLiveData().observe(this, ids -> {
            boolean isFav = ids != null && ids.contains(productId);
            favoriteIcon.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            favoriteIcon.setColorFilter(ContextCompat.getColor(
                    this,
                    isFav ? R.color.status_heart : R.color.text_primary
            ));
        });

        viewModel.loadVariants(productId).observe(this, variants -> {
            if (variants == null || variants.isEmpty()) {
                return;
            }
            product.setVariants(variants);
            setupColorSwatches();
            refreshSizesForSelectedColor();
            updateSelectedVariantStock();
            updateSpecSizeRange();
        });

        viewModel.getOrdersLiveData().observe(this, orders -> {
            latestOrders = orders != null ? orders : new ArrayList<>();
            refreshReviewEligibility();
        });

        viewModel.loadReviews(productId).observe(this, loadedReviews -> {
            reviewsLoaded = true;
            latestReviews = loadedReviews != null ? loadedReviews : new ArrayList<>();
            reviewAdapter.submitReviews(latestReviews);
            reviewsEmptyText.setVisibility(latestReviews.isEmpty() ? View.VISIBLE : View.GONE);

            if (!latestReviews.isEmpty()) {
                List<Double> ratings = new ArrayList<>();
                for (ProductReviewModel rev : latestReviews) {
                    ratings.add((double) rev.getRating());
                }
                double liveAvg = ProductReviewService.calculateAverageRating(ratings);
                updateRatingSummaryViews(liveAvg, latestReviews.size());
            } else {
                updateRatingSummaryViews(0.0, 0);
            }
            refreshReviewEligibility();
        });
    }

    private void refreshReviewEligibility() {
        String uid = FirebaseProviderHelper.getInstance().getCurrentUid();
        eligibleReviewOrderId = ProductReviewService.findEligibleOrderIdForReview(
                uid,
                product.getId(),
                latestOrders,
                latestReviews
        );
        if (eligibleReviewOrderId != null) {
            writeReviewBtn.setVisibility(View.VISIBLE);
            reviewEligibilityHint.setVisibility(View.GONE);
        } else if (ProductReviewService.hasUserReviewedProduct(uid, latestReviews)) {
            writeReviewBtn.setVisibility(View.GONE);
            reviewEligibilityHint.setVisibility(View.VISIBLE);
            reviewEligibilityHint.setText("Bạn đã đánh giá sản phẩm từ đơn hàng đã hoàn tất.");
            reviewEligibilityHint.setTextColor(ContextCompat.getColor(this, R.color.status_success));
        } else {
            writeReviewBtn.setVisibility(View.GONE);
            reviewEligibilityHint.setVisibility(View.VISIBLE);
            reviewEligibilityHint.setText("Chỉ khách hàng đã mua và hoàn tất đơn hàng mới có thể đánh giá.");
            reviewEligibilityHint.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void updateRatingSummaryViews(double avg, int reviewCount) {
        if (reviewCount <= 0) {
            ratingHeaderText.setText("Chưa có đánh giá");
            reviewsScoreText.setText("0.0 / 5.0");
            reviewsSummaryText.setText("(0 đánh giá)");
        } else {
            ratingHeaderText.setText(String.format(Locale.US, "%.1f · %d đánh giá", avg, reviewCount));
            reviewsScoreText.setText(String.format(Locale.US, "%.1f / 5.0", avg));
            reviewsSummaryText.setText("(" + reviewCount + " đánh giá từ runner)");
        }
        TabLayout.Tab reviewTab = infoTabs.getTabAt(3);
        if (reviewTab != null) {
            reviewTab.setText("Đánh giá (" + Math.max(0, reviewCount) + ")");
        }
    }

    private void jumpToInfoTab(int tabIndex) {
        TabLayout.Tab targetTab = infoTabs.getTabAt(tabIndex);
        if (targetTab != null) {
            targetTab.select();
        }
        scrollView.post(() -> scrollView.smoothScrollTo(0, infoTabs.getTop()));
    }

    private void bindProductInfo() {
        TextView categoryBadge = findViewById(R.id.detail_category_badge);
        CategoryStyleHelper.applyCategoryBadge(categoryBadge, product.getCategory());

        ((TextView) findViewById(R.id.detail_brand)).setText(product.getBrand());
        ((TextView) findViewById(R.id.detail_name)).setText(product.getName());

        if (reviewsLoaded) {
            List<Double> ratings = new ArrayList<>();
            for (ProductReviewModel rev : latestReviews) {
                ratings.add((double) rev.getRating());
            }
            updateRatingSummaryViews(ProductReviewService.calculateAverageRating(ratings), latestReviews.size());
        } else {
            double fallbackRating = product.getReviewCount() <= 0 ? 0.0 : product.getRating();
            updateRatingSummaryViews(fallbackRating, product.getReviewCount());
        }

        TextView priceText = findViewById(R.id.detail_price);
        TextView originalPriceText = findViewById(R.id.detail_original_price);
        TextView discountBadge = findViewById(R.id.detail_discount_badge);

        priceText.setText(CurrencyFormatUtil.formatVnd(product.getPrice()));
        if (product.hasDiscount()) {
            priceText.setTextColor(ContextCompat.getColor(this, R.color.brand_accent));
            originalPriceText.setVisibility(View.VISIBLE);
            originalPriceText.setText(CurrencyFormatUtil.formatVnd(product.getOriginalPrice()));
            originalPriceText.setPaintFlags(originalPriceText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            discountBadge.setVisibility(View.VISIBLE);
            discountBadge.setText("Giảm " + product.getDiscountPercent() + "%");
        } else {
            priceText.setTextColor(ContextCompat.getColor(this, R.color.brand_primary));
            originalPriceText.setVisibility(View.GONE);
            discountBadge.setVisibility(View.GONE);
        }

        ((TextView) findViewById(R.id.detail_desc_highlight))
                .setText("Dòng " + product.getCategory() + " · Bề mặt " + product.getSurfaceType());
        ((TextView) findViewById(R.id.detail_description)).setText(product.getDescription());

        ((TextView) findViewById(R.id.spec_weight)).setText(product.getWeightGrams());
        ((TextView) findViewById(R.id.spec_drop)).setText(product.getHeelDropMm());
        ((TextView) findViewById(R.id.spec_cushion)).setText(product.getCushionLevel());
        ((TextView) findViewById(R.id.spec_surface)).setText(product.getSurfaceType());

        ((TextView) findViewById(R.id.spec_table_brand)).setText(product.getBrand());
        ((TextView) findViewById(R.id.spec_table_category)).setText(product.getCategory());
        ((TextView) findViewById(R.id.spec_table_weight)).setText(product.getWeightGrams());
        ((TextView) findViewById(R.id.spec_table_drop)).setText(product.getHeelDropMm());
        ((TextView) findViewById(R.id.spec_table_cushion)).setText(product.getCushionLevel());
        ((TextView) findViewById(R.id.spec_table_surface)).setText(product.getSurfaceType());
        updateSpecSizeRange();
    }

    private void updateSpecSizeRange() {
        List<String> sizes;
        if (!product.getVariants().isEmpty()) {
            Set<String> distinct = new LinkedHashSet<>();
            for (ProductVariantModel v : product.getVariants()) {
                if (!v.getSize().trim().isEmpty()) {
                    distinct.add(v.getSize().trim());
                }
            }
            sizes = new ArrayList<>(distinct);
            Collections.sort(sizes, Comparator.comparingDouble(s -> {
                try {
                    return Double.parseDouble(s);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }));
        } else {
            sizes = product.getAllSizes();
        }
        String sizeRange = (sizes != null && !sizes.isEmpty())
                ? "EU " + sizes.get(0) + " - " + sizes.get(sizes.size() - 1)
                : "EU 38 - 44";
        ((TextView) findViewById(R.id.spec_table_sizes)).setText(sizeRange);
    }

    private void setupInfoTabs() {
        showInfoTab(0);
        infoTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showInfoTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void showInfoTab(int position) {
        panelDesc.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        panelSpecs.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        panelReturn.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
        panelReviews.setVisibility(position == 3 ? View.VISIBLE : View.GONE);
    }

    private List<String> buildGalleryUrls(@NonNull ProductModel model) {
        List<String> urls = new ArrayList<>(model.getGalleryUrls());
        if (urls.isEmpty() && !model.getThumbnailUrl().isEmpty()) {
            urls.add(model.getThumbnailUrl());
        }
        if (urls.isEmpty()) {
            urls = Collections.singletonList("");
        }
        return urls;
    }

    private void syncGalleryIfChanged() {
        if (galleryAdapter == null) {
            return;
        }
        List<String> newUrls = buildGalleryUrls(product);
        if (newUrls.equals(currentGalleryUrls)) {
            return;
        }
        currentGalleryUrls = newUrls;
        galleryAdapter.submitImages(newUrls);
        int count = galleryAdapter.getItemCount();
        updateGalleryDots(count, Math.min(galleryPager.getCurrentItem(), Math.max(0, count - 1)));
    }

    private void setupGallery() {
        currentGalleryUrls = buildGalleryUrls(product);
        galleryAdapter = new ProductGalleryAdapter(currentGalleryUrls, product.getImageResId());
        galleryPager.setAdapter(galleryAdapter);
        updateGalleryDots(galleryAdapter.getItemCount(), 0);
        galleryPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateGalleryDots(galleryAdapter.getItemCount(), position);
            }
        });
    }

    private void updateGalleryDots(int count, int activeIndex) {
        galleryDotsLayout.removeAllViews();
        int activeWidth = Math.round(18 * getResources().getDisplayMetrics().density);
        int dotSize = Math.round(6 * getResources().getDisplayMetrics().density);
        int margin = Math.round(3 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    i == activeIndex ? activeWidth : dotSize,
                    dotSize
            );
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == activeIndex ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
            galleryDotsLayout.addView(dot);
        }
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
        colorContainer.removeAllViews();
        selectedColorText.setText(selectedColorName);
        float density = getResources().getDisplayMetrics().density;
        int outerSize = Math.round(36 * density);
        int innerSize = Math.round(26 * density);
        int spacing = Math.round(10 * density);

        for (int i = 0; i < colorReps.size(); i++) {
            final int index = i;
            ProductVariantModel rep = colorReps.get(i);
            FrameLayout wrapper = new FrameLayout(this);
            LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(outerSize, outerSize);
            wrapperParams.setMarginEnd(spacing);
            wrapper.setLayoutParams(wrapperParams);
            if (i == selectedColorIndex) {
                wrapper.setBackgroundResource(R.drawable.bg_color_ring);
            }

            View swatch = new View(this);
            FrameLayout.LayoutParams innerParams = new FrameLayout.LayoutParams(innerSize, innerSize, Gravity.CENTER);
            swatch.setLayoutParams(innerParams);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            try {
                circle.setColor(Color.parseColor(rep.getColorHex()));
            } catch (IllegalArgumentException e) {
                circle.setColor(ContextCompat.getColor(this, R.color.brand_primary));
            }
            circle.setStroke(Math.round(1 * density), ContextCompat.getColor(this, R.color.border_subtle));
            swatch.setBackground(circle);

            wrapper.addView(swatch);
            wrapper.setOnClickListener(v -> {
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
            colorContainer.addView(wrapper);
        }
    }

    private void setupSizeGrid() {
        List<String> sizes = product.getSizesForColor(selectedColorName);
        Set<String> outOfStock = product.getOutOfStockSizesForColor(selectedColorName);
        String defaultSize = findFirstAvailableSize(sizes, outOfStock);
        RecyclerView sizeRecycler = findViewById(R.id.detail_size_recycler);
        sizeAdapter = new ProductSizeAdapter(sizes, outOfStock, defaultSize, size -> updateSelectedVariantStock());
        sizeRecycler.setLayoutManager(new GridLayoutManager(this, 4));
        sizeRecycler.setAdapter(sizeAdapter);
        updateSelectedVariantStock();
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
                : findFirstAvailableSize(sizes, outOfStock);
        sizeAdapter.updateSizes(sizes, outOfStock, nextSelected);
        updateSelectedVariantStock();
    }

    private void updateSelectedVariantStock() {
        if (stockStatusText == null) {
            return;
        }
        if (product.getVariants().isEmpty()) {
            stockStatusText.setText("Đang tải tồn kho...");
            stockStatusText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            return;
        }
        ProductVariantModel variant = getSelectedVariantOrNull();
        if (variant == null) {
            stockStatusText.setText("Không có sẵn");
            stockStatusText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else if (variant.getStockQty() > 0) {
            stockStatusText.setText("Còn " + variant.getStockQty() + " đôi trong kho");
            stockStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_success));
        } else {
            stockStatusText.setText("Tạm hết hàng");
            stockStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_danger));
        }
    }

    private String findFirstAvailableSize(List<String> sizes, Set<String> outOfStock) {
        if (sizes.contains("42") && !outOfStock.contains("42")) {
            return "42";
        }
        for (String size : sizes) {
            if (!outOfStock.contains(size)) {
                return size;
            }
        }
        return sizes.isEmpty() ? "42" : sizes.get(0);
    }

    private void setupReviews() {
        RecyclerView reviewRecycler = findViewById(R.id.detail_reviews_recycler);
        reviewRecycler.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ProductReviewAdapter(Collections.emptyList());
        reviewRecycler.setAdapter(reviewAdapter);
    }

    private void openWriteReviewDialog() {
        if (!ensureSignedIn()) {
            return;
        }
        if (eligibleReviewOrderId == null) {
            Toast.makeText(
                    this,
                    "Chỉ khách hàng đã mua và hoàn tất đơn hàng mới có thể đánh giá",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        final String targetOrderId = eligibleReviewOrderId;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.product_dialog_review, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.review_dialog_rating);
        TextInputLayout commentLayout = dialogView.findViewById(R.id.review_dialog_comment_layout);
        TextInputEditText commentInput = dialogView.findViewById(R.id.review_dialog_comment);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Đánh giá " + product.getName())
                .setView(dialogView)
                .setPositiveButton("Gửi đánh giá", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button submitBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            submitBtn.setOnClickListener(v -> {
                float rating = ratingBar.getRating();
                String comment = commentInput.getText() != null ? commentInput.getText().toString().trim() : "";
                if (rating < 1.0f) {
                    Toast.makeText(this, "Vui lòng chọn từ 1 đến 5 sao", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (comment.isEmpty()) {
                    commentLayout.setError("Vui lòng nhập cảm nhận của bạn");
                    return;
                }
                commentLayout.setError(null);
                submitBtn.setEnabled(false);
                submitBtn.setText("Đang gửi...");

                viewModel.submitReview(product.getId(), targetOrderId, rating, comment, new ResourceCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        dialog.dismiss();
                        Toast.makeText(ProductDetailActivity.this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        submitBtn.setEnabled(true);
                        submitBtn.setText("Gửi đánh giá");
                        Toast.makeText(ProductDetailActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
            });
        });

        dialog.show();
    }

    @Nullable
    private ProductVariantModel getSelectedVariantOrNull() {
        String size = sizeAdapter != null ? sizeAdapter.getSelectedSize() : "42";
        return product.findVariant(selectedColorName, size);
    }

    private void setupBottomActions() {
        findViewById(R.id.detail_add_cart_btn).setOnClickListener(v -> {
            if (!ensureSignedIn()) {
                return;
            }
            String size = sizeAdapter.getSelectedSize();
            ProductVariantModel variant = getSelectedVariantOrNull();
            if (variant != null && !variant.isInStock()) {
                Toast.makeText(this, "Size này hiện đã hết hàng", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean added = viewModel.addToCart(product, variant, size, selectedColorName, 1);
            if (!added && variant != null) {
                Toast.makeText(
                        this,
                        "Giỏ hàng đã đạt tối đa tồn kho (" + variant.getStockQty() + " đôi)",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(this, "Đã thêm " + product.getName() + " (EU " + size + ") vào giỏ", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.detail_buy_now_btn).setOnClickListener(v -> {
            if (!ensureSignedIn()) {
                return;
            }
            String size = sizeAdapter.getSelectedSize();
            ProductVariantModel variant = getSelectedVariantOrNull();
            if (variant != null && !variant.isInStock()) {
                Toast.makeText(this, "Size này hiện đã hết hàng", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, OrderCheckoutActivity.class);
            intent.putExtra(OrderCheckoutActivity.EXTRA_DIRECT_PRODUCT_ID, product.getId());
            intent.putExtra(OrderCheckoutActivity.EXTRA_DIRECT_SIZE, size);
            intent.putExtra(OrderCheckoutActivity.EXTRA_DIRECT_COLOR, selectedColorName);
            if (variant != null) {
                intent.putExtra(OrderCheckoutActivity.EXTRA_DIRECT_VARIANT_ID, variant.getVariantId());
            }
            startActivity(intent);
        });
    }
}
