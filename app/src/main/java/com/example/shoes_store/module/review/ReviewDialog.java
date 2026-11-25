package com.example.shoes_store.module.review;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.R;
import com.example.shoes_store.core.DialogHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Hộp thoại viết đánh giá sản phẩm dùng chung giữa Chi tiết sản phẩm và Chi tiết đơn hàng.
 */
public final class ReviewDialog {

    private ReviewDialog() {
    }

    public static void show(
            @NonNull Context context,
            @NonNull ReviewViewModel reviewViewModel,
            @NonNull String productId,
            @NonNull String productName,
            @NonNull String orderId,
            @Nullable Runnable onSuccess
    ) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.review_dialog_write, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.review_dialog_rating);
        TextInputLayout commentLayout = dialogView.findViewById(R.id.review_dialog_comment_layout);
        TextInputEditText commentInput = dialogView.findViewById(R.id.review_dialog_comment);

        DialogHelper.showFormDialog(
                context,
                "Đánh giá " + productName,
                dialogView,
                "Hủy",
                "Gửi đánh giá",
                (dialog, submitBtn) -> {
                    float rating = ratingBar.getRating();
                    String comment = DialogHelper.textOf(commentInput);
                    if (rating < 1.0f) {
                        Toast.makeText(context, "Vui lòng chọn từ 1 đến 5 sao", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (comment.isEmpty()) {
                        commentLayout.setError("Vui lòng nhập cảm nhận của bạn");
                        return;
                    }
                    commentLayout.setError(null);
                    submitBtn.setEnabled(false);
                    submitBtn.setText("Đang gửi...");

                    reviewViewModel.submitReview(productId, orderId, rating, comment, new ResourceCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            if (!DialogHelper.isHostAlive(context)) {
                                return;
                            }
                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }
                            Toast.makeText(context, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();
                            if (onSuccess != null) {
                                onSuccess.run();
                            }
                        }

                        @Override
                        public void onError(String message) {
                            if (!DialogHelper.isHostAlive(context)) {
                                return;
                            }
                            submitBtn.setEnabled(true);
                            submitBtn.setText("Gửi đánh giá");
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                        }
                    });
                }
        );
    }
}
