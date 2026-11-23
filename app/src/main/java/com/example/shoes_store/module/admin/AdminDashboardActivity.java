package com.example.shoes_store.module.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.core.UiInsetHelper;
import com.example.shoes_store.module.auth.AuthLoginActivity;
import com.example.shoes_store.module.auth.AuthRepository;
import com.example.shoes_store.module.order.OrderItemModel;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.voucher.VoucherModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Bảng điều khiển Quản trị viên (Admin):
 * - Tab 0: Duyệt đơn hàng (Xác nhận & Giao, Đã giao thành công, Hủy đơn hoàn kho/voucher)
 * - Tab 1: Quản lý Sản phẩm & Tồn kho biến thể
 * - Tab 2: Quản lý Kho Voucher
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private AdminViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private View orderFilterScroll;
    private MaterialButton addBtn;
    private TextView kpiPendingText;
    private TextView kpiShippingText;
    private TextView kpiRevenueText;

    private AdminOrderAdapter orderAdapter;
    private AdminProductAdapter productAdapter;
    private AdminVoucherAdapter voucherAdapter;

    private int activeTab = 0;
    private List<OrderModel> latestOrders = new ArrayList<>();
    private List<ProductModel> latestProducts = new ArrayList<>();
    private List<VoucherModel> latestVouchers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.admin_dashboard_activity);
        UiInsetHelper.applySystemBarInsets(findViewById(R.id.admin_root));

        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        recyclerView = findViewById(R.id.admin_recycler);
        emptyText = findViewById(R.id.admin_empty_text);
        orderFilterScroll = findViewById(R.id.admin_order_filter_scroll);
        addBtn = findViewById(R.id.admin_add_btn);
        kpiPendingText = findViewById(R.id.admin_kpi_pending);
        kpiShippingText = findViewById(R.id.admin_kpi_shipping);
        kpiRevenueText = findViewById(R.id.admin_kpi_revenue);

        findViewById(R.id.admin_back_btn).setOnClickListener(v -> finish());
        findViewById(R.id.admin_logout_btn).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Đăng xuất Admin")
                        .setMessage("Bạn muốn đăng xuất khỏi tài khoản Quản trị viên?")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Đăng xuất", (d, w) -> {
                            AuthRepository.getInstance().logout();
                            Toast.makeText(this, "Đã đăng xuất tài khoản Admin", Toast.LENGTH_SHORT).show();
                            AuthLoginActivity.open(this);
                            finish();
                        })
                        .show());

        setupAdapters();
        setupTabsAndFilters();
        observeData();
    }

    private void setupAdapters() {
        orderAdapter = new AdminOrderAdapter(new AdminOrderAdapter.OnAdminOrderActionListener() {
            @Override
            public void onOrderCardClick(@NonNull OrderModel order) {
                showOrderDetailDialog(order);
            }

            @Override
            public void onAdvanceStatus(@NonNull OrderModel order, @NonNull OrderModel.Status nextStatus) {
                String actionTitle = nextStatus == OrderModel.Status.SHIPPING
                        ? "Xác nhận & Giao đơn #" + order.getOrderCode()
                        : "Xác nhận Đã giao đơn #" + order.getOrderCode();
                String actionMsg = nextStatus == OrderModel.Status.SHIPPING
                        ? "Chuyển đơn sang trạng thái Đang giao và gửi thông báo cho khách hàng?"
                        : "Xác nhận khách đã nhận giày? (Đơn COD sẽ tự chuyển sang Đã thanh toán và mở khóa quyền đánh giá sản phẩm cho khách).";

                new AlertDialog.Builder(AdminDashboardActivity.this)
                        .setTitle(actionTitle)
                        .setMessage(actionMsg)
                        .setNegativeButton("Đóng", null)
                        .setPositiveButton("Xác nhận", (d, w) ->
                                viewModel.updateOrderStatus(order.getOrderId(), nextStatus, toastCallback(
                                        "Đã cập nhật đơn #" + order.getOrderCode() + " → " + nextStatus.getLabel()
                                )))
                        .show();
            }

            @Override
            public void onCancelOrder(@NonNull OrderModel order) {
                new AlertDialog.Builder(AdminDashboardActivity.this)
                        .setTitle("Hủy đơn #" + order.getOrderCode())
                        .setMessage("Hủy đơn hàng này, tự động hoàn tồn kho các size và hoàn lượt voucher cho khách?")
                        .setNegativeButton("Không", null)
                        .setPositiveButton("Hủy đơn", (d, w) ->
                                viewModel.updateOrderStatus(
                                        order.getOrderId(),
                                        OrderModel.Status.CANCELLED,
                                        toastCallback("Đã hủy đơn #" + order.getOrderCode() + " và hoàn kho")
                                ))
                        .show();
            }
        });

        productAdapter = new AdminProductAdapter(new AdminProductAdapter.OnAdminProductActionListener() {
            @Override
            public void onEditProduct(@NonNull ProductModel product) {
                AdminProductDialog.show(AdminDashboardActivity.this, viewModel, product);
            }

            @Override
            public void onToggleActive(@NonNull ProductModel product, boolean isActive) {
                viewModel.toggleProductActive(
                        product.getId(),
                        isActive,
                        toastCallback(isActive ? "Đã mở bán " + product.getName() : "Đã tạm ẩn " + product.getName())
                );
            }
        });

        voucherAdapter = new AdminVoucherAdapter(new AdminVoucherAdapter.OnAdminVoucherActionListener() {
            @Override
            public void onEditVoucher(@NonNull VoucherModel voucher) {
                AdminVoucherDialog.show(AdminDashboardActivity.this, viewModel, voucher);
            }

            @Override
            public void onToggleActive(@NonNull VoucherModel voucher, boolean isActive) {
                viewModel.toggleVoucherActive(
                        voucher.getCode(),
                        isActive,
                        toastCallback(isActive ? "Đã kích hoạt mã " + voucher.getCode() : "Đã tạm tắt mã " + voucher.getCode())
                );
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(orderAdapter);
    }

    private void setupTabsAndFilters() {
        TabLayout tabLayout = findViewById(R.id.admin_tabs);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                activeTab = tab.getPosition();
                syncActiveTabUi();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        ChipGroup orderFilterChips = findViewById(R.id.admin_order_filter_chips);
        orderFilterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.admin_chip_order_pending) {
                viewModel.setOrderFilterStatus(OrderModel.Status.PENDING);
            } else if (id == R.id.admin_chip_order_shipping) {
                viewModel.setOrderFilterStatus(OrderModel.Status.SHIPPING);
            } else if (id == R.id.admin_chip_order_completed) {
                viewModel.setOrderFilterStatus(OrderModel.Status.COMPLETED);
            } else if (id == R.id.admin_chip_order_cancelled) {
                viewModel.setOrderFilterStatus(OrderModel.Status.CANCELLED);
            } else {
                viewModel.setOrderFilterStatus(null);
            }
            renderCurrentTabList();
        });

        addBtn.setOnClickListener(v -> {
            if (activeTab == 1) {
                AdminProductDialog.show(this, viewModel, null);
            } else if (activeTab == 2) {
                AdminVoucherDialog.show(this, viewModel, null);
            }
        });
    }

    private void observeData() {
        viewModel.getAllOrdersLiveData().observe(this, orders -> {
            latestOrders = orders != null ? orders : new ArrayList<>();
            updateKpis();
            if (activeTab == 0) {
                renderCurrentTabList();
            }
        });

        viewModel.getAllProductsLiveData().observe(this, products -> {
            latestProducts = products != null ? products : new ArrayList<>();
            if (activeTab == 1) {
                renderCurrentTabList();
            }
        });

        viewModel.getAllVouchersLiveData().observe(this, vouchers -> {
            latestVouchers = vouchers != null ? vouchers : new ArrayList<>();
            if (activeTab == 2) {
                renderCurrentTabList();
            }
        });
    }

    private void updateKpis() {
        int pending = AdminService.countOrdersByStatus(latestOrders, OrderModel.Status.PENDING);
        int shipping = AdminService.countOrdersByStatus(latestOrders, OrderModel.Status.SHIPPING);
        long revenue = AdminService.calculateCompletedRevenue(latestOrders);

        kpiPendingText.setText(String.valueOf(pending));
        kpiShippingText.setText(String.valueOf(shipping));
        kpiRevenueText.setText(CurrencyFormatUtil.formatVnd(revenue));
    }

    private void syncActiveTabUi() {
        if (activeTab == 0) {
            orderFilterScroll.setVisibility(View.VISIBLE);
            addBtn.setVisibility(View.GONE);
            recyclerView.setAdapter(orderAdapter);
        } else if (activeTab == 1) {
            orderFilterScroll.setVisibility(View.GONE);
            addBtn.setVisibility(View.VISIBLE);
            addBtn.setText("+ Thêm giày");
            recyclerView.setAdapter(productAdapter);
        } else {
            orderFilterScroll.setVisibility(View.GONE);
            addBtn.setVisibility(View.VISIBLE);
            addBtn.setText("+ Thêm mã");
            recyclerView.setAdapter(voucherAdapter);
        }
        renderCurrentTabList();
    }

    private void renderCurrentTabList() {
        boolean isEmpty;
        if (activeTab == 0) {
            List<OrderModel> filtered = viewModel.filterOrders(latestOrders);
            orderAdapter.submitList(filtered);
            isEmpty = filtered.isEmpty();
        } else if (activeTab == 1) {
            productAdapter.submitList(latestProducts);
            isEmpty = latestProducts.isEmpty();
        } else {
            voucherAdapter.submitList(latestVouchers);
            isEmpty = latestVouchers.isEmpty();
        }
        emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showOrderDetailDialog(@NonNull OrderModel order) {
        StringBuilder sb = new StringBuilder();
        sb.append("Trạng thái: ").append(order.getStatus().getLabel()).append("\n");
        sb.append("Thời gian: ").append(order.getCreatedAt()).append("\n");
        sb.append("Khách nhận: ").append(order.getReceiverInfo()).append("\n");
        sb.append("Địa chỉ: ").append(order.getAddressSummary()).append("\n");
        sb.append("Vận chuyển: ").append(order.getShippingMethod()).append("\n");
        sb.append("Thanh toán: ").append(order.getPaymentMethod()).append("\n\n");
        sb.append("Sản phẩm:\n");
        for (OrderItemModel item : order.getOrderItems()) {
            sb.append("• ").append(item.getProductName())
                    .append(" (EU ").append(item.getSize()).append(" · ").append(item.getColorName()).append(")")
                    .append(" x").append(item.getQuantity())
                    .append(" = ").append(CurrencyFormatUtil.formatVnd(item.getLineTotal()))
                    .append("\n");
        }
        sb.append("\nTạm tính: ").append(CurrencyFormatUtil.formatVnd(order.getSubtotal()));
        sb.append("\nPhí ship: ").append(CurrencyFormatUtil.formatVnd(order.getShippingFee()));
        if (order.getDiscount() > 0) {
            sb.append("\nGiảm giá");
            if (!order.getVoucherCode().isEmpty()) {
                sb.append(" (").append(order.getVoucherCode()).append(")");
            }
            sb.append(": -").append(CurrencyFormatUtil.formatVnd(order.getDiscount()));
        }
        sb.append("\nTổng thanh toán: ").append(CurrencyFormatUtil.formatVnd(order.getTotal()));

        new AlertDialog.Builder(this)
                .setTitle("Chi tiết đơn #" + order.getOrderCode())
                .setMessage(sb.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }

    @NonNull
    private ResourceCallback<Void> toastCallback(@NonNull String successMsg) {
        return new ResourceCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(AdminDashboardActivity.this, successMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(AdminDashboardActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }
        };
    }
}
