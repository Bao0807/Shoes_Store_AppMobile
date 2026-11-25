package com.example.shoes_store.module.review;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.core.ResourceCallback;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Set;

/**
 * ViewModel cho Đánh giá sản phẩm: đọc realtime danh sách đánh giá, kiểm tra trạng thái đã đánh giá theo đơn hàng và gửi đánh giá.
 */
public class ReviewViewModel extends ViewModel {

    private final ReviewRepository repository = ReviewRepository.getInstance();
    private final MutableLiveData<List<ReviewModel>> reviewsLiveData = new MutableLiveData<>();

    @Nullable
    private String observedProductId;
    @Nullable
    private ListenerRegistration reviewsRegistration;

    public LiveData<List<ReviewModel>> loadReviews(@NonNull String productId) {
        if (!productId.equals(observedProductId)) {
            detachListener();
            observedProductId = productId;
            reviewsRegistration = repository.observeReviews(productId, reviewsLiveData);
        }
        return reviewsLiveData;
    }

    public void submitReview(
            @NonNull String productId,
            @NonNull String orderId,
            float rating,
            @NonNull String comment,
            @NonNull ResourceCallback<Void> callback
    ) {
        repository.submitReview(productId, orderId, rating, comment, callback);
    }

    public void loadReviewedProductIdsForOrder(
            @NonNull String orderId,
            @NonNull List<String> productIds,
            @NonNull ResourceCallback<Set<String>> callback
    ) {
        repository.loadReviewedProductIdsForOrder(orderId, productIds, callback);
    }

    private void detachListener() {
        if (reviewsRegistration != null) {
            reviewsRegistration.remove();
            reviewsRegistration = null;
        }
    }

    @Override
    protected void onCleared() {
        detachListener();
        super.onCleared();
    }
}
