package com.example.shoes_store.module.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.RemoteImageHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter cho banner nổi bật trên Trang chủ.
 */
public class HomeBannerAdapter extends RecyclerView.Adapter<HomeBannerAdapter.BannerViewHolder> {

    public interface OnBannerClickListener {
        void onBannerClick(HomeBannerModel banner);
    }

    private final List<HomeBannerModel> items = new ArrayList<>();
    private final OnBannerClickListener listener;

    public HomeBannerAdapter(List<HomeBannerModel> initialItems, OnBannerClickListener listener) {
        if (initialItems != null) {
            items.addAll(initialItems);
        }
        this.listener = listener;
    }

    public void submitBanners(List<HomeBannerModel> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.home_item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        HomeBannerModel item = items.get(position);
        RemoteImageHelper.bindImage(holder.imageView, item.getImageUrl(), item.getImageResId());
        holder.badgeView.setText(item.getBadge());
        holder.titleView.setText(item.getTitle());
        holder.subtitleView.setText(item.getSubtitle());
        holder.ctaView.setText(item.getCtaLabel());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBannerClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final TextView badgeView;
        final TextView titleView;
        final TextView subtitleView;
        final TextView ctaView;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.banner_image);
            badgeView = itemView.findViewById(R.id.banner_badge);
            titleView = itemView.findViewById(R.id.banner_title);
            subtitleView = itemView.findViewById(R.id.banner_subtitle);
            ctaView = itemView.findViewById(R.id.banner_cta);
        }
    }
}
