package com.example.shoes_store.module.product;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;

/**
 * Adapter cho ViewPager2 hiển thị ảnh nhiều góc nhìn của giày.
 */
public class ProductGalleryAdapter extends RecyclerView.Adapter<ProductGalleryAdapter.GalleryViewHolder> {

    private final int[] imageResIds;

    public ProductGalleryAdapter(int[] imageResIds) {
        this.imageResIds = imageResIds != null ? imageResIds : new int[0];
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
        holder.imageView.setImageResource(imageResIds[position]);
    }

    @Override
    public int getItemCount() {
        return imageResIds.length;
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;

        GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.gallery_image);
        }
    }
}
