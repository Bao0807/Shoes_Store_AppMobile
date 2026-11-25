package com.example.shoes_store.module.wishlist;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.MainActivity;
import com.example.shoes_store.R;
import com.example.shoes_store.core.CategoryStyleHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.product.ProductDetailActivity;
import com.example.shoes_store.module.product.ProductModel;
import com.google.android.material.chip.ChipGroup;

import java.util.List;
import java.util.Set;

/**
 * Tab 2 - Danh sách giày yêu thích.
 */
public class WishlistFragment extends Fragment {

    private WishlistViewModel viewModel;
    private WishlistAdapter adapter;
    private TextView countPill;
    private RecyclerView recyclerView;
    private View emptyStateView;
    private Set<String> latestFavoriteIds;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wishlist_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WishlistViewModel.class);

        countPill = view.findViewById(R.id.wishlist_count_pill);
        recyclerView = view.findViewById(R.id.wishlist_recycler);
        emptyStateView = view.findViewById(R.id.wishlist_empty_state);
        ChipGroup chipGroup = view.findViewById(R.id.wishlist_category_chips);

        adapter = new WishlistAdapter(new WishlistAdapter.OnWishlistActionListener() {
            @Override
            public void onShoeClick(ProductModel product) {
                Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onRemoveFavorite(ProductModel product) {
                viewModel.removeFavorite(product.getId());
                Toast.makeText(requireContext(), "Đã bỏ khỏi Yêu thích", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAddToCart(ProductModel product) {
                viewModel.addQuickToCart(product, new ResourceCallback<String>() {
                    @Override
                    public void onSuccess(String size) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Đã thêm " + product.getName() + " (EU " + size + ") vào giỏ", Toast.LENGTH_SHORT).show();
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

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.wishlist_explore_btn).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
            }
        });

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.wl_chip_daily) {
                viewModel.setSelectedCategory(CategoryStyleHelper.CAT_DAILY);
            } else if (id == R.id.wl_chip_race) {
                viewModel.setSelectedCategory(CategoryStyleHelper.CAT_RACE);
            } else if (id == R.id.wl_chip_tempo) {
                viewModel.setSelectedCategory(CategoryStyleHelper.CAT_TEMPO);
            } else if (id == R.id.wl_chip_trail) {
                viewModel.setSelectedCategory(CategoryStyleHelper.CAT_TRAIL);
            } else if (id == R.id.wl_chip_recovery) {
                viewModel.setSelectedCategory(CategoryStyleHelper.CAT_RECOVERY);
            } else {
                viewModel.setSelectedCategory(CategoryStyleHelper.CAT_ALL);
            }
            renderFavorites();
        });

        viewModel.getFavoriteIdsLiveData().observe(getViewLifecycleOwner(), ids -> {
            latestFavoriteIds = ids;
            renderFavorites();
        });

        viewModel.getProductsLiveData().observe(getViewLifecycleOwner(), products -> renderFavorites());
    }

    private void renderFavorites() {
        List<ProductModel> list = viewModel.getFilteredFavorites(latestFavoriteIds);
        int totalSaved = latestFavoriteIds != null ? latestFavoriteIds.size() : 0;
        countPill.setText(totalSaved + " đôi");
        adapter.submitList(list);

        boolean empty = list.isEmpty();
        emptyStateView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
