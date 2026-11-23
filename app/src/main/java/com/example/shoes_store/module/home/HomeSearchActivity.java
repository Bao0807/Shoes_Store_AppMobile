package com.example.shoes_store.module.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.UiInsetHelper;
import com.example.shoes_store.module.product.ProductDetailActivity;
import com.example.shoes_store.module.product.ProductModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

/**
 * Màn hình Tìm kiếm & Sắp xếp / Lọc nâng cao giày chạy bộ.
 */
public class HomeSearchActivity extends AppCompatActivity {

    private static final String[] POPULAR_KEYWORDS = {"Alphafly 3", "Daily", "HOKA", "Trail", "Carbon", "ASICS"};

    private HomeViewModel viewModel;
    private HomeShoeAdapter shoeAdapter;
    private EditText searchInput;
    private View clearBtn;
    private View filterDot;
    private View suggestionsContainer;
    private View recentSection;
    private ChipGroup recentChips;
    private ChipGroup popularChips;
    private View resultsContainer;
    private TextView resultCountText;
    private RecyclerView resultRecycler;
    private View emptyStateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.home_search_activity);
        UiInsetHelper.applySystemBarInsets(findViewById(R.id.search_root));

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        searchInput = findViewById(R.id.search_input);
        clearBtn = findViewById(R.id.search_clear_btn);
        filterDot = findViewById(R.id.search_filter_dot);
        suggestionsContainer = findViewById(R.id.search_suggestions_container);
        recentSection = findViewById(R.id.search_recent_section);
        recentChips = findViewById(R.id.search_recent_chips);
        popularChips = findViewById(R.id.search_popular_chips);
        resultsContainer = findViewById(R.id.search_results_container);
        resultCountText = findViewById(R.id.search_result_count);
        resultRecycler = findViewById(R.id.search_result_recycler);
        emptyStateView = findViewById(R.id.search_empty_state);

        findViewById(R.id.search_back_btn).setOnClickListener(v -> finish());
        findViewById(R.id.search_filter_btn).setOnClickListener(v -> openFilterSheet());
        clearBtn.setOnClickListener(v -> searchInput.setText(""));

        setupPopularChips();
        setupSortChips();
        setupSearchInput();
        setupResultsRecycler();
        observeData();

        searchInput.requestFocus();
    }

    private void setupPopularChips() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String keyword : POPULAR_KEYWORDS) {
            Chip chip = (Chip) inflater.inflate(R.layout.home_chip_template, popularChips, false);
            chip.setCheckable(false);
            chip.setText(keyword);
            chip.setOnClickListener(v -> applyKeyword(keyword));
            popularChips.addView(chip);
        }
    }

    private void setupSortChips() {
        ChipGroup sortGroup = findViewById(R.id.search_sort_chips);
        sortGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chip_sort_price_asc) {
                viewModel.setSortOption(HomeFilterState.SortOption.PRICE_ASC);
            } else if (id == R.id.chip_sort_price_desc) {
                viewModel.setSortOption(HomeFilterState.SortOption.PRICE_DESC);
            } else if (id == R.id.chip_sort_rating) {
                viewModel.setSortOption(HomeFilterState.SortOption.RATING_DESC);
            } else {
                viewModel.setSortOption(HomeFilterState.SortOption.POPULAR);
            }
        });
    }

    private void setupSearchInput() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s != null ? s.toString() : "";
                clearBtn.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                viewModel.setQuery(q);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    viewModel.addRecentSearch(query);
                }
                return true;
            }
            return false;
        });
    }

    private void setupResultsRecycler() {
        shoeAdapter = new HomeShoeAdapter(new HomeShoeAdapter.OnShoeInteractionListener() {
            @Override
            public void onShoeClick(ProductModel product) {
                String currentQuery = searchInput.getText().toString().trim();
                if (!currentQuery.isEmpty()) {
                    viewModel.addRecentSearch(currentQuery);
                }
                Intent intent = new Intent(HomeSearchActivity.this, ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onFavoriteToggle(ProductModel product) {
                boolean added = viewModel.toggleFavorite(product.getId());
                Toast.makeText(
                        HomeSearchActivity.this,
                        added ? "Đã thêm vào Yêu thích" : "Đã bỏ khỏi Yêu thích",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
        resultRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        resultRecycler.setAdapter(shoeAdapter);
    }

    private void observeData() {
        viewModel.getFavoriteIdsLiveData().observe(this, ids -> shoeAdapter.setFavoriteIds(ids));

        viewModel.getRecentSearchesLiveData().observe(this, this::renderRecentChips);

        viewModel.getFilterStateLiveData().observe(this, state -> {
            if (state == null) {
                return;
            }
            filterDot.setVisibility(state.hasAnyFilter() ? View.VISIBLE : View.GONE);
            boolean showSuggestions = state.getQuery().trim().isEmpty() && !state.hasAnyFilter();
            suggestionsContainer.setVisibility(showSuggestions ? View.VISIBLE : View.GONE);
            resultsContainer.setVisibility(showSuggestions ? View.GONE : View.VISIBLE);
        });

        viewModel.getDisplayedProductsLiveData().observe(this, products -> {
            shoeAdapter.submitProducts(products);
            int count = products != null ? products.size() : 0;
            resultCountText.setText("Tìm thấy " + count + " đôi giày");
            emptyStateView.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            resultRecycler.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        });
    }

    private void renderRecentChips(List<String> recentList) {
        recentChips.removeAllViews();
        if (recentList == null || recentList.isEmpty()) {
            recentSection.setVisibility(View.GONE);
            return;
        }
        recentSection.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String term : recentList) {
            Chip chip = (Chip) inflater.inflate(R.layout.home_chip_template, recentChips, false);
            chip.setCheckable(false);
            chip.setCloseIconVisible(true);
            chip.setText(term);
            chip.setOnClickListener(v -> applyKeyword(term));
            chip.setOnCloseIconClickListener(v -> viewModel.removeRecentSearch(term));
            recentChips.addView(chip);
        }
    }

    private void applyKeyword(String keyword) {
        searchInput.setText(keyword);
        searchInput.setSelection(keyword.length());
        viewModel.addRecentSearch(keyword);
    }

    private void openFilterSheet() {
        HomeFilterBottomSheet sheet = HomeFilterBottomSheet.newInstance(
                viewModel.getCurrentFilterState(),
                updated -> viewModel.updateFilterState(updated)
        );
        sheet.show(getSupportFragmentManager(), "search_filter_sheet");
    }
}
