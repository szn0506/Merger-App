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
        void onItemSelected(int position, String value, int selectedCount);
    }

    private final List<String> items;
    private final OnItemSelectedListener listener;
    private final List<String> originalItems;
    private final Set<String> checkedItems = new HashSet<>();
    private final Set<String> disabledItems = new HashSet<>();
    private long lastClickTime = 0;
    private int lastClickPosition = -1;

    public CheckBoxAdapter(List<String> items, OnItemSelectedListener listener) {
        this.items = new ArrayList<>(items);
        this.originalItems = new ArrayList<>(items);
        this.listener = listener;
    }

    public void filter(String query) {
        items.clear();

        if (query == null || query.trim().isEmpty()) {
            items.addAll(originalItems);
        } else {
            String search = query.toLowerCase().trim();

            for (String item : originalItems) {
                if (item.toLowerCase().contains(search)) {
                    items.add(item);
                }
            }
        }

        notifyDataSetChanged();
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
        holder.checkBox.setEnabled(!disabledItems.contains(value));
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(checkedItems.contains(value));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                checkedItems.add(value);
            } else {
                checkedItems.remove(value);
            }

            if (listener != null) {
                listener.onItemSelected(position, value, checkedItems.size());
            }
        });
        holder.itemView.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();

            if (lastClickPosition == position && currentTime - lastClickTime < 300) {
                toggleSelectAll();
                lastClickTime = 0;
                lastClickPosition = -1;
                return;
            }

            lastClickTime = currentTime;
            lastClickPosition = position;
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

    public void selectAll() {
        checkedItems.clear();
        checkedItems.addAll(items);
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        return checkedItems.size();
    }

    public void setCheckedItems(List<String> values) {
        checkedItems.clear();

        for (String item : items) {
            String lowerItem = item.toLowerCase();

            for (String value : values) {
                if (lowerItem.contains(value.toLowerCase())) {
                    checkedItems.add(item);
                    break;
                }
            }
        }

        notifyDataSetChanged();
    }

    public void setDisabled(List<String> values) {
        disabledItems.clear();

        for (String item : items) {
            String lowerItem = item.toLowerCase();

            for (String value : values) {
                if (lowerItem.contains(value.toLowerCase())) {
                    disabledItems.add(item);
                    break;
                }
            }
        }

        notifyDataSetChanged();
    }

    public void toggleSelectAll() {
        if (checkedItems.size() == items.size()) {
            checkedItems.clear();
            checkedItems.addAll(disabledItems);
        } else {
            checkedItems.clear();
            checkedItems.addAll(items);
        }

        notifyDataSetChanged();

        if (listener != null) {
            listener.onItemSelected(-1, "", checkedItems.size());
        }
    }
}