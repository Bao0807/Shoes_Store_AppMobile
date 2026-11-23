package com.example.shoes_store.module.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.MainActivity;
import com.example.shoes_store.R;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.core.UiInsetHelper;
import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.cart.CartVoucherBottomSheet;
import com.example.shoes_store.module.cart.CartVoucherModel;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.profile.ProfileAddressActivity;
import com.example.shoes_store.module.profile.ProfileAddressModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Thanh toán / Đặt hàng.
 */
public class OrderCheckoutActivity extends AppCompatActivity {

    public static final String EXTRA_DISCOUNT_AMOUNT = "extra_discount_amount";
    public static final String EXTRA_VOUCHER_CODE = "extra_voucher_code";
    public static final String EXTRA_DIRECT_PRODUCT_ID = "extra_direct_product_id";
    public static final String EXTRA_DIRECT_SIZE = "extra_direct_size";
    public static final String EXTRA_DIRECT_COLOR = "extra_direct_color";

    private static final long STANDARD_SHIPPING_FEE = 30_000L;
    private static final long EXPRESS_SHIPPING_FEE = 55_000L;

    private OrderViewModel viewModel;
    private final List<CartItemModel> checkoutItems = new ArrayList<>();
    private ProfileAddressModel selectedAddress;
    @Nullable
    private CartVoucherModel selectedVoucher;
    private boolean isDirectBuy = false;
    private long discountAmount = 0L;
    private long shippingFee = STANDARD_SHIPPING_FEE;
    private String shippingMethod = "Giao tiêu chuẩn";
    private String paymentMethod = "Thanh toán khi nhận hàng (COD)";

    private TextView addressReceiverText;
    private TextView addressDetailText;
    private TextView voucherTitleText;
    private TextView voucherSubtitleText;
    private TextView voucherActionText;
    private TextView subtotalText;
    private TextView shippingFeeText;
    private View discountRow;
    private TextView discountText;
    private TextView totalText;
    private TextView bottomTotalText;

    private final ActivityResultLauncher<Intent> pickAddressLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String addressId = result.getData().getStringExtra(ProfileAddressActivity.EXTRA_SELECTED_ADDRESS_ID);
                    ProfileAddressModel picked = viewModel.findAddressById(addressId);
                    if (picked != null) {
                        selectedAddress = picked;
                        renderAddress();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.order_checkout_activity);

        UiInsetHelper.applyTopInset(findViewById(R.id.checkout_top_bar));
        UiInsetHelper.applyBottomInset(findViewById(R.id.checkout_bottom_bar));

        viewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        addressReceiverText = findViewById(R.id.checkout_address_receiver);
        addressDetailText = findViewById(R.id.checkout_address_detail);
        voucherTitleText = findViewById(R.id.checkout_voucher_title);
        voucherSubtitleText = findViewById(R.id.checkout_voucher_subtitle);
        voucherActionText = findViewById(R.id.checkout_voucher_action);
        subtotalText = findViewById(R.id.checkout_subtotal);
        shippingFeeText = findViewById(R.id.checkout_shipping_fee);
        discountRow = findViewById(R.id.checkout_discount_row);
        discountText = findViewById(R.id.checkout_discount);
        totalText = findViewById(R.id.checkout_total);
        bottomTotalText = findViewById(R.id.checkout_bottom_total);

        findViewById(R.id.checkout_back_btn).setOnClickListener(v -> finish());
        findViewById(R.id.checkout_change_address_btn).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileAddressActivity.class);
            intent.putExtra(ProfileAddressActivity.EXTRA_PICK_MODE, true);
            pickAddressLauncher.launch(intent);
        });
        findViewById(R.id.checkout_voucher_row).setOnClickListener(v -> openVoucherSheet());

        loadCheckoutData();
        selectedAddress = viewModel.getDefaultAddress();
        renderAddress();
        setupItemsRecycler();
        setupRadioGroups();
        recalculateTotals();

        findViewById(R.id.checkout_place_order_btn).setOnClickListener(v -> submitOrder());
    }

    private void loadCheckoutData() {
        String directProductId = getIntent().getStringExtra(EXTRA_DIRECT_PRODUCT_ID);
        if (directProductId != null) {
            isDirectBuy = true;
            ProductModel product = viewModel.getProductById(directProductId);
            String size = getIntent().getStringExtra(EXTRA_DIRECT_SIZE);
            String color = getIntent().getStringExtra(EXTRA_DIRECT_COLOR);
            if (product != null) {
                checkoutItems.add(new CartItemModel(
                        "direct_1",
                        product,
                        size != null ? size : "42",
                        color != null ? color : "Mặc định",
                        1,
                        true
                ));
            }
        } else {
            discountAmount = getIntent().getLongExtra(EXTRA_DISCOUNT_AMOUNT, 0L);
            String voucherCode = getIntent().getStringExtra(EXTRA_VOUCHER_CODE);
            selectedVoucher = viewModel.findVoucherByCode(voucherCode);
            checkoutItems.addAll(viewModel.getSelectedCartItems());
        }

        if (checkoutItems.isEmpty()) {
            Toast.makeText(this, "Không có sản phẩm để thanh toán", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void renderAddress() {
        if (selectedAddress == null) {
            addressReceiverText.setText("Chưa có địa chỉ giao hàng");
            addressDetailText.setText("Nhấn Đổi để thêm địa chỉ mới");
            return;
        }
        addressReceiverText.setText(selectedAddress.getReceiverName() + " · " + selectedAddress.getPhone());
        addressDetailText.setText(selectedAddress.getAddressDetail());
    }

    private void setupItemsRecycler() {
        RecyclerView recyclerView = findViewById(R.id.checkout_items_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new OrderCheckoutItemAdapter(checkoutItems));
    }

    private void setupRadioGroups() {
        RadioGroup shippingGroup = findViewById(R.id.checkout_shipping_group);
        shippingGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_ship_express) {
                shippingMethod = "Giao nhanh 24h";
                shippingFee = EXPRESS_SHIPPING_FEE;
            } else {
                shippingMethod = "Giao tiêu chuẩn";
                shippingFee = STANDARD_SHIPPING_FEE;
            }
            recalculateTotals();
        });

        RadioGroup paymentGroup = findViewById(R.id.checkout_payment_group);
        paymentGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_pay_qr) {
                paymentMethod = "Chuyển khoản QR";
            } else if (checkedId == R.id.rb_pay_wallet) {
                paymentMethod = "Ví điện tử";
            } else {
                paymentMethod = "Thanh toán khi nhận hàng (COD)";
            }
        });
    }

    private void openVoucherSheet() {
        long subtotal = getSubtotal();
        if (subtotal <= 0) {
            return;
        }
        CartVoucherBottomSheet sheet = CartVoucherBottomSheet.newInstance(
                viewModel.getVouchers(),
                subtotal,
                selectedVoucher,
                picked -> {
                    selectedVoucher = picked;
                    discountAmount = picked != null ? picked.getDiscountAmount() : 0L;
                    recalculateTotals();
                }
        );
        sheet.show(getSupportFragmentManager(), "checkout_voucher_sheet");
    }

    private long getSubtotal() {
        long subtotal = 0L;
        for (CartItemModel item : checkoutItems) {
            subtotal += item.getLineTotal();
        }
        return subtotal;
    }

    private void recalculateTotals() {
        long subtotal = getSubtotal();
        if (selectedVoucher != null) {
            if (selectedVoucher.isEligible(subtotal)) {
                discountAmount = Math.min(selectedVoucher.getDiscountAmount(), subtotal);
                voucherTitleText.setText("Mã " + selectedVoucher.getCode());
                voucherSubtitleText.setText("Đã giảm " + CurrencyFormatUtil.formatVnd(discountAmount));
                voucherActionText.setText("Đổi mã");
            } else {
                selectedVoucher = null;
                discountAmount = 0L;
                voucherTitleText.setText("Voucher của cửa hàng");
                voucherSubtitleText.setText("Bấm để chọn hoặc nhập mã giảm giá");
                voucherActionText.setText("Chọn mã");
            }
        } else {
            discountAmount = Math.min(discountAmount, subtotal);
            voucherTitleText.setText("Voucher của cửa hàng");
            voucherSubtitleText.setText("Bấm để chọn hoặc nhập mã giảm giá");
            voucherActionText.setText("Chọn mã");
        }

        long total = Math.max(0L, subtotal + shippingFee - discountAmount);

        subtotalText.setText(CurrencyFormatUtil.formatVnd(subtotal));
        shippingFeeText.setText(CurrencyFormatUtil.formatVnd(shippingFee));
        if (discountAmount > 0) {
            discountRow.setVisibility(View.VISIBLE);
            discountText.setText("-" + CurrencyFormatUtil.formatVnd(discountAmount));
        } else {
            discountRow.setVisibility(View.GONE);
        }
        totalText.setText(CurrencyFormatUtil.formatVnd(total));
        bottomTotalText.setText(CurrencyFormatUtil.formatVnd(total));
    }

    private void submitOrder() {
        if (selectedAddress == null) {
            Toast.makeText(this, "Vui lòng chọn địa chỉ nhận hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        OrderModel created = viewModel.placeOrder(
                checkoutItems,
                selectedAddress,
                shippingMethod,
                shippingFee,
                paymentMethod,
                discountAmount,
                !isDirectBuy
        );
        OrderSuccessDialog.show(this, created.getOrderCode(), new OrderSuccessDialog.OnDialogActionListener() {
            @Override
            public void onViewOrders() {
                Intent intent = new Intent(OrderCheckoutActivity.this, OrderHistoryActivity.class);
                intent.putExtra(OrderHistoryActivity.EXTRA_INITIAL_TAB, 0);
                startActivity(intent);
                finish();
            }

            @Override
            public void onGoHome() {
                Intent intent = new Intent(OrderCheckoutActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(MainActivity.EXTRA_TARGET_TAB, R.id.nav_home);
                startActivity(intent);
                finish();
            }
        });
    }
}
