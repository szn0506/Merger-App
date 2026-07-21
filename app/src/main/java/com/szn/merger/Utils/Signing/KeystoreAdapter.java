package com.szn.merger.Utils.Signing;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.szn.merger.R;

import java.util.List;

public class KeystoreAdapter extends RecyclerView.Adapter<KeystoreAdapter.ViewHolder> {


    private final Context context;
    private List<KeystoreManager.Item> items;

    private int selectedPosition = 0;

    public KeystoreAdapter(Context context, List<KeystoreManager.Item> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_keystore, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        KeystoreManager.Item item = items.get(position);

        holder.title.setText(item.name);
        holder.fileName.setText(item.fileName);
        holder.radio.setChecked(position == selectedPosition);

        View.OnClickListener listener = v -> {
            int old = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            notifyItemChanged(old);
            notifyItemChanged(selectedPosition);
        };

        holder.itemView.setOnClickListener(listener);
        holder.radio.setOnClickListener(listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    public void reload(
            List<KeystoreManager.Item> items
    ) {

        this.items.clear();
        this.items.addAll(items);

        notifyDataSetChanged();
    }
    public KeystoreManager.Item getSelectedItem() {
        return items.get(selectedPosition);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView title;
        final TextView fileName;
        final MaterialRadioButton radio;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title);
            fileName = itemView.findViewById(R.id.fileName);
            radio = itemView.findViewById(R.id.radio);
        }
    }
}