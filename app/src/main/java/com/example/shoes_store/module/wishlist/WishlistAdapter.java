package com.example.shoes_store.module.wishlist;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseListAdapter;
import com.example.shoes_store.core.CategoryStyleHelper;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.core.RemoteImageHelper;
import com.example.shoes_store.module.product.ProductModel;

import java.util.Locale;

/**
 * Adapter hiển thị lưới giày đã yêu thích kèm thao tác thêm nhanh vào giỏ.
 */
public class WishlistAdapter extends BaseListAdapter<ProductModel, WishlistAdapter.WishlistViewHolder> {

    public interface OnWishlistActionListener {
        void onShoeClick(ProductModel product);

        void onRemoveFavorite(ProductModel product);

        void onAddToCart(ProductModel product);
    }

    private final OnWishlistActionListener listener;

    public WishlistAdapter(OnWishlistActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public WishlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new WishlistViewHolder(inflate(parent, R.layout.wishlist_item_shoe));
    }

    @Override
    public void onBindViewHolder(@NonNull WishlistViewHolder holder, int position) {
        ProductModel item = getItem(position);
        RemoteImageHelper.bindImage(holder.shoeImage, item.getThumbnailUrl(), item.getImageResId());
        CategoryStyleHelper.applyCategoryBadge(holder.categoryBadge, item.getCategory());
        holder.brandText.setText(item.getBrand());
        holder.nameText.setText(item.getName());
        double displayRating = item.getReviewCount() <= 0 ? 0.0 : item.getRating();
        holder.ratingText.setText(String.format(Locale.US, "%.1f (%d)", displayRating, item.getReviewCount()));
        holder.priceText.setText(CurrencyFormatUtil.formatVnd(item.getPrice()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShoeClick(item);
            }
        });
        holder.favoriteBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveFavorite(item);
            }
        });
        holder.addCartBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddToCart(item);
            }
        });
    }

    static class WishlistViewHolder extends RecyclerView.ViewHolder {
        final ImageView shoeImage;
        final TextView categoryBadge;
        final View favoriteBtn;
        final TextView brandText;
        final TextView nameText;
        final TextView ratingText;
        final TextView priceText;
        final View addCartBtn;

        WishlistViewHolder(@NonNull View itemView) {
            super(itemView);
            shoeImage = itemView.findViewById(R.id.wishlist_shoe_image);
            categoryBadge = itemView.findViewById(R.id.wishlist_category_badge);
            favoriteBtn = itemView.findViewById(R.id.wishlist_favorite_btn);
            brandText = itemView.findViewById(R.id.wishlist_brand);
            nameText = itemView.findViewById(R.id.wishlist_name);
            ratingText = itemView.findViewById(R.id.wishlist_rating);
            priceText = itemView.findViewById(R.id.wishlist_price);
            addCartBtn = itemView.findViewById(R.id.wishlist_add_cart_btn);
        }
    }
}
