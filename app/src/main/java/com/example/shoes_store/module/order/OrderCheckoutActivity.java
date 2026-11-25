package com.example.shoes_store.module.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.MainActivity;
import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseActivity;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.core.DialogHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.profile.ProfileAddressActivity;
import com.example.shoes_store.module.profile.ProfileAddressModel;
import com.example.shoes_store.module.voucher.VoucherBottomSheet;
import com.example.shoes_store.module.voucher.VoucherModel;
import com.example.shoes_store.module.voucher.VoucherService;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Thanh toán / Đặt hàng.
 */
public class OrderCheckoutActivity extends BaseActivity {

    public static final String EXTRA_DISCOUNT_AMOUNT = "extra_discount_amount";
    public static final String EXTRA_VOUCHER_CODE = "extra_voucher_code";
    public static final String EXTRA_DIRECT_PRODUCT_ID = "extra_direct_product_id";
    public static final String EXTRA_DIRECT_VARIANT_ID = "extra_direct_variant_id";
    public static final String EXTRA_DIRECT_SIZE = "extra_direct_size";
    public static final String EXTRA_DIRECT_COLOR = "extra_direct_color";

    private OrderViewModel viewModel;
    private final List<CartItemModel> checkoutItems = new ArrayList<>();
    private ProfileAddressModel selectedAddress;
    @Nullable
    private VoucherModel selectedVoucher;
    @Nullable
    private String pendingVoucherCode;
    private boolean isDirectBuy = false;
    private boolean isSubmitting = false;
    private long discountAmount = 0L;
    private long shippingFee = OrderService.STANDARD_SHIPPING_FEE;
    private String shippingMethod = OrderService.SHIPPING_METHOD_STANDARD;
    private PaymentModel.Method paymentMethod = PaymentModel.Method.COD;

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
    private MaterialButton placeOrderBtn;

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
        setEdgeToEdgeContent(R.layout.order_checkout_activity, R.id.checkout_top_bar, R.id.checkout_bottom_bar);

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
        placeOrderBtn = findViewById(R.id.checkout_place_order_btn);

        bindBackButton(R.id.checkout_back_btn);
        findViewById(R.id.checkout_change_address_btn).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileAddressActivity.class);
            intent.putExtra(ProfileAddressActivity.EXTRA_PICK_MODE, true);
            pickAddressLauncher.launch(intent);
        });
        findViewById(R.id.checkout_voucher_row).setOnClickListener(v -> openVoucherSheet());

        loadCheckoutData();
        selectedAddress = viewModel.getDefaultAddress();
        renderAddress();
        observeAddresses();
        observeVouchers();
        setupItemsRecycler();
        setupRadioGroups();
        recalculateTotals();

        placeOrderBtn.setOnClickListener(v -> submitOrder());
    }

    private void observeVouchers() {
        viewModel.getVouchersLiveData().observe(this, vouchers -> {
            if (vouchers == null) {
                return;
            }
            String targetCode = selectedVoucher != null ? selectedVoucher.getCode() : pendingVoucherCode;
            if (targetCode == null || targetCode.trim().isEmpty()) {
                return;
            }
            VoucherModel matched = VoucherService.findByCode(vouchers, targetCode);
            selectedVoucher = matched;
            pendingVoucherCode = null;
            if (matched == null) {
                discountAmount = 0L;
            }
            recalculateTotals();
        });
    }

    private void observeAddresses() {
        viewModel.getAddressesLiveData().observe(this, addresses -> {
            if (addresses == null || addresses.isEmpty()) {
                selectedAddress = null;
                renderAddress();
                return;
            }
            if (selectedAddress == null) {
                selectedAddress = pickDefaultOrFirst(addresses);
                renderAddress();
                return;
            }
            String currentId = selectedAddress.getId();
            ProfileAddressModel updatedMatch = null;
            for (ProfileAddressModel item : addresses) {
                if (currentId.equals(item.getId())) {
                    updatedMatch = item;
                    break;
                }
            }
            selectedAddress = updatedMatch != null ? updatedMatch : pickDefaultOrFirst(addresses);
            renderAddress();
        });
    }

    private ProfileAddressModel pickDefaultOrFirst(List<ProfileAddressModel> addresses) {
        for (ProfileAddressModel address : addresses) {
            if (address.isDefaultAddress()) {
                return address;
            }
        }
        return addresses.get(0);
    }

    private void loadCheckoutData() {
        String directProductId = getIntent().getStringExtra(EXTRA_DIRECT_PRODUCT_ID);
        if (directProductId != null) {
            isDirectBuy = true;
            ProductModel product = viewModel.getProductById(directProductId);
            String variantId = getIntent().getStringExtra(EXTRA_DIRECT_VARIANT_ID);
            String size = getIntent().getStringExtra(EXTRA_DIRECT_SIZE);
            String color = getIntent().getStringExtra(EXTRA_DIRECT_COLOR);
            if (product != null) {
                checkoutItems.add(new CartItemModel(
                        "direct_1",
                        product,
                        variantId != null ? variantId : "",
                        size != null ? size : "42",
                        color != null ? color : "Mặc định",
                        1,
                        true,
                        0L
                ));
            }
        } else {
            discountAmount = getIntent().getLongExtra(EXTRA_DISCOUNT_AMOUNT, 0L);
            pendingVoucherCode = getIntent().getStringExtra(EXTRA_VOUCHER_CODE);
            selectedVoucher = viewModel.findVoucherByCode(pendingVoucherCode);
            checkoutItems.addAll(viewModel.getSelectedCartItems());
        }

        if (checkoutItems.isEmpty()) {
            showToast("Không có sản phẩm để thanh toán");
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
            boolean isExpress = checkedId == R.id.rb_ship_express;
            shippingMethod = isExpress
                    ? OrderService.SHIPPING_METHOD_EXPRESS
                    : OrderService.SHIPPING_METHOD_STANDARD;
            shippingFee = OrderService.resolveShippingFee(isExpress);
            recalculateTotals();
        });

        RadioGroup paymentGroup = findViewById(R.id.checkout_payment_group);
        paymentGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_pay_qr) {
                paymentMethod = PaymentModel.Method.BANK_QR;
            } else if (checkedId == R.id.rb_pay_wallet) {
                paymentMethod = PaymentModel.Method.E_WALLET;
            } else {
                paymentMethod = PaymentModel.Method.COD;
            }
        });
    }

    private void openVoucherSheet() {
        long subtotal = getSubtotal();
        if (subtotal <= 0) {
            return;
        }
        VoucherBottomSheet sheet = VoucherBottomSheet.newInstance(
                viewModel.getVouchers(),
                subtotal,
                selectedVoucher,
                picked -> {
                    selectedVoucher = picked;
                    pendingVoucherCode = null;
                    discountAmount = picked != null ? picked.getDiscountAmount() : 0L;
                    recalculateTotals();
                }
        );
        sheet.show(getSupportFragmentManager(), "checkout_voucher_sheet");
    }

    private long getSubtotal() {
        return OrderService.calculateSubtotal(checkoutItems);
    }

    private void recalculateTotals() {
        long subtotal = getSubtotal();
        if (selectedVoucher != null) {
            if (selectedVoucher.isEligible(subtotal)) {
                discountAmount = VoucherService.resolveDiscountAmount(selectedVoucher, subtotal);
                voucherTitleText.setText(getString(R.string.voucher_applied_title, selectedVoucher.getCode()));
                voucherSubtitleText.setText(getString(R.string.voucher_applied_subtitle, CurrencyFormatUtil.formatVnd(discountAmount)));
                voucherActionText.setText(R.string.voucher_action_change);
            } else {
                selectedVoucher = null;
                pendingVoucherCode = null;
                discountAmount = 0L;
                voucherTitleText.setText(R.string.voucher_default_title);
                voucherSubtitleText.setText(R.string.voucher_default_subtitle);
                voucherActionText.setText(R.string.voucher_action_pick);
            }
        } else {
            if (pendingVoucherCode != null && !pendingVoucherCode.trim().isEmpty()) {
                discountAmount = Math.min(discountAmount, subtotal);
            } else {
                discountAmount = 0L;
            }
            voucherTitleText.setText(R.string.voucher_default_title);
            voucherSubtitleText.setText(R.string.voucher_default_subtitle);
            voucherActionText.setText(R.string.voucher_action_pick);
        }

        long total = OrderService.calculateTotal(subtotal, shippingFee, discountAmount);

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
        if (isSubmitting) {
            return;
        }
        if (selectedAddress == null) {
            showToast("Vui lòng chọn địa chỉ nhận hàng");
            return;
        }
        if (paymentMethod == PaymentModel.Method.COD) {
            executeSubmitOrder();
            return;
        }
        long total = OrderService.calculateTotal(getSubtotal(), shippingFee, discountAmount);
        String message = "Phương thức: " + paymentMethod.getLabel()
                + "\nSố tiền thanh toán: " + CurrencyFormatUtil.formatVnd(total)
                + "\nNội dung chuyển khoản: SHOESSTORE " + selectedAddress.getPhone()
                + "\n\nNhấn \"Tôi đã thanh toán\" sau khi hoàn tất giao dịch giả lập.";
        DialogHelper.showConfirmDialog(
                this,
                "Xác nhận thanh toán",
                message,
                "Hủy",
                "Tôi đã thanh toán",
                this::executeSubmitOrder
        );
    }

    private void executeSubmitOrder() {
        if (isSubmitting || selectedAddress == null) {
            return;
        }
        isSubmitting = true;
        placeOrderBtn.setEnabled(false);
        placeOrderBtn.setText("Đang xử lý...");

        String voucherCode = selectedVoucher != null ? selectedVoucher.getCode() : null;
        viewModel.placeOrder(
                checkoutItems,
                selectedAddress,
                shippingMethod,
                shippingFee,
                paymentMethod,
                voucherCode,
                discountAmount,
                !isDirectBuy,
                new ResourceCallback<OrderModel>() {
                    @Override
                    public void onSuccess(OrderModel created) {
                        isSubmitting = false;
                        if (!isAlive()) {
                            return;
                        }
                        placeOrderBtn.setEnabled(true);
                        placeOrderBtn.setText(R.string.checkout_place_order);
                        OrderSuccessDialog.show(OrderCheckoutActivity.this, created.getOrderCode(), new OrderSuccessDialog.OnDialogActionListener() {
                            @Override
                            public void onViewOrders() {
                                Intent intent = new Intent(OrderCheckoutActivity.this, OrderHistoryActivity.class);
                                intent.putExtra(OrderHistoryActivity.EXTRA_INITIAL_TAB, 0);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onGoHome() {
                                MainActivity.openTab(OrderCheckoutActivity.this, R.id.nav_home);
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        isSubmitting = false;
                        if (!isAlive()) {
                            return;
                        }
                        placeOrderBtn.setEnabled(true);
                        placeOrderBtn.setText(R.string.checkout_place_order);
                        showLongToast(message);
                    }
                }
        );
    }
}
