package com.example.shoes_store.module.profile;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;
import com.example.shoes_store.core.BaseListAdapter;

/**
 * Adapter hiển thị danh sách địa chỉ giao hàng.
 */
public class ProfileAddressAdapter extends BaseListAdapter<ProfileAddressModel, ProfileAddressAdapter.AddressViewHolder> {

    public interface OnAddressActionListener {
        void onSelectAddress(ProfileAddressModel address);

        void onSetDefault(ProfileAddressModel address);

        void onEditAddress(ProfileAddressModel address);

        void onDeleteAddress(ProfileAddressModel address);
    }

    private final OnAddressActionListener listener;

    public ProfileAddressAdapter(OnAddressActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AddressViewHolder(inflate(parent, R.layout.profile_item_address));
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        ProfileAddressModel item = getItem(position);
        holder.receiverPhoneText.setText(item.getReceiverName() + " · " + item.getPhone());
        holder.tagText.setText(item.getLabel());
        holder.defaultBadge.setVisibility(item.isDefaultAddress() ? View.VISIBLE : View.GONE);
        holder.detailText.setText(item.getAddressDetail());

        holder.setDefaultBtn.setVisibility(item.isDefaultAddress() ? View.INVISIBLE : View.VISIBLE);
        holder.deleteBtn.setVisibility(getItemCount() > 1 ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSelectAddress(item);
            }
        });
        holder.setDefaultBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetDefault(item);
            }
        });
        holder.editBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditAddress(item);
            }
        });
        holder.deleteBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteAddress(item);
            }
        });
    }

    static class AddressViewHolder extends RecyclerView.ViewHolder {
        final TextView receiverPhoneText;
        final TextView tagText;
        final TextView defaultBadge;
        final TextView detailText;
        final View setDefaultBtn;
        final View editBtn;
        final View deleteBtn;

        AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            receiverPhoneText = itemView.findViewById(R.id.addr_receiver_phone);
            tagText = itemView.findViewById(R.id.addr_tag);
            defaultBadge = itemView.findViewById(R.id.addr_default_badge);
            detailText = itemView.findViewById(R.id.addr_detail);
            setDefaultBtn = itemView.findViewById(R.id.addr_set_default_btn);
            editBtn = itemView.findViewById(R.id.addr_edit_btn);
            deleteBtn = itemView.findViewById(R.id.addr_delete_btn);
        }
    }
}
