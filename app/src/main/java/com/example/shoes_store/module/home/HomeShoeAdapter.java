package com.example.shoes_store.module.home;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CategoryStyleHelper;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.core.RemoteImageHelper;
import com.example.shoes_store.module.product.ProductModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adapter hiển thị lưới 2 cột giày chạy bộ ở Trang chủ và Tìm kiếm.
 */
public class HomeShoeAdapter extends RecyclerView.Adapter<HomeShoeAdapter.ShoeViewHolder> {

    public interface OnShoeInteractionListener {
        void onShoeClick(ProductModel product);

        void onFavoriteToggle(ProductModel product);
    }

    private final List<ProductModel> items = new ArrayList<>();
    private final Set<String> favoriteIds = new HashSet<>();
    private final OnShoeInteractionListener listener;

    public HomeShoeAdapter(OnShoeInteractionListener listener) {
        this.listener = listener;
    }

    public void submitProducts(List<ProductModel> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setFavoriteIds(Set<String> newFavoriteIds) {
        favoriteIds.clear();
        if (newFavoriteIds != null) {
            favoriteIds.addAll(newFavoriteIds);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ShoeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.home_item_shoe, parent, false);
        return new ShoeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShoeViewHolder holder, int position) {
        ProductModel item = items.get(position);
        RemoteImageHelper.bindImage(holder.shoeImage, item.getThumbnailUrl(), item.getImageResId());
        CategoryStyleHelper.applyCategoryBadge(holder.categoryBadge, item.getCategory());

        if (item.hasDiscount()) {
            holder.discountBadge.setVisibility(View.VISIBLE);
            holder.discountBadge.setText("-" + item.getDiscountPercent() + "%");
            holder.priceText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.brand_accent));
            holder.originalPriceText.setVisibility(View.VISIBLE);
            holder.originalPriceText.setText(CurrencyFormatUtil.formatVnd(item.getOriginalPrice()));
            holder.originalPriceText.setPaintFlags(holder.originalPriceText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.discountBadge.setVisibility(View.GONE);
            holder.priceText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.brand_primary));
            holder.originalPriceText.setVisibility(View.GONE);
        }

        holder.brandText.setText(item.getBrand());
        holder.nameText.setText(item.getName());
        double displayRating = item.getReviewCount() <= 0 ? 0.0 : item.getRating();
        holder.ratingText.setText(String.format(Locale.US, "%.1f", displayRating));
        holder.reviewCountText.setText("(" + item.getReviewCount() + ")");
        holder.priceText.setText(CurrencyFormatUtil.formatVnd(item.getPrice()));

        boolean isFav = favoriteIds.contains(item.getId());
        holder.favoriteIcon.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        holder.favoriteIcon.setColorFilter(ContextCompat.getColor(
                holder.itemView.getContext(),
                isFav ? R.color.status_heart : R.color.text_primary
        ));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShoeClick(item);
            }
        });
        holder.favoriteBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteToggle(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ShoeViewHolder extends RecyclerView.ViewHolder {
        final ImageView shoeImage;
        final TextView categoryBadge;
        final TextView discountBadge;
        final View favoriteBtn;
        final ImageView favoriteIcon;
        final TextView brandText;
        final TextView nameText;
        final TextView ratingText;
        final TextView reviewCountText;
        final TextView priceText;
        final TextView originalPriceText;

        ShoeViewHolder(@NonNull View itemView) {
            super(itemView);
            shoeImage = itemView.findViewById(R.id.shoe_image);
            categoryBadge = itemView.findViewById(R.id.shoe_category_badge);
            discountBadge = itemView.findViewById(R.id.shoe_discount_badge);
            favoriteBtn = itemView.findViewById(R.id.shoe_favorite_btn);
            favoriteIcon = itemView.findViewById(R.id.shoe_favorite_icon);
            brandText = itemView.findViewById(R.id.shoe_brand);
            nameText = itemView.findViewById(R.id.shoe_name);
            ratingText = itemView.findViewById(R.id.shoe_rating_text);
            reviewCountText = itemView.findViewById(R.id.shoe_review_count);
            priceText = itemView.findViewById(R.id.shoe_price);
            originalPriceText = itemView.findViewById(R.id.shoe_original_price);
        }
    }
}
