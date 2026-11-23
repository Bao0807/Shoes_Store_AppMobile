package com.example.shoes_store.module.product;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.shoes_store.core.UiInsetHelper;
import com.example.shoes_store.module.order.OrderCheckoutActivity;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.Locale;

/**
 * Màn hình Chi tiết giày chạy bộ (Gallery, Màu, Size EU, Tab thông tin: Mô tả / Thông số / Đổi trả / Đánh giá).
 */
public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private ProductViewModel viewModel;
    private ProductModel product;
    private ProductSizeAdapter sizeAdapter;
    private String selectedColorName = "";
    private int selectedColorIndex = 0;

    private NestedScrollView scrollView;
    private TabLayout infoTabs;
    private View panelDesc;
    private View panelSpecs;
    private View panelReturn;
    private View panelReviews;
    private LinearLayout galleryDotsLayout;
    private LinearLayout colorContainer;
    private TextView selectedColorText;
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
        galleryDotsLayout = findViewById(R.id.detail_gallery_dots);
        colorContainer = findViewById(R.id.detail_color_container);
        selectedColorText = findViewById(R.id.detail_selected_color_name);
        favoriteIcon = findViewById(R.id.detail_favorite_icon);

        findViewById(R.id.detail_back_btn).setOnClickListener(v -> finish());
        findViewById(R.id.detail_favorite_btn).setOnClickListener(v -> {
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
        observeFavoriteState();
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
        ((TextView) findViewById(R.id.detail_rating)).setText(
                String.format(Locale.US, "%.1f · %d đánh giá", product.getRating(), product.getReviewCount())
        );

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
        }

        // Tab Mô tả
        ((TextView) findViewById(R.id.detail_desc_highlight))
                .setText("Dòng " + product.getCategory() + " · Bề mặt " + product.getSurfaceType());
        ((TextView) findViewById(R.id.detail_description)).setText(product.getDescription());

        // Tab Thông số chi tiết (4 cards + bảng thông số)
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
        List<String> sizes = product.getAllSizes();
        String sizeRange = (sizes != null && !sizes.isEmpty())
                ? "EU " + sizes.get(0) + " - " + sizes.get(sizes.size() - 1)
                : "EU 38 - 44";
        ((TextView) findViewById(R.id.spec_table_sizes)).setText(sizeRange);

        // Tab Đánh giá summary
        ((TextView) findViewById(R.id.detail_reviews_score))
                .setText(String.format(Locale.US, "%.1f / 5.0", product.getRating()));
        ((TextView) findViewById(R.id.detail_reviews_summary))
                .setText("(" + product.getReviewCount() + " đánh giá từ runner)");
    }

    private void setupInfoTabs() {
        TabLayout.Tab reviewTab = infoTabs.getTabAt(3);
        if (reviewTab != null) {
            reviewTab.setText("Đánh giá (" + product.getReviewCount() + ")");
        }

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

    private void setupGallery() {
        ViewPager2 pager = findViewById(R.id.detail_gallery_pager);
        int[] gallery = product.getGalleryResIds();
        pager.setAdapter(new ProductGalleryAdapter(gallery));
        updateGalleryDots(gallery.length, 0);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateGalleryDots(gallery.length, position);
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
        List<String> names = product.getColorNames();
        List<String> hexes = product.getColorHexes();
        if (names == null || names.isEmpty()) {
            return;
        }
        selectedColorIndex = 0;
        selectedColorName = names.get(0);
        renderColorSwatches(names, hexes);
    }

    private void renderColorSwatches(List<String> names, List<String> hexes) {
        colorContainer.removeAllViews();
        selectedColorText.setText(selectedColorName);
        float density = getResources().getDisplayMetrics().density;
        int outerSize = Math.round(36 * density);
        int innerSize = Math.round(26 * density);
        int spacing = Math.round(10 * density);

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
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
            circle.setColor(Color.parseColor(hexes.get(i)));
            circle.setStroke(Math.round(1 * density), ContextCompat.getColor(this, R.color.border_subtle));
            swatch.setBackground(circle);

            wrapper.addView(swatch);
            wrapper.setOnClickListener(v -> {
                selectedColorIndex = index;
                selectedColorName = names.get(index);
                renderColorSwatches(names, hexes);
            });
            colorContainer.addView(wrapper);
        }
    }

    private void setupSizeGrid() {
        String defaultSize = findFirstAvailableSize(product);
        RecyclerView sizeRecycler = findViewById(R.id.detail_size_recycler);
        sizeAdapter = new ProductSizeAdapter(
                product.getAllSizes(),
                product.getOutOfStockSizes(),
                defaultSize,
                size -> {
                }
        );
        sizeRecycler.setLayoutManager(new GridLayoutManager(this, 4));
        sizeRecycler.setAdapter(sizeAdapter);
    }

    private String findFirstAvailableSize(ProductModel model) {
        if (model.getAllSizes().contains("42") && !model.getOutOfStockSizes().contains("42")) {
            return "42";
        }
        for (String size : model.getAllSizes()) {
            if (!model.getOutOfStockSizes().contains(size)) {
                return size;
            }
        }
        return "42";
    }

    private void setupReviews() {
        RecyclerView reviewRecycler = findViewById(R.id.detail_reviews_recycler);
        reviewRecycler.setLayoutManager(new LinearLayoutManager(this));
        reviewRecycler.setAdapter(new ProductReviewAdapter(product.getReviews()));
    }

    private void setupBottomActions() {
        findViewById(R.id.detail_add_cart_btn).setOnClickListener(v -> {
            String size = sizeAdapter.getSelectedSize();
            viewModel.addToCart(product, size, selectedColorName, 1);
            Toast.makeText(this, "Đã thêm " + product.getName() + " (EU " + size + ") vào giỏ", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.detail_buy_now_btn).setOnClickListener(v -> {
            String size = sizeAdapter.getSelectedSize();
            Intent intent = new Intent(this, OrderCheckoutActivity.class);
            intent.putExtra(OrderCheckoutActivity.EXTRA_DIRECT_PRODUCT_ID, product.getId());
            intent.putExtra(OrderCheckoutActivity.EXTRA_DIRECT_SIZE, size);
            intent.putExtra(OrderCheckoutActivity.EXTRA_DIRECT_COLOR, selectedColorName);
            startActivity(intent);
        });
    }

    private void observeFavoriteState() {
        viewModel.getFavoriteIdsLiveData().observe(this, ids -> {
            boolean isFav = ids != null && ids.contains(product.getId());
            favoriteIcon.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            favoriteIcon.setColorFilter(ContextCompat.getColor(
                    this,
                    isFav ? R.color.status_heart : R.color.text_primary
            ));
        });
    }
}
