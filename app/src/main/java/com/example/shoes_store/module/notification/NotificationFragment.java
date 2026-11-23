package com.example.shoes_store.module.notification;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.module.order.OrderDetailActivity;
import com.example.shoes_store.module.product.ProductDetailActivity;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

/**
 * Tab 4 - Thông báo đơn hàng & ưu đãi.
 */
public class NotificationFragment extends Fragment {

    private NotificationViewModel viewModel;
    private NotificationAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyText;
    private List<NotificationModel> latestSource;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.notification_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        recyclerView = view.findViewById(R.id.notif_recycler);
        emptyText = view.findViewById(R.id.notif_empty_text);
        ChipGroup filterChips = view.findViewById(R.id.notif_filter_chips);

        adapter = new NotificationAdapter(item -> {
            viewModel.markRead(item.getId());
            if (item.getType() == NotificationModel.Type.ORDER && !item.getTargetId().isEmpty()) {
                Intent intent = new Intent(requireContext(), OrderDetailActivity.class);
                intent.putExtra(OrderDetailActivity.EXTRA_ORDER_CODE, item.getTargetId());
                startActivity(intent);
            } else if (item.getType() == NotificationModel.Type.DROP && !item.getTargetId().isEmpty()) {
                Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, item.getTargetId());
                startActivity(intent);
            } else if (item.getType() == NotificationModel.Type.PROMO && !item.getTargetId().isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("voucher_code", item.getTargetId()));
                }
                Toast.makeText(
                        requireContext(),
                        "Đã sao chép mã giảm giá " + item.getTargetId(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.notif_mark_all_btn).setOnClickListener(v -> {
            viewModel.markAllRead();
            Toast.makeText(requireContext(), "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show();
        });

        filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.notif_chip_order) {
                viewModel.setFilter(NotificationViewModel.Filter.ORDER);
            } else if (id == R.id.notif_chip_promo) {
                viewModel.setFilter(NotificationViewModel.Filter.PROMO);
            } else {
                viewModel.setFilter(NotificationViewModel.Filter.ALL);
            }
            renderList();
        });

        viewModel.getNotificationsLiveData().observe(getViewLifecycleOwner(), list -> {
            latestSource = list;
            renderList();
        });
    }

    private void renderList() {
        List<NotificationModel> filtered = viewModel.filterList(latestSource);
        adapter.submitList(filtered);
        boolean empty = filtered.isEmpty();
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
