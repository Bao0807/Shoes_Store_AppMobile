package com.example.shoes_store.module.product;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CategoryStyleHelper;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.module.review.ReviewModel;
import com.example.shoes_store.module.review.ReviewService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Helper thuần UI cho màn Chi tiết sản phẩm: cache View, bind thông tin, gallery dots,
 * color swatches và bảng thông số/đánh giá (0 tham chiếu Repository/Firebase).
 */
public final class ProductDetailUiHelper {

    private static final String TAG = "ProductDetailUiHelper";

    public interface OnColorSelectedListener {
        void onColorSelected(int index, @NonNull ProductVariantModel representative);
    }

    private final Context context;
    private final TabLayout infoTabs;
    private final View panelDesc;
    private final View panelSpecs;
    private final View panelReturn;
    private final View panelReviews;
    private final LinearLayout galleryDotsLayout;
    private final LinearLayout colorContainer;
    private final TextView selectedColorText;
    private final TextView stockStatusText;
    private final TextView ratingHeaderText;
    private final TextView reviewsScoreText;
    private final TextView reviewsSummaryText;
    private final TextView reviewEligibilityHint;
    private final TextView reviewsEmptyText;
    private final MaterialButton writeReviewBtn;
    private final ImageView favoriteIcon;
    private final TextView categoryBadgeText;
    private final TextView brandText;
    private final TextView nameText;
    private final TextView priceText;
    private final TextView originalPriceText;
    private final TextView discountBadgeText;
    private final TextView descHighlightText;
    private final TextView descriptionText;
    private final TextView specWeightText;
    private final TextView specDropText;
    private final TextView specCushionText;
    private final TextView specSurfaceText;
    private final TextView specTableBrandText;
    private final TextView specTableCategoryText;
    private final TextView specTableWeightText;
    private final TextView specTableDropText;
    private final TextView specTableCushionText;
    private final TextView specTableSurfaceText;
    private final TextView specTableSizesText;

    public ProductDetailUiHelper(@NonNull View root) {
        this.context = root.getContext();
        this.infoTabs = root.findViewById(R.id.detail_info_tabs);
        this.panelDesc = root.findViewById(R.id.detail_panel_desc);
        this.panelSpecs = root.findViewById(R.id.detail_panel_specs);
        this.panelReturn = root.findViewById(R.id.detail_panel_return);
        this.panelReviews = root.findViewById(R.id.detail_panel_reviews);
        this.galleryDotsLayout = root.findViewById(R.id.detail_gallery_dots);
        this.colorContainer = root.findViewById(R.id.detail_color_container);
        this.selectedColorText = root.findViewById(R.id.detail_selected_color_name);
        this.stockStatusText = root.findViewById(R.id.detail_stock_status);
        this.ratingHeaderText = root.findViewById(R.id.detail_rating);
        this.reviewsScoreText = root.findViewById(R.id.detail_reviews_score);
        this.reviewsSummaryText = root.findViewById(R.id.detail_reviews_summary);
        this.reviewEligibilityHint = root.findViewById(R.id.detail_review_eligibility_hint);
        this.reviewsEmptyText = root.findViewById(R.id.detail_reviews_empty);
        this.writeReviewBtn = root.findViewById(R.id.detail_write_review_btn);
        this.favoriteIcon = root.findViewById(R.id.detail_favorite_icon);
        this.categoryBadgeText = root.findViewById(R.id.detail_category_badge);
        this.brandText = root.findViewById(R.id.detail_brand);
        this.nameText = root.findViewById(R.id.detail_name);
        this.priceText = root.findViewById(R.id.detail_price);
        this.originalPriceText = root.findViewById(R.id.detail_original_price);
        this.discountBadgeText = root.findViewById(R.id.detail_discount_badge);
        this.descHighlightText = root.findViewById(R.id.detail_desc_highlight);
        this.descriptionText = root.findViewById(R.id.detail_description);
        this.specWeightText = root.findViewById(R.id.spec_weight);
        this.specDropText = root.findViewById(R.id.spec_drop);
        this.specCushionText = root.findViewById(R.id.spec_cushion);
        this.specSurfaceText = root.findViewById(R.id.spec_surface);
        this.specTableBrandText = root.findViewById(R.id.spec_table_brand);
        this.specTableCategoryText = root.findViewById(R.id.spec_table_category);
        this.specTableWeightText = root.findViewById(R.id.spec_table_weight);
        this.specTableDropText = root.findViewById(R.id.spec_table_drop);
        this.specTableCushionText = root.findViewById(R.id.spec_table_cushion);
        this.specTableSurfaceText = root.findViewById(R.id.spec_table_surface);
        this.specTableSizesText = root.findViewById(R.id.spec_table_sizes);
    }

    public void bindProductInfo(
            @NonNull ProductModel product,
            boolean reviewsLoaded,
            @NonNull List<ReviewModel> latestReviews
    ) {
        CategoryStyleHelper.applyCategoryBadge(categoryBadgeText, product.getCategory());
        brandText.setText(product.getBrand());
        nameText.setText(product.getName());

        if (reviewsLoaded) {
            updateRatingSummary(
                    ReviewService.calculateAverageRatingFromReviews(latestReviews),
                    latestReviews.size()
            );
        } else {
            double fallbackRating = product.getReviewCount() <= 0 ? 0.0 : product.getRating();
            updateRatingSummary(fallbackRating, product.getReviewCount());
        }

        priceText.setText(CurrencyFormatUtil.formatVnd(product.getPrice()));
        if (product.hasDiscount()) {
            priceText.setTextColor(ContextCompat.getColor(context, R.color.brand_accent));
            originalPriceText.setVisibility(View.VISIBLE);
            originalPriceText.setText(CurrencyFormatUtil.formatVnd(product.getOriginalPrice()));
            originalPriceText.setPaintFlags(originalPriceText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            discountBadgeText.setVisibility(View.VISIBLE);
            discountBadgeText.setText("Giảm " + product.getDiscountPercent() + "%");
        } else {
            priceText.setTextColor(ContextCompat.getColor(context, R.color.brand_primary));
            originalPriceText.setVisibility(View.GONE);
            discountBadgeText.setVisibility(View.GONE);
        }

        descHighlightText.setText("Dòng " + product.getCategory() + " · Bề mặt " + product.getSurfaceType());
        descriptionText.setText(product.getDescription());

        specWeightText.setText(product.getWeightGrams());
        specDropText.setText(product.getHeelDropMm());
        specCushionText.setText(product.getCushionLevel());
        specSurfaceText.setText(product.getSurfaceType());

        specTableBrandText.setText(product.getBrand());
        specTableCategoryText.setText(product.getCategory());
        specTableWeightText.setText(product.getWeightGrams());
        specTableDropText.setText(product.getHeelDropMm());
        specTableCushionText.setText(product.getCushionLevel());
        specTableSurfaceText.setText(product.getSurfaceType());
        updateSpecSizeRange(product);
    }

    public void updateSpecSizeRange(@NonNull ProductModel product) {
        specTableSizesText.setText(product.getEuSizeRange());
    }

    public void updateRatingSummary(double avg, int reviewCount) {
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

    public void bindFavoriteState(boolean isFavorite) {
        favoriteIcon.setImageResource(isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        favoriteIcon.setColorFilter(ContextCompat.getColor(
                context,
                isFavorite ? R.color.status_heart : R.color.text_primary
        ));
    }

    public void bindReviewEligibility(@Nullable String eligibleOrderId, boolean hasUserReviewed) {
        if (eligibleOrderId != null) {
            writeReviewBtn.setVisibility(View.VISIBLE);
            writeReviewBtn.setEnabled(true);
            writeReviewBtn.setText("Viết đánh giá");
            writeReviewBtn.setAlpha(1.0f);
            reviewEligibilityHint.setVisibility(View.GONE);
        } else if (hasUserReviewed) {
            writeReviewBtn.setVisibility(View.VISIBLE);
            writeReviewBtn.setEnabled(false);
            writeReviewBtn.setText("Đã đánh giá");
            writeReviewBtn.setAlpha(0.55f);
            reviewEligibilityHint.setVisibility(View.VISIBLE);
            reviewEligibilityHint.setText(
                    "Bạn đã đánh giá sản phẩm cho đơn hàng hoàn tất gần nhất. Mua thêm đơn mới để tiếp tục đánh giá."
            );
            reviewEligibilityHint.setTextColor(ContextCompat.getColor(context, R.color.status_success));
        } else {
            writeReviewBtn.setVisibility(View.GONE);
            reviewEligibilityHint.setVisibility(View.VISIBLE);
            reviewEligibilityHint.setText("Chỉ khách hàng đã mua và hoàn tất đơn hàng mới có thể đánh giá.");
            reviewEligibilityHint.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        }
    }

    public void bindReviewsEmpty(boolean isEmpty) {
        reviewsEmptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    public void bindStockStatus(boolean variantsLoaded, @Nullable ProductVariantModel variant) {
        if (!variantsLoaded) {
            stockStatusText.setText("Đang tải tồn kho...");
            stockStatusText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            return;
        }
        if (variant == null) {
            stockStatusText.setText("Không có sẵn");
            stockStatusText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        } else if (variant.getStockQty() > 0) {
            stockStatusText.setText("Còn " + variant.getStockQty() + " đôi trong kho");
            stockStatusText.setTextColor(ContextCompat.getColor(context, R.color.status_success));
        } else {
            stockStatusText.setText("Tạm hết hàng");
            stockStatusText.setTextColor(ContextCompat.getColor(context, R.color.status_danger));
        }
    }

    public void showInfoTab(int position) {
        panelDesc.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        panelSpecs.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        panelReturn.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
        panelReviews.setVisibility(position == 3 ? View.VISIBLE : View.GONE);
    }

    public void renderGalleryDots(int count, int activeIndex) {
        galleryDotsLayout.removeAllViews();
        float density = context.getResources().getDisplayMetrics().density;
        int activeWidth = Math.round(18 * density);
        int dotSize = Math.round(6 * density);
        int margin = Math.round(3 * density);

        for (int i = 0; i < count; i++) {
            View dot = new View(context);
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

    public void renderColorSwatches(
            @NonNull List<ProductVariantModel> colorReps,
            int selectedColorIndex,
            @NonNull String selectedColorName,
            @NonNull OnColorSelectedListener listener
    ) {
        colorContainer.removeAllViews();
        selectedColorText.setText(selectedColorName);
        float density = context.getResources().getDisplayMetrics().density;
        int outerSize = Math.round(36 * density);
        int innerSize = Math.round(26 * density);
        int spacing = Math.round(10 * density);

        for (int i = 0; i < colorReps.size(); i++) {
            final int index = i;
            ProductVariantModel rep = colorReps.get(i);
            FrameLayout wrapper = new FrameLayout(context);
            LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(outerSize, outerSize);
            wrapperParams.setMarginEnd(spacing);
            wrapper.setLayoutParams(wrapperParams);
            if (i == selectedColorIndex) {
                wrapper.setBackgroundResource(R.drawable.bg_color_ring);
            }

            View swatch = new View(context);
            swatch.setLayoutParams(new FrameLayout.LayoutParams(innerSize, innerSize, Gravity.CENTER));
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            try {
                circle.setColor(Color.parseColor(rep.getColorHex()));
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Invalid color hex '" + rep.getColorHex() + "': " + e.getMessage());
                circle.setColor(ContextCompat.getColor(context, R.color.brand_primary));
            }
            circle.setStroke(Math.round(1 * density), ContextCompat.getColor(context, R.color.border_subtle));
            swatch.setBackground(circle);

            wrapper.addView(swatch);
            wrapper.setOnClickListener(v -> listener.onColorSelected(index, rep));
            colorContainer.addView(wrapper);
        }
    }

    @NonNull
    public static List<String> buildGalleryUrls(@NonNull ProductModel model) {
        List<String> urls = new ArrayList<>(model.getGalleryUrls());
        if (urls.isEmpty() && !model.getThumbnailUrl().isEmpty()) {
            urls.add(model.getThumbnailUrl());
        }
        if (urls.isEmpty()) {
            urls = Collections.singletonList("");
        }
        return urls;
    }

    @NonNull
    public static String findFirstAvailableSize(@NonNull List<String> sizes, @NonNull Set<String> outOfStock) {
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
}
