package com.example.shoes_store.module.order;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseActivity;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.payment.PaymentStatusHelper;
import com.example.shoes_store.module.payment.PaymentViewModel;
import com.example.shoes_store.module.product.ProductDetailActivity;
import com.example.shoes_store.module.review.ReviewDialog;
import com.example.shoes_store.module.review.ReviewViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Màn hình Chi tiết đơn hàng kèm tiến trình 4 mốc giao hàng, thông tin giao dịch Payment realtime,
 * thao tác Hủy / Mua lại và nút Đánh giá từng sản phẩm sau khi hoàn thành đơn.
 */
public class OrderDetailActivity extends BaseActivity {

    private static final String TAG = "OrderDetailActivity";
    public static final String EXTRA_ORDER_ID = "extra_order_id";
    public static final String EXTRA_ORDER_CODE = "extra_order_code";

    private OrderViewModel viewModel;
    private PaymentViewModel paymentViewModel;
    private ReviewViewModel reviewViewModel;
    private OrderCheckoutItemAdapter itemsAdapter;
    private View bottomBar;
    private MaterialButton actionBtn;
    private String orderKey = "";
    @Nullable
    private OrderModel currentOrder;
    @Nullable
    private String lastPaymentFetchKey;
    @Nullable
    private String lastReviewFetchOrderId;
    private boolean skipNextResumeReviewRefresh = false;
    private final Set<String> reviewedProductIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContent(R.layout.order_detail_activity, R.id.order_detail_root);

        viewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        paymentViewModel = new ViewModelProvider(this).get(PaymentViewModel.class);
        reviewViewModel = new ViewModelProvider(this).get(ReviewViewModel.class);

        orderKey = getIntent().getStringExtra(EXTRA_ORDER_ID);
        if (orderKey == null || orderKey.isEmpty()) {
            orderKey = getIntent().getStringExtra(EXTRA_ORDER_CODE);
        }
        OrderModel initialOrder = viewModel.getOrderByCode(orderKey);
        if (initialOrder == null) {
            finish();
            return;
        }

        bindBackButton(R.id.order_detail_back_btn);
        bottomBar = findViewById(R.id.od_bottom_bar);
        actionBtn = findViewById(R.id.od_action_btn);

        RecyclerView itemsRecycler = findViewById(R.id.od_items_recycler);
        itemsRecycler.setLayoutManager(new LinearLayoutManager(this));
        itemsAdapter = OrderCheckoutItemAdapter.fromOrderItems(Collections.emptyList());
        itemsAdapter.setInteractionListener(new OrderCheckoutItemAdapter.OnOrderItemInteractionListener() {
            @Override
            public void onItemClick(@NonNull OrderItemModel item) {
                if (viewModel.getProductById(item.getProductId()) == null) {
                    showToast("Sản phẩm này hiện không còn kinh doanh");
                    return;
                }
                Intent intent = new Intent(OrderDetailActivity.this, ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, item.getProductId());
                startActivity(intent);
            }

            @Override
            public void onReviewClick(@NonNull OrderItemModel item) {
                if (currentOrder == null || currentOrder.getStatus() != OrderModel.Status.COMPLETED) {
                    return;
                }
                ReviewDialog.show(
                        OrderDetailActivity.this,
                        reviewViewModel,
                        item.getProductId(),
                        item.getProductName(),
                        currentOrder.getOrderId(),
                        () -> {
                            reviewedProductIds.add(item.getProductId());
                            itemsAdapter.setReviewMode(true, reviewedProductIds);
                        }
                );
            }
        });
        itemsRecycler.setAdapter(itemsAdapter);

        bindOrder(initialOrder);

        viewModel.getOrdersLiveData().observe(this, orders -> {
            if (orders == null || orderKey == null || orderKey.isEmpty()) {
                return;
            }
            for (OrderModel candidate : orders) {
                if (orderKey.equals(candidate.getOrderId()) || orderKey.equals(candidate.getOrderCode())) {
                    bindOrder(candidate);
                    break;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (skipNextResumeReviewRefresh) {
            skipNextResumeReviewRefresh = false;
            return;
        }
        if (currentOrder != null && currentOrder.getStatus() == OrderModel.Status.COMPLETED) {
            refreshOrderReviewStatus(currentOrder);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        skipNextResumeReviewRefresh = false;
    }

    private void bindOrder(@NonNull OrderModel order) {
        currentOrder = order;
        ((TextView) findViewById(R.id.od_code)).setText("Mã đơn #" + order.getOrderCode());
        ((TextView) findViewById(R.id.od_created_at)).setText("Đặt lúc " + order.getCreatedAt());
        OrderStatusHelper.applyStatusBadge(findViewById(R.id.od_status_badge), order.getStatus());

        setupTimeline(order.getStatus());

        ((TextView) findViewById(R.id.od_receiver)).setText(order.getReceiverInfo());
        ((TextView) findViewById(R.id.od_address)).setText(order.getAddressSummary());
        ((TextView) findViewById(R.id.od_shipping_method)).setText(order.getShippingMethod());
        ((TextView) findViewById(R.id.od_payment_method)).setText(order.getPaymentMethod());
        ((TextView) findViewById(R.id.od_item_count)).setText(order.getTotalQuantity() + " sản phẩm");

        itemsAdapter.submitList(order.getOrderItems());
        if (order.getStatus() == OrderModel.Status.COMPLETED) {
            itemsAdapter.setReviewMode(true, reviewedProductIds);
            if (!order.getOrderId().equals(lastReviewFetchOrderId)) {
                lastReviewFetchOrderId = order.getOrderId();
                skipNextResumeReviewRefresh = true;
                refreshOrderReviewStatus(order);
            }
        } else {
            itemsAdapter.setReviewMode(false, null);
        }

        bindBottomActionBar(order);

        ((TextView) findViewById(R.id.od_subtotal)).setText(CurrencyFormatUtil.formatVnd(order.getSubtotal()));
        ((TextView) findViewById(R.id.od_shipping_fee)).setText(CurrencyFormatUtil.formatVnd(order.getShippingFee()));
        ((TextView) findViewById(R.id.od_total)).setText(CurrencyFormatUtil.formatVnd(order.getTotal()));

        View discountRow = findViewById(R.id.od_discount_row);
        if (order.getDiscount() > 0) {
            discountRow.setVisibility(View.VISIBLE);
            String label = !order.getVoucherCode().isEmpty()
                    ? "Giảm giá (" + order.getVoucherCode() + ")"
                    : "Giảm giá";
            ((TextView) findViewById(R.id.od_discount_label)).setText(label);
            ((TextView) findViewById(R.id.od_discount)).setText("-" + CurrencyFormatUtil.formatVnd(order.getDiscount()));
        } else {
            discountRow.setVisibility(View.GONE);
        }

        String paymentFetchKey = order.getOrderId() + "|" + order.getStatus().name();
        if (!paymentFetchKey.equals(lastPaymentFetchKey)) {
            lastPaymentFetchKey = paymentFetchKey;
            TextView paymentStatusBadge = findViewById(R.id.od_payment_status_badge);
            View transactionRow = findViewById(R.id.od_transaction_row);
            TextView transactionCodeView = findViewById(R.id.od_transaction_code);

            paymentViewModel.loadLatestPayment(order.getOrderId(), new ResourceCallback<PaymentModel>() {
                @Override
                public void onSuccess(PaymentModel payment) {
                    if (payment == null || !isAlive()) {
                        return;
                    }
                    PaymentStatusHelper.applyPaymentStatusBadge(paymentStatusBadge, payment.getStatus());
                    if (!payment.getTransactionCode().isEmpty()) {
                        transactionRow.setVisibility(View.VISIBLE);
                        transactionCodeView.setText(payment.getTransactionCode());
                    } else {
                        transactionRow.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(String message) {
                    Log.w(TAG, "Load latest payment failed for order " + order.getOrderId() + ": " + message);
                    // Giữ nguyên thông tin phương thức thanh toán từ Order
                }
            });
        }
    }

    private void refreshOrderReviewStatus(@NonNull OrderModel order) {
        List<String> productIds = new ArrayList<>();
        for (OrderItemModel item : order.getOrderItems()) {
            productIds.add(item.getProductId());
        }
        reviewViewModel.loadReviewedProductIdsForOrder(order.getOrderId(), productIds, new ResourceCallback<Set<String>>() {
            @Override
            public void onSuccess(Set<String> data) {
                if (!isAlive() || currentOrder == null
                        || !order.getOrderId().equals(currentOrder.getOrderId())
                        || currentOrder.getStatus() != OrderModel.Status.COMPLETED) {
                    return;
                }
                reviewedProductIds.clear();
                if (data != null) {
                    reviewedProductIds.addAll(data);
                }
                itemsAdapter.setReviewMode(true, reviewedProductIds);
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Load reviewed product ids failed for order " + order.getOrderId() + ": " + message);
                // Giữ nguyên trạng thái nút hiện tại nếu chưa truy vấn được
            }
        });
    }

    private void bindBottomActionBar(@NonNull OrderModel order) {
        if (order.canCancel()) {
            bottomBar.setVisibility(View.VISIBLE);
            actionBtn.setEnabled(true);
            actionBtn.setText("Hủy đơn hàng");
            actionBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_danger_soft)));
            actionBtn.setTextColor(ContextCompat.getColor(this, R.color.status_danger));
            actionBtn.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_danger)));
            actionBtn.setStrokeWidth(Math.round(1.2f * getResources().getDisplayMetrics().density));
            actionBtn.setOnClickListener(v -> OrderActionHelper.confirmCancelOrder(this, viewModel, order, actionBtn));
        } else if (order.getStatus() == OrderModel.Status.COMPLETED
                || order.getStatus() == OrderModel.Status.CANCELLED) {
            bottomBar.setVisibility(View.VISIBLE);
            actionBtn.setEnabled(true);
            actionBtn.setText("Mua lại");
            actionBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_accent)));
            actionBtn.setTextColor(ContextCompat.getColor(this, R.color.text_on_dark));
            actionBtn.setStrokeWidth(0);
            actionBtn.setOnClickListener(v -> OrderActionHelper.executeReorder(this, viewModel, order, actionBtn));
        } else {
            bottomBar.setVisibility(View.GONE);
            actionBtn.setOnClickListener(null);
        }
    }

    private void setupTimeline(OrderModel.Status status) {
        View timelineSection = findViewById(R.id.od_timeline_section);
        if (status == OrderModel.Status.CANCELLED) {
            timelineSection.setVisibility(View.GONE);
            return;
        }
        timelineSection.setVisibility(View.VISIBLE);

        int activeSteps;
        if (status == OrderModel.Status.COMPLETED) {
            activeSteps = 4;
        } else if (status == OrderModel.Status.SHIPPING) {
            activeSteps = 3;
        } else {
            activeSteps = 1;
        }

        int activeColor = ContextCompat.getColor(this, R.color.brand_accent);
        int inactiveColor = ContextCompat.getColor(this, R.color.border_subtle);
        int activeTextColor = ContextCompat.getColor(this, R.color.text_primary);
        int inactiveTextColor = ContextCompat.getColor(this, R.color.text_tertiary);

        View[] dots = {
                findViewById(R.id.od_step_1_dot),
                findViewById(R.id.od_step_2_dot),
                findViewById(R.id.od_step_3_dot),
                findViewById(R.id.od_step_4_dot)
        };
        TextView[] stepLabels = {
                findViewById(R.id.od_step_1_label),
                findViewById(R.id.od_step_2_label),
                findViewById(R.id.od_step_3_label),
                findViewById(R.id.od_step_4_label)
        };
        View[][] linePairs = {
                {findViewById(R.id.od_line_1_start), findViewById(R.id.od_line_1_end)},
                {findViewById(R.id.od_line_2_start), findViewById(R.id.od_line_2_end)},
                {findViewById(R.id.od_line_3_start), findViewById(R.id.od_line_3_end)}
        };

        for (int i = 0; i < dots.length; i++) {
            boolean reached = i < activeSteps;
            dots[i].getBackground().mutate().setTint(reached ? activeColor : inactiveColor);
            stepLabels[i].setTextColor(reached ? activeTextColor : inactiveTextColor);
            stepLabels[i].setTypeface(reached ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }
        for (int j = 0; j < linePairs.length; j++) {
            int lineColor = j < activeSteps - 1 ? activeColor : inactiveColor;
            linePairs[j][0].setBackgroundColor(lineColor);
            linePairs[j][1].setBackgroundColor(lineColor);
        }
    }
}
