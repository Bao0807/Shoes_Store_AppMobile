package com.example.shoes_store.module.order;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.core.UiInsetHelper;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.payment.PaymentViewModel;

/**
 * Màn hình Chi tiết đơn hàng kèm tiến trình 4 mốc giao hàng và thông tin giao dịch Payment realtime.
 */
public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";
    public static final String EXTRA_ORDER_CODE = "extra_order_code";

    private OrderViewModel viewModel;
    private PaymentViewModel paymentViewModel;
    private RecyclerView itemsRecycler;
    private String orderKey = "";
    @Nullable
    private String lastPaymentFetchKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.order_detail_activity);
        UiInsetHelper.applySystemBarInsets(findViewById(R.id.order_detail_root));

        viewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        paymentViewModel = new ViewModelProvider(this).get(PaymentViewModel.class);
        orderKey = getIntent().getStringExtra(EXTRA_ORDER_ID);
        if (orderKey == null || orderKey.isEmpty()) {
            orderKey = getIntent().getStringExtra(EXTRA_ORDER_CODE);
        }
        OrderModel initialOrder = viewModel.getOrderByCode(orderKey);
        if (initialOrder == null) {
            finish();
            return;
        }

        findViewById(R.id.order_detail_back_btn).setOnClickListener(v -> finish());
        itemsRecycler = findViewById(R.id.od_items_recycler);
        itemsRecycler.setLayoutManager(new LinearLayoutManager(this));

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

    private void bindOrder(@NonNull OrderModel order) {
        ((TextView) findViewById(R.id.od_code)).setText("Mã đơn #" + order.getOrderCode());
        ((TextView) findViewById(R.id.od_created_at)).setText("Đặt lúc " + order.getCreatedAt());
        OrderHistoryAdapter.applyStatusStyle(findViewById(R.id.od_status_badge), order.getStatus(), this);

        setupTimeline(order.getStatus());

        ((TextView) findViewById(R.id.od_receiver)).setText(order.getReceiverInfo());
        ((TextView) findViewById(R.id.od_address)).setText(order.getAddressSummary());
        ((TextView) findViewById(R.id.od_shipping_method)).setText(order.getShippingMethod());
        ((TextView) findViewById(R.id.od_payment_method)).setText(order.getPaymentMethod());
        ((TextView) findViewById(R.id.od_item_count)).setText(order.getTotalQuantity() + " sản phẩm");

        itemsRecycler.setAdapter(OrderCheckoutItemAdapter.fromOrderItems(order.getOrderItems()));

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
                    if (payment == null || isFinishing() || isDestroyed()) {
                        return;
                    }
                    applyPaymentStatusBadge(paymentStatusBadge, payment.getStatus());
                    if (!payment.getTransactionCode().isEmpty()) {
                        transactionRow.setVisibility(View.VISIBLE);
                        transactionCodeView.setText(payment.getTransactionCode());
                    } else {
                        transactionRow.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(String message) {
                    // Giữ nguyên thông tin phương thức thanh toán từ Order
                }
            });
        }
    }

    private void applyPaymentStatusBadge(@NonNull TextView badge, @NonNull PaymentModel.Status status) {
        badge.setVisibility(View.VISIBLE);
        badge.setText(status.getLabel());
        int bgRes;
        int textRes;
        switch (status) {
            case PAID:
                bgRes = R.color.status_success_soft;
                textRes = R.color.status_success;
                break;
            case REFUNDED:
                bgRes = R.color.status_info_soft;
                textRes = R.color.status_info;
                break;
            case FAILED:
                bgRes = R.color.status_danger_soft;
                textRes = R.color.status_danger;
                break;
            case UNPAID:
            default:
                bgRes = R.color.status_warning_soft;
                textRes = R.color.status_warning;
                break;
        }
        badge.setTextColor(ContextCompat.getColor(this, textRes));
        if (badge.getBackground() != null) {
            badge.getBackground().mutate().setTint(ContextCompat.getColor(this, bgRes));
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
