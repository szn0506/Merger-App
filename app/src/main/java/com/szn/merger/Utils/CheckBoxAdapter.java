package com.szn.merger.Utils;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.szn.merger.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CheckBoxAdapter extends RecyclerView.Adapter<CheckBoxAdapter.ViewHolder> {

    public interface OnItemSelectedListener {
        void onItemSelected(int position, String value);
    }

    private final List<String> items;
    private final OnItemSelectedListener listener;

    private final Set<String> checkedItems = new HashSet<>();

    public CheckBoxAdapter(List<String> items, OnItemSelectedListener listener) {
        this.items = new ArrayList<>(items);

        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        MaterialCheckBox checkBox = (MaterialCheckBox) LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_checkbox, parent, false);

        return new ViewHolder(checkBox);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        String value = items.get(position);

        holder.checkBox.setText(value);

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(checkedItems.contains(value));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                checkedItems.add(value);
            } else {
                checkedItems.remove(value);
            }

            if (listener != null) {
                listener.onItemSelected(position, value);
            }
        });
    }

    public List<String> getCheckedItems() {
        return new ArrayList<>(checkedItems);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        MaterialCheckBox checkBox;

        public ViewHolder(@NonNull MaterialCheckBox itemView) {
            super(itemView);
            checkBox = itemView;
        }
    }
}