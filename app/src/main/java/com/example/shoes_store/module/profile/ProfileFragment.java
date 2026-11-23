package com.example.shoes_store.module.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.shoes_store.MainActivity;
import com.example.shoes_store.R;
import com.example.shoes_store.module.order.OrderHistoryActivity;
import com.example.shoes_store.module.order.OrderModel;
import com.google.android.material.imageview.ShapeableImageView;

/**
 * Tab 5 - Tài khoản & Hồ sơ chạy bộ (chưa có Đăng nhập/Đăng xuất).
 */
public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.profile_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ProfileViewModel viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        ShapeableImageView avatarView = view.findViewById(R.id.profile_avatar);
        TextView nameView = view.findViewById(R.id.profile_name);
        TextView emailPhoneView = view.findViewById(R.id.profile_email_phone);
        TextView runnerSummaryView = view.findViewById(R.id.profile_runner_summary);
        TextView pendingBadge = view.findViewById(R.id.badge_order_pending);
        TextView shippingBadge = view.findViewById(R.id.badge_order_shipping);

        View.OnClickListener openEditProfile = v ->
                startActivity(new Intent(requireContext(), ProfileEditActivity.class));
        view.findViewById(R.id.profile_edit_btn).setOnClickListener(openEditProfile);
        view.findViewById(R.id.profile_runner_card).setOnClickListener(openEditProfile);

        view.findViewById(R.id.profile_view_all_orders).setOnClickListener(v -> openOrderHistory(0));
        view.findViewById(R.id.profile_order_pending).setOnClickListener(v -> openOrderHistory(0));
        view.findViewById(R.id.profile_order_shipping).setOnClickListener(v -> openOrderHistory(1));
        view.findViewById(R.id.profile_order_completed).setOnClickListener(v -> openOrderHistory(2));
        view.findViewById(R.id.profile_order_cancelled).setOnClickListener(v -> openOrderHistory(3));

        view.findViewById(R.id.menu_addresses).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileAddressActivity.class)));

        view.findViewById(R.id.menu_wishlist).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_wishlist);
            }
        });

        view.findViewById(R.id.menu_vouchers).setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Kho Voucher của bạn")
                        .setMessage("• RUN50K: Giảm 150.000₫ cho đơn từ giỏ hàng\n• FREESHIP24: Miễn phí ship với đơn hàng trên 5.000.000₫")
                        .setPositiveButton("Đóng", null)
                        .show());

        view.findViewById(R.id.menu_support).setOnClickListener(v ->
                Toast.makeText(
                        requireContext(),
                        "Hotline tư vấn chọn giày chạy: 1900 6868 (8:00 - 21:00)",
                        Toast.LENGTH_LONG
                ).show());

        viewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            if (profile == null) {
                return;
            }
            avatarView.setImageResource(profile.getAvatarResId());
            nameView.setText(profile.getFullName());
            emailPhoneView.setText(profile.getEmail() + " · " + profile.getPhone());
            runnerSummaryView.setText("Size EU " + profile.getPreferredSize() + " · " + profile.getRunningGoal());
        });

        viewModel.getOrdersLiveData().observe(getViewLifecycleOwner(), orders -> {
            int pendingCount = 0;
            int shippingCount = 0;
            if (orders != null) {
                for (OrderModel order : orders) {
                    if (order.getStatus() == OrderModel.Status.PENDING) {
                        pendingCount++;
                    } else if (order.getStatus() == OrderModel.Status.SHIPPING) {
                        shippingCount++;
                    }
                }
            }
            updateBadge(pendingBadge, pendingCount);
            updateBadge(shippingBadge, shippingCount);
        });
    }

    private void updateBadge(TextView badge, int count) {
        if (count > 0) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(String.valueOf(count));
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private void openOrderHistory(int tabIndex) {
        Intent intent = new Intent(requireContext(), OrderHistoryActivity.class);
        intent.putExtra(OrderHistoryActivity.EXTRA_INITIAL_TAB, tabIndex);
        startActivity(intent);
    }
}
