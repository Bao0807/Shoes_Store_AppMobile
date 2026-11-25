package com.example.shoes_store.module.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.MainActivity;
import com.example.shoes_store.R;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.order.OrderCheckoutActivity;
import com.example.shoes_store.module.product.ProductDetailActivity;
import com.example.shoes_store.module.voucher.VoucherBottomSheet;
import com.example.shoes_store.module.voucher.VoucherModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.List;

/**
 * Tab 3 - Giỏ hàng & chọn sản phẩm thanh toán.
 */
public class CartFragment extends Fragment {

    private CartViewModel viewModel;
    private CartAdapter adapter;
    private TextView headerTitle;
    private View contentContainer;
    private View emptyState;
    private TextView voucherTitle;
    private TextView voucherSubtitle;
    private TextView voucherAction;
    private MaterialCheckBox selectAllCb;
    private TextView totalPriceText;
    private MaterialButton checkoutBtn;
    private List<CartItemModel> latestItems;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cart_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CartViewModel.class);

        headerTitle = view.findViewById(R.id.cart_header_title);
        contentContainer = view.findViewById(R.id.cart_content_container);
        emptyState = view.findViewById(R.id.cart_empty_state);
        voucherTitle = view.findViewById(R.id.cart_voucher_title);
        voucherSubtitle = view.findViewById(R.id.cart_voucher_subtitle);
        voucherAction = view.findViewById(R.id.cart_voucher_action);
        selectAllCb = view.findViewById(R.id.cart_select_all_cb);
        totalPriceText = view.findViewById(R.id.cart_total_price);
        checkoutBtn = view.findViewById(R.id.cart_checkout_btn);

        RecyclerView recyclerView = view.findViewById(R.id.cart_recycler);
        adapter = new CartAdapter(new CartAdapter.OnCartItemActionListener() {
            @Override
            public void onItemClick(CartItemModel item) {
                Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, item.getProduct().getId());
                startActivity(intent);
            }

            @Override
            public void onSelectionChanged(CartItemModel item, boolean selected) {
                viewModel.toggleItemSelected(item.getId(), selected, toastErrorCallback());
            }

            @Override
            public void onQuantityDelta(CartItemModel item, int delta) {
                viewModel.updateQuantity(item, delta, toastErrorCallback());
            }

            @Override
            public void onRemoveClick(CartItemModel item) {
                viewModel.removeItem(item.getId(), new ResourceCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Đã xóa khỏi giỏ hàng", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.cart_voucher_row).setOnClickListener(v -> openVoucherSheet());

        view.findViewById(R.id.cart_pick_shoes_btn).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
            }
        });

        checkoutBtn.setOnClickListener(v -> {
            if (getSelectedQuantity() == 0) {
                Toast.makeText(requireContext(), "Vui lòng chọn ít nhất 1 đôi giày", Toast.LENGTH_SHORT).show();
                return;
            }
            long subtotal = getSelectedSubtotal();
            long discount = viewModel.calculateEffectiveDiscount(subtotal);
            VoucherModel activeVoucher = viewModel.getSelectedVoucher();
            Intent intent = new Intent(requireContext(), OrderCheckoutActivity.class);
            intent.putExtra(OrderCheckoutActivity.EXTRA_DISCOUNT_AMOUNT, discount);
            if (activeVoucher != null && discount > 0) {
                intent.putExtra(OrderCheckoutActivity.EXTRA_VOUCHER_CODE, activeVoucher.getCode());
            }
            startActivity(intent);
        });

        viewModel.getCartItemsLiveData().observe(getViewLifecycleOwner(), items -> {
            latestItems = items;
            adapter.submitList(items);
            refreshSummary();
        });
        viewModel.getVouchersLiveData().observe(getViewLifecycleOwner(), vouchers -> {
            viewModel.syncSelectedVoucherWithLatest(vouchers);
            refreshSummary();
        });
    }

    private void openVoucherSheet() {
        long subtotal = getSelectedSubtotal();
        if (subtotal <= 0) {
            Toast.makeText(requireContext(), "Vui lòng chọn sản phẩm trước khi chọn voucher", Toast.LENGTH_SHORT).show();
            return;
        }
        VoucherBottomSheet sheet = VoucherBottomSheet.newInstance(
                viewModel.getAvailableVouchers(),
                subtotal,
                viewModel.getSelectedVoucher(),
                selected -> {
                    viewModel.setSelectedVoucher(selected);
                    refreshSummary();
                }
        );
        sheet.show(getChildFragmentManager(), "cart_voucher_sheet");
    }

    private void refreshSummary() {
        CartService.CartSummary summary = CartService.summarize(latestItems);

        headerTitle.setText("Giỏ hàng (" + summary.getTotalQuantity() + ")");
        boolean isEmpty = summary.isEmpty();
        contentContainer.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        long discount = viewModel.calculateEffectiveDiscount(summary.getSelectedSubtotal());
        VoucherModel activeVoucher = viewModel.getSelectedVoucher();
        if (activeVoucher != null && discount > 0) {
            voucherTitle.setText(getString(R.string.voucher_applied_title, activeVoucher.getCode()));
            voucherSubtitle.setText(getString(R.string.voucher_applied_subtitle, CurrencyFormatUtil.formatVnd(discount)));
            voucherAction.setText(R.string.voucher_action_change);
        } else {
            voucherTitle.setText(R.string.voucher_default_title);
            voucherSubtitle.setText(R.string.voucher_default_subtitle);
            voucherAction.setText(R.string.voucher_action_pick);
        }

        long finalTotal = CartService.calculateFinalTotal(summary.getSelectedSubtotal(), discount);
        totalPriceText.setText(CurrencyFormatUtil.formatVnd(finalTotal));
        checkoutBtn.setText("Mua hàng (" + summary.getSelectedQuantity() + ")");

        selectAllCb.setOnCheckedChangeListener(null);
        selectAllCb.setChecked(summary.isAllSelected());
        selectAllCb.setOnCheckedChangeListener((buttonView, isChecked) ->
                viewModel.selectAll(latestItems, isChecked, toastErrorCallback()));
    }

    @NonNull
    private ResourceCallback<Void> toastErrorCallback() {
        return new ResourceCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                // LiveData tự cập nhật từ Firestore
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private long getSelectedSubtotal() {
        if (latestItems == null) {
            return 0L;
        }
        long subtotal = 0L;
        for (CartItemModel item : latestItems) {
            if (item.isSelected()) {
                subtotal += item.getLineTotal();
            }
        }
        return subtotal;
    }

    private int getSelectedQuantity() {
        if (latestItems == null) {
            return 0;
        }
        int count = 0;
        for (CartItemModel item : latestItems) {
            if (item.isSelected()) {
                count += item.getQuantity();
            }
        }
        return count;
    }
}
