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
import com.example.shoes_store.module.order.OrderCheckoutActivity;
import com.example.shoes_store.module.product.ProductDetailActivity;
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
                viewModel.toggleItemSelected(item.getId(), selected);
            }

            @Override
            public void onQuantityDelta(CartItemModel item, int delta) {
                viewModel.updateQuantity(item.getId(), delta);
            }

            @Override
            public void onRemoveClick(CartItemModel item) {
                viewModel.removeItem(item.getId());
                Toast.makeText(requireContext(), "Đã xóa khỏi giỏ hàng", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.cart_voucher_row).setOnClickListener(v -> {
            viewModel.toggleVoucher();
            refreshSummary();
        });

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
            Intent intent = new Intent(requireContext(), OrderCheckoutActivity.class);
            intent.putExtra(
                    OrderCheckoutActivity.EXTRA_DISCOUNT_AMOUNT,
                    viewModel.isVoucherApplied() ? CartViewModel.VOUCHER_DISCOUNT_AMOUNT : 0L
            );
            startActivity(intent);
        });

        viewModel.getCartItemsLiveData().observe(getViewLifecycleOwner(), items -> {
            latestItems = items;
            adapter.submitList(items);
            refreshSummary();
        });
    }

    private void refreshSummary() {
        int totalQtyAll = 0;
        int selectedQty = 0;
        long selectedSubtotal = 0L;
        boolean allSelected = latestItems != null && !latestItems.isEmpty();

        if (latestItems != null) {
            for (CartItemModel item : latestItems) {
                totalQtyAll += item.getQuantity();
                if (item.isSelected()) {
                    selectedQty += item.getQuantity();
                    selectedSubtotal += item.getLineTotal();
                } else {
                    allSelected = false;
                }
            }
        }

        headerTitle.setText("Giỏ hàng (" + totalQtyAll + ")");
        boolean isEmpty = latestItems == null || latestItems.isEmpty();
        contentContainer.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        boolean voucherOn = viewModel.isVoucherApplied();
        voucherSubtitle.setText(voucherOn ? "Đã áp dụng · Giảm 150.000₫" : "Bấm để áp dụng giảm 150.000₫");
        voucherAction.setText(voucherOn ? "Bỏ chọn" : "Áp dụng");

        long discount = (voucherOn && selectedSubtotal > 0) ? CartViewModel.VOUCHER_DISCOUNT_AMOUNT : 0L;
        long finalTotal = Math.max(0L, selectedSubtotal - discount);

        totalPriceText.setText(CurrencyFormatUtil.formatVnd(finalTotal));
        checkoutBtn.setText("Mua hàng (" + selectedQty + ")");

        selectAllCb.setOnCheckedChangeListener(null);
        selectAllCb.setChecked(allSelected);
        selectAllCb.setOnCheckedChangeListener((buttonView, isChecked) -> viewModel.selectAll(isChecked));
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
