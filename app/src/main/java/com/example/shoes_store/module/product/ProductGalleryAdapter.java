package com.example.shoes_store.module.product;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.RemoteImageHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter cho ViewPager2 hiển thị ảnh nhiều góc nhìn của giày từ URL hoặc drawable.
 */
public class ProductGalleryAdapter extends RecyclerView.Adapter<ProductGalleryAdapter.GalleryViewHolder> {

    private final List<String> imageUrls = new ArrayList<>();
    @DrawableRes
    private final int fallbackResId;

    public ProductGalleryAdapter(List<String> imageUrls, @DrawableRes int fallbackResId) {
        if (imageUrls != null) {
            this.imageUrls.addAll(imageUrls);
        }
        this.fallbackResId = fallbackResId != 0 ? fallbackResId : R.drawable.img_shoe_asics_novablast;
    }

    public void submitImages(List<String> newUrls) {
        imageUrls.clear();
        if (newUrls != null) {
            imageUrls.addAll(newUrls);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.product_item_gallery, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        RemoteImageHelper.bindImage(holder.imageView, imageUrls.get(position), fallbackResId);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;

        GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.gallery_image);
        }
    }
}
