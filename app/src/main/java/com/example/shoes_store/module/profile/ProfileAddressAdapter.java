package com.example.shoes_store.module.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoes_store.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách địa chỉ giao hàng.
 */
public class ProfileAddressAdapter extends RecyclerView.Adapter<ProfileAddressAdapter.AddressViewHolder> {

    public interface OnAddressActionListener {
        void onSelectAddress(ProfileAddressModel address);

        void onSetDefault(ProfileAddressModel address);

        void onEditAddress(ProfileAddressModel address);

        void onDeleteAddress(ProfileAddressModel address);
    }

    private final List<ProfileAddressModel> addresses = new ArrayList<>();
    private final OnAddressActionListener listener;

    public ProfileAddressAdapter(OnAddressActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ProfileAddressModel> list) {
        addresses.clear();
        if (list != null) {
            addresses.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.profile_item_address, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        ProfileAddressModel item = addresses.get(position);
        holder.receiverPhoneText.setText(item.getReceiverName() + " · " + item.getPhone());
        holder.tagText.setText(item.getLabel());
        holder.defaultBadge.setVisibility(item.isDefaultAddress() ? View.VISIBLE : View.GONE);
        holder.detailText.setText(item.getAddressDetail());

        holder.setDefaultBtn.setVisibility(item.isDefaultAddress() ? View.INVISIBLE : View.VISIBLE);
        holder.deleteBtn.setVisibility(addresses.size() > 1 ? View.VISIBLE : View.GONE);

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

    @Override
    public int getItemCount() {
        return addresses.size();
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
