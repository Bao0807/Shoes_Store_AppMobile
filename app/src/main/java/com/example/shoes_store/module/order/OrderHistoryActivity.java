package com.example.shoes_store.module.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseActivity;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Lịch sử đơn hàng với 4 tab trạng thái.
 */
public class OrderHistoryActivity extends BaseActivity {

    public static final String EXTRA_INITIAL_TAB = "extra_initial_tab";

    private OrderViewModel viewModel;
    private OrderHistoryAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyText;
    private int currentTab = 0;
    private List<OrderModel> allOrders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContent(R.layout.order_history_activity, R.id.order_history_root);

        viewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        currentTab = getIntent().getIntExtra(EXTRA_INITIAL_TAB, 0);

        recyclerView = findViewById(R.id.order_history_recycler);
        emptyText = findViewById(R.id.order_history_empty);
        TabLayout tabLayout = findViewById(R.id.order_history_tabs);

        bindBackButton(R.id.order_history_back_btn);

        adapter = new OrderHistoryAdapter(new OrderHistoryAdapter.OnOrderActionListener() {
            @Override
            public void onOrderClick(OrderModel order) {
                Intent intent = new Intent(OrderHistoryActivity.this, OrderDetailActivity.class);
                intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.getOrderId());
                intent.putExtra(OrderDetailActivity.EXTRA_ORDER_CODE, order.getOrderCode());
                startActivity(intent);
            }

            @Override
            public void onOrderActionClick(OrderModel order) {
                if (order.canCancel()) {
                    OrderActionHelper.confirmCancelOrder(OrderHistoryActivity.this, viewModel, order, null);
                } else {
                    OrderActionHelper.executeReorder(OrderHistoryActivity.this, viewModel, order, null);
                }
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                renderOrdersForTab();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        TabLayout.Tab initialTab = tabLayout.getTabAt(currentTab);
        if (initialTab != null) {
            initialTab.select();
        }

        viewModel.getOrdersLiveData().observe(this, orders -> {
            allOrders = orders != null ? orders : new ArrayList<>();
            renderOrdersForTab();
        });
    }

    private void renderOrdersForTab() {
        OrderModel.Status targetStatus = statusForTab(currentTab);
        List<OrderModel> filtered = new ArrayList<>();
        for (OrderModel order : allOrders) {
            if (order.getStatus() == targetStatus) {
                filtered.add(order);
            }
        }
        adapter.submitList(filtered);
        boolean empty = filtered.isEmpty();
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private OrderModel.Status statusForTab(int position) {
        switch (position) {
            case 1:
                return OrderModel.Status.SHIPPING;
            case 2:
                return OrderModel.Status.COMPLETED;
            case 3:
                return OrderModel.Status.CANCELLED;
            case 0:
            default:
                return OrderModel.Status.PENDING;
        }
    }
}
