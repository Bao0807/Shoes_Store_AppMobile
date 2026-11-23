package com.example.shoes_store.module.admin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.voucher.VoucherModel;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel (Controller) cho màn hình Bảng điều khiển Quản trị viên (Admin).
 */
public class AdminViewModel extends ViewModel {

    private final AdminRepository repository = new AdminRepository();
    @Nullable
    private OrderModel.Status orderFilterStatus = null;

    public AdminViewModel() {
        repository.startListening();
    }

    public LiveData<List<OrderModel>> getAllOrdersLiveData() {
        return repository.getAllOrdersLiveData();
    }

    public LiveData<List<ProductModel>> getAllProductsLiveData() {
        return repository.getAllProductsLiveData();
    }

    public LiveData<List<VoucherModel>> getAllVouchersLiveData() {
        return repository.getAllVouchersLiveData();
    }

    @Nullable
    public OrderModel.Status getOrderFilterStatus() {
        return orderFilterStatus;
    }

    public void setOrderFilterStatus(@Nullable OrderModel.Status status) {
        this.orderFilterStatus = status;
    }

    @NonNull
    public List<OrderModel> filterOrders(@Nullable List<OrderModel> source) {
        List<OrderModel> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (OrderModel order : source) {
            if (orderFilterStatus == null || order.getStatus() == orderFilterStatus) {
                result.add(order);
            }
        }
        return result;
    }

    public void updateOrderStatus(
            @NonNull String orderId,
            @NonNull OrderModel.Status targetStatus,
            @NonNull ResourceCallback<Void> callback
    ) {
        repository.updateOrderStatus(orderId, targetStatus, callback);
    }

    public void saveProductWithVariants(
            @Nullable String existingProductId,
            @NonNull String name,
            @NonNull String brand,
            @NonNull String category,
            long price,
            long originalPrice,
            @NonNull String thumbnailUrl,
            @NonNull String weightGrams,
            @NonNull String heelDropMm,
            @NonNull String cushionLevel,
            @NonNull String surfaceType,
            @NonNull String description,
            @NonNull List<String> colorNames,
            @NonNull List<String> colorHexes,
            @NonNull List<String> sizes,
            int stockPerVariant,
            boolean isActive,
            @NonNull ResourceCallback<Void> callback
    ) {
        repository.saveProductWithVariants(
                existingProductId, name, brand, category, price, originalPrice, thumbnailUrl,
                weightGrams, heelDropMm, cushionLevel, surfaceType, description,
                colorNames, colorHexes, sizes, stockPerVariant, isActive, callback
        );
    }

    public void toggleProductActive(
            @NonNull String productId,
            boolean isActive,
            @NonNull ResourceCallback<Void> callback
    ) {
        repository.toggleProductActive(productId, isActive, callback);
    }

    public void saveVoucher(
            @NonNull String code,
            @NonNull String shortBadge,
            @NonNull String title,
            @NonNull String description,
            long discountAmount,
            long minOrderAmount,
            int usageLimit,
            int validDays,
            boolean isActive,
            @NonNull ResourceCallback<Void> callback
    ) {
        repository.saveVoucher(
                code, shortBadge, title, description, discountAmount,
                minOrderAmount, usageLimit, validDays, isActive, callback
        );
    }

    public void toggleVoucherActive(
            @NonNull String code,
            boolean isActive,
            @NonNull ResourceCallback<Void> callback
    ) {
        repository.toggleVoucherActive(code, isActive, callback);
    }

    @Override
    protected void onCleared() {
        repository.stopListening();
        super.onCleared();
    }
}
