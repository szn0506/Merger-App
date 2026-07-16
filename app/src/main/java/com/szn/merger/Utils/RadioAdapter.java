package com.szn.merger.Utils;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.szn.merger.R;

import java.util.ArrayList;
import java.util.List;

public class RadioAdapter extends RecyclerView.Adapter<RadioAdapter.ViewHolder> {

    public interface OnItemSelectedListener {
        void onItemSelected(int position, String value);
    }

    private final List<String> items;
    private final List<String> originalItems;
    private final OnItemSelectedListener listener;

    private int selectedPosition = -1;

    public RadioAdapter(List<String> items, OnItemSelectedListener listener) {
        this.items = new ArrayList<>(items);
        this.originalItems = new ArrayList<>(items);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        MaterialRadioButton radio = (MaterialRadioButton) LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_radio, parent, false);

        return new ViewHolder(radio);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        holder.radio.setText(items.get(position));
        holder.radio.setChecked(position == selectedPosition);

        holder.radio.setOnClickListener(v -> {

            int adapterPosition = holder.getAdapterPosition();

            if (adapterPosition == RecyclerView.NO_POSITION)
                return;

            int oldPosition = selectedPosition;
            selectedPosition = adapterPosition;

            if (oldPosition != -1)
                notifyItemChanged(oldPosition);

            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onItemSelected(
                        selectedPosition,
                        items.get(selectedPosition)
                );
            }
        });
    }
    public void filter(String keyword) {

        items.clear();

        if (keyword.isEmpty()) {
            items.addAll(originalItems);
        } else {
            keyword = keyword.toLowerCase();

            for (String item : originalItems) {
                if (item.toLowerCase().contains(keyword)) {
                    items.add(item);
                }
            }
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        MaterialRadioButton radio;

        public ViewHolder(@NonNull MaterialRadioButton itemView) {
            super(itemView);
            radio = itemView;
        }
    }
}