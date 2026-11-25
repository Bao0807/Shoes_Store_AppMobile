package com.example.shoes_store.module.product;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseListAdapter;
import com.example.shoes_store.core.RemoteImageHelper;

import java.util.List;

/**
 * Adapter cho ViewPager2 hiển thị ảnh nhiều góc nhìn của giày từ URL hoặc drawable.
 */
public class ProductGalleryAdapter extends BaseListAdapter<String, ProductGalleryAdapter.GalleryViewHolder> {

    @DrawableRes
    private final int fallbackResId;

    public ProductGalleryAdapter(List<String> imageUrls, @DrawableRes int fallbackResId) {
        setInitialItems(imageUrls);
        this.fallbackResId = fallbackResId != 0 ? fallbackResId : R.drawable.img_shoe_asics_novablast;
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GalleryViewHolder(inflate(parent, R.layout.product_item_gallery));
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        RemoteImageHelper.bindImage(holder.imageView, getItem(position), fallbackResId);
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;

        GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.gallery_image);
        }
    }
}
