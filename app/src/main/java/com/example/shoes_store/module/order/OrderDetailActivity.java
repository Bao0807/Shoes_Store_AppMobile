package com.example.shoes_store.module.order;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.core.UiInsetHelper;

/**
 * Màn hình Chi tiết đơn hàng kèm tiến trình 4 mốc giao hàng.
 */
public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_CODE = "extra_order_code";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.order_detail_activity);
        UiInsetHelper.applySystemBarInsets(findViewById(R.id.order_detail_root));

        OrderViewModel viewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        String orderCode = getIntent().getStringExtra(EXTRA_ORDER_CODE);
        OrderModel order = viewModel.getOrderByCode(orderCode);
        if (order == null) {
            finish();
            return;
        }

        findViewById(R.id.order_detail_back_btn).setOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.od_code)).setText("Mã đơn #" + order.getOrderCode());
        ((TextView) findViewById(R.id.od_created_at)).setText("Đặt lúc " + order.getCreatedAt());
        OrderHistoryAdapter.applyStatusStyle(findViewById(R.id.od_status_badge), order.getStatus(), this);

        setupTimeline(order.getStatus());

        ((TextView) findViewById(R.id.od_receiver)).setText(order.getReceiverInfo());
        ((TextView) findViewById(R.id.od_address)).setText(order.getAddressSummary());
        ((TextView) findViewById(R.id.od_shipping_payment))
                .setText(order.getShippingMethod() + " · " + order.getPaymentMethod());

        RecyclerView recyclerView = findViewById(R.id.od_items_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new OrderCheckoutItemAdapter(order.getItems()));

        ((TextView) findViewById(R.id.od_subtotal)).setText(CurrencyFormatUtil.formatVnd(order.getSubtotal()));
        ((TextView) findViewById(R.id.od_shipping_fee)).setText(CurrencyFormatUtil.formatVnd(order.getShippingFee()));
        ((TextView) findViewById(R.id.od_total)).setText(CurrencyFormatUtil.formatVnd(order.getTotal()));

        View discountRow = findViewById(R.id.od_discount_row);
        if (order.getDiscount() > 0) {
            discountRow.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.od_discount)).setText("-" + CurrencyFormatUtil.formatVnd(order.getDiscount()));
        }
    }

    private void setupTimeline(OrderModel.Status status) {
        if (status == OrderModel.Status.CANCELLED) {
            findViewById(R.id.od_timeline_container).setVisibility(View.GONE);
            findViewById(R.id.od_timeline_labels).setVisibility(View.GONE);
            return;
        }

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

        View[] dots = {
                findViewById(R.id.od_step_1_dot),
                findViewById(R.id.od_step_2_dot),
                findViewById(R.id.od_step_3_dot),
                findViewById(R.id.od_step_4_dot)
        };
        View[] lines = {
                findViewById(R.id.od_line_1),
                findViewById(R.id.od_line_2),
                findViewById(R.id.od_line_3)
        };

        for (int i = 0; i < dots.length; i++) {
            dots[i].getBackground().mutate().setTint(i < activeSteps ? activeColor : inactiveColor);
        }
        for (int i = 0; i < lines.length; i++) {
            lines[i].setBackgroundColor(i < activeSteps - 1 ? activeColor : inactiveColor);
        }
    }
}
