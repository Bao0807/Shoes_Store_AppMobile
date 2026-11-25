package com.example.shoes_store.module.product;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.core.BaseFirestoreRepository;
import com.example.shoes_store.core.FirebaseProviderHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository sở hữu đồng bộ realtime Sản phẩm (`products`) và Biến thể (`variants`).
 */
public class ProductRepository extends BaseFirestoreRepository {

    private static final String TAG = "ProductRepository";
    private static final String LISTENER_PRODUCTS = "products";
    private static volatile ProductRepository instance;

    private final MutableLiveData<List<ProductModel>> productsLiveData = new MutableLiveData<>(new ArrayList<>());

    private ProductRepository() {
    }

    public static ProductRepository getInstance() {
        if (instance == null) {
            synchronized (ProductRepository.class) {
                if (instance == null) {
                    instance = new ProductRepository();
                }
            }
        }
        return instance;
    }

    @Override
    public void onAppInit() {
        startPublicListening();
    }

    // ===== 1. REALTIME =====

    public void startPublicListening() {
        replacePublicListener(LISTENER_PRODUCTS, listenCollection(
                collection(FirebaseProviderHelper.COL_PRODUCTS),
                TAG,
                doc -> {
                    ProductModel product = ProductMapper.productFromSnapshot(doc);
                    return product.isActive() ? product : null;
                },
                null,
                productsLiveData::setValue
        ));
    }

    public void refreshPublicCatalog() {
        collection(FirebaseProviderHelper.COL_PRODUCTS)
                .get()
                .addOnFailureListener(e -> Log.w(TAG, "Refresh products failed: " + e.getMessage()));
    }

    public ListenerRegistration observeVariants(
            @NonNull String productId,
            @NonNull MutableLiveData<List<ProductVariantModel>> target
    ) {
        return listenCollection(
                collection(FirebaseProviderHelper.COL_PRODUCTS)
                        .document(productId)
                        .collection(FirebaseProviderHelper.SUB_VARIANTS),
                TAG,
                doc -> ProductMapper.variantFromSnapshot(doc, productId),
                (a, b) -> {
                    int c = a.getVariantId().compareTo(b.getVariantId());
                    return c != 0 ? c : a.getSize().compareTo(b.getSize());
                },
                target::setValue
        );
    }

    // ===== 2. ĐỌC =====

    public LiveData<List<ProductModel>> getProductsLiveData() {
        return productsLiveData;
    }

    @NonNull
    public List<ProductModel> getAllProducts() {
        List<ProductModel> list = productsLiveData.getValue();
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    @Nullable
    public ProductModel getProductById(@Nullable String productId) {
        if (productId == null) {
            return null;
        }
        List<ProductModel> list = productsLiveData.getValue();
        if (list == null) {
            return null;
        }
        for (ProductModel p : list) {
            if (productId.equals(p.getId())) {
                return p;
            }
        }
        return null;
    }
}
