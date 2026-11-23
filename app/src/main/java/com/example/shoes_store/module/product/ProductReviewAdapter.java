package com.example.shoes_store.module.product;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;

import java.util.List;
import java.util.Locale;

/**
 * Adapter hiển thị danh sách đánh giá từ runner.
 */
public class ProductReviewAdapter extends RecyclerView.Adapter<ProductReviewAdapter.ReviewViewHolder> {

    private final List<ProductReviewModel> reviews;

    public ProductReviewAdapter(List<ProductReviewModel> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.product_item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        ProductReviewModel item = reviews.get(position);
        holder.authorText.setText(item.getReviewerName());
        holder.ratingText.setText(String.format(Locale.US, "%.1f", item.getRating()));
        holder.dateText.setText(item.getDate());
        holder.commentText.setText(item.getComment());
    }

    @Override
    public int getItemCount() {
        return reviews != null ? reviews.size() : 0;
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        final TextView authorText;
        final TextView ratingText;
        final TextView dateText;
        final TextView commentText;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            authorText = itemView.findViewById(R.id.review_author);
            ratingText = itemView.findViewById(R.id.review_rating);
            dateText = itemView.findViewById(R.id.review_date);
            commentText = itemView.findViewById(R.id.review_comment);
        }
    }
}
