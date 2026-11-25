package com.example.shoes_store.module.review;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseListAdapter;
import com.example.shoes_store.core.RemoteImageHelper;

import java.util.List;
import java.util.Locale;

/**
 * Adapter hiển thị danh sách đánh giá từ người mua thực.
 */
public class ReviewAdapter extends BaseListAdapter<ReviewModel, ReviewAdapter.ReviewViewHolder> {

    public ReviewAdapter(@Nullable List<ReviewModel> initialReviews) {
        setInitialItems(initialReviews);
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ReviewViewHolder(inflate(parent, R.layout.review_item_card));
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        ReviewModel item = getItem(position);
        RemoteImageHelper.bindImage(holder.avatarView, item.getReviewerAvatarUrl(), R.drawable.img_avatar_runner);
        holder.authorText.setText(item.getReviewerName());
        holder.ratingText.setText(String.format(Locale.US, "%.1f", item.getRating()));
        holder.dateText.setText(item.getDate());
        holder.commentText.setText(item.getComment());
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        final ImageView avatarView;
        final TextView authorText;
        final TextView ratingText;
        final TextView dateText;
        final TextView commentText;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarView = itemView.findViewById(R.id.review_avatar);
            authorText = itemView.findViewById(R.id.review_author);
            ratingText = itemView.findViewById(R.id.review_rating);
            dateText = itemView.findViewById(R.id.review_date);
            commentText = itemView.findViewById(R.id.review_comment);
        }
    }
}
