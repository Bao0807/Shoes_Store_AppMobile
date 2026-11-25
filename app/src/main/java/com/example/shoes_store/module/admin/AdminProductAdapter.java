package com.example.shoes_store.module.admin;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseListAdapter;
import com.example.shoes_store.core.CurrencyFormatUtil;
import com.example.shoes_store.core.RemoteImageHelper;
import com.example.shoes_store.module.product.ProductModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

/**
 * Adapter quản lý danh sách Sản phẩm & Tồn kho trong Kênh Quản trị (Admin).
 */
public class AdminProductAdapter extends BaseListAdapter<ProductModel, AdminProductAdapter.ProductViewHolder> {

    public interface OnAdminProductActionListener {
        void onEditProduct(@NonNull ProductModel product);

        void onToggleActive(@NonNull ProductModel product, boolean isActive);
    }

    private final OnAdminProductActionListener listener;

    public AdminProductAdapter(@NonNull OnAdminProductActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ProductViewHolder(inflate(parent, R.layout.admin_item_product));
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductModel product = getItem(position);
        Context context = holder.itemView.getContext();

        RemoteImageHelper.bindImage(holder.imageView, product.getThumbnailUrl(), product.getImageResId());
        holder.metaText.setText(product.getBrand() + " · Dòng " + product.getCategory());
        holder.nameText.setText(product.getName());

        String priceStr = CurrencyFormatUtil.formatVnd(product.getPrice());
        if (product.hasDiscount()) {
            priceStr += " (Gốc: " + CurrencyFormatUtil.formatVnd(product.getOriginalPrice()) + ")";
        }
        holder.priceText.setText(priceStr);

        List<String> sizes = product.getAllSizes();
        String sizesJoined = sizes.isEmpty() ? "39 - 43" : TextUtils.join(", ", sizes);
        int outCount = product.getOutOfStockSizes().size();
        String stockState = outCount > 0 && outCount >= sizes.size() && !sizes.isEmpty()
                ? " · Đang hết kho"
                : " · Còn hàng";
        holder.sizesText.setText("Size EU: " + sizesJoined + stockState);

        boolean active = product.isActive();
        holder.statusText.setText(active ? "Trạng thái: Đang kinh doanh" : "Trạng thái: Đang tạm ẩn");
        holder.statusText.setTextColor(ContextCompat.getColor(
                context,
                active ? R.color.status_success : R.color.text_tertiary
        ));

        holder.activeSwitch.setOnCheckedChangeListener(null);
        holder.activeSwitch.setChecked(active);
        holder.activeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                listener.onToggleActive(product, isChecked));

        holder.editBtn.setOnClickListener(v -> listener.onEditProduct(product));
        holder.itemView.setOnClickListener(v -> listener.onEditProduct(product));
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final TextView metaText;
        final TextView nameText;
        final TextView priceText;
        final TextView sizesText;
        final TextView statusText;
        final MaterialSwitch activeSwitch;
        final MaterialButton editBtn;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.admin_product_image);
            metaText = itemView.findViewById(R.id.admin_product_meta);
            nameText = itemView.findViewById(R.id.admin_product_name);
            priceText = itemView.findViewById(R.id.admin_product_price);
            sizesText = itemView.findViewById(R.id.admin_product_sizes);
            statusText = itemView.findViewById(R.id.admin_product_status_text);
            activeSwitch = itemView.findViewById(R.id.admin_product_active_switch);
            editBtn = itemView.findViewById(R.id.admin_product_edit_btn);
        }
    }
}
