package com.szn.merger.Utils.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.szn.merger.R;
import com.szn.merger.Utils.Processing.ProcessingManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FormatNameReorderAdapter extends RecyclerView.Adapter<FormatNameReorderAdapter.ViewHolder> {
    public static final List<String> DEFAULT_FORMAT_ORDER = Arrays.asList(
            "Package Name",
            "Version Name",
            "Version Code",
            "ABI",
            "DPI",
            "Language",
            "Signing Status",
            "Signing Schemes",
            "Timestamp",
            "SDK Versions"
    );
    public static class Item {

        public String title;
        public int icon;

        public Item(String title, int icon) {
            this.title = title;
            this.icon = icon;
        }
    }

    private final List<Item> items = new ArrayList<>();
    private final Context context;
    private Runnable onOrderChanged;
    public FormatNameReorderAdapter(Context context) {
        this.context = context;
        loadEnabledItems(context);
    }

    public void setOnOrderChangedListener(Runnable listener) {
        this.onOrderChanged = listener;
    }
    // live update check
    public void checkEnabledItems() {

        List<Item> enabledItems = new ArrayList<>();

        if (ProcessingManager.isAppendPackageNameEnabled(context)) {
            enabledItems.add(new Item(
                    context.getString(R.string.package_name),
                    R.drawable.ic_android
            ));
        }

        if (ProcessingManager.isAppendVersionNameEnabled(context)) {
            enabledItems.add(new Item(
                    context.getString(R.string.version_name),
                    R.drawable.ic_label
            ));
        }

        if (ProcessingManager.isAppendVersionCodeEnabled(context)) {
            enabledItems.add(new Item(
                    context.getString(R.string.version_code),
                    R.drawable.ic_number
            ));
        }

        if (ProcessingManager.isAppendABIEnabled(context)) {
            enabledItems.add(new Item(
                    context.getString(R.string.abi),
                    R.drawable.ic_memory
            ));
        }

        if (ProcessingManager.isAppendDPIEnabled(context)) {
            enabledItems.add(new Item(
                    context.getString(R.string.dpi),
                    R.drawable.ic_display
            ));
        }

        if (ProcessingManager.isAppendLanguageEnabled(context)) {
            enabledItems.add(new Item(
                    context.getString(R.string.language),
                    R.drawable.ic_language
            ));
        }

        if (ProcessingManager.isAppendSigningStatusEnabled(context)) {
            enabledItems.add(new Item(
                    context.getString(R.string.signing_status),
                    R.drawable.ic_verified
            ));
        }

        if (ProcessingManager.isAppendSigningSchemesEnabled(context)) {
            enabledItems.add(new Item(
                    context.getString(R.string.signing_schemes),
                    R.drawable.ic_security
            ));
        }

        if (ProcessingManager.isAppendTimestampEnabled(context)) {
            enabledItems.add(new Item(
                    context.getString(R.string.timestamp),
                    R.drawable.ic_schedule
            ));
        }

        if (ProcessingManager.isAppendSDKVersionsEnabled(context)) {
            enabledItems.add(new Item(
                    context.getString(R.string.sdk_versions),
                    R.drawable.ic_layers
            ));
        }

        // Remove disabled items
        items.removeIf(item -> !containsTitle(enabledItems, item.title));

        // Add newly enabled items
        for (Item item : enabledItems) {
            if (!containsTitle(items, item.title)) {
                items.add(item);
            }
        }

        notifyDataSetChanged();
    }

    private boolean containsTitle(List<Item> list, String title) {
        for (Item item : list) {
            if (item.title.equals(title)) {
                return true;
            }
        }

        return false;
    }

    // init list
    private void loadEnabledItems(Context context) {

        List<Item> allItems = new ArrayList<>();

        if (ProcessingManager.isAppendPackageNameEnabled(context)) {
            allItems.add(new Item(
                    context.getString(R.string.package_name),
                    R.drawable.ic_android
            ));
        }

        if (ProcessingManager.isAppendVersionNameEnabled(context)) {
            allItems.add(new Item(
                    context.getString(R.string.version_name),
                    R.drawable.ic_label
            ));
        }

        if (ProcessingManager.isAppendVersionCodeEnabled(context)) {
            allItems.add(new Item(
                    context.getString(R.string.version_code),
                    R.drawable.ic_number
            ));
        }

        if (ProcessingManager.isAppendABIEnabled(context)) {
            allItems.add(new Item(
                    context.getString(R.string.abi),
                    R.drawable.ic_memory
            ));
        }

        if (ProcessingManager.isAppendDPIEnabled(context)) {
            allItems.add(new Item(
                    context.getString(R.string.dpi),
                    R.drawable.ic_display
            ));
        }

        if (ProcessingManager.isAppendLanguageEnabled(context)) {
            allItems.add(new Item(
                    context.getString(R.string.language),
                    R.drawable.ic_language
            ));
        }

        if (ProcessingManager.isAppendSigningStatusEnabled(context)) {
            allItems.add(new Item(
                    context.getString(R.string.signing_status),
                    R.drawable.ic_verified
            ));
        }

        if (ProcessingManager.isAppendSigningSchemesEnabled(context)) {
            allItems.add(new Item(
                    context.getString(R.string.signing_schemes),
                    R.drawable.ic_security
            ));
        }

        if (ProcessingManager.isAppendTimestampEnabled(context)) {
            allItems.add(new Item(
                    context.getString(R.string.timestamp),
                    R.drawable.ic_schedule
            ));
        }

        if (ProcessingManager.isAppendSDKVersionsEnabled(context)) {
            allItems.add(new Item(
                    context.getString(R.string.sdk_versions),
                    R.drawable.ic_layers
            ));
        }

        List<String> order = ProcessingManager.getFormatOrder(context);

        // Build items based on the saved order
        for (String title : order) {
            for (Item item : allItems) {
                if (item.title.equals(title)) {
                    items.add(item);
                    break;
                }
            }
        }

        // Add items that are not present in the saved order
        for (Item item : allItems) {
            if (!containsTitle(items, item.title)) {
                items.add(item);
            }
        }
    }

    public void resetOrder() {
        List<Item> defaultItems = new ArrayList<>();

        for (String title : DEFAULT_FORMAT_ORDER) {
            for (Item item : items) {
                if (item.title.equals(title)) {
                    defaultItems.add(item);
                    break;
                }
            }
        }

        items.clear();
        items.addAll(defaultItems);

        saveOrder(context);
        notifyDataSetChanged();

        if (onOrderChanged != null) {
            onOrderChanged.run();
        }
    }
    private void saveOrder(Context context) {
        List<String> order = new ArrayList<>();

        for (Item item : items) {
            order.add(item.title);
        }

        ProcessingManager.setFormatOrder(context, order);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_format_name, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        Item item = items.get(position);

        holder.title.setText(item.title);
        holder.icon.setImageResource(item.icon);

        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                RecyclerView recyclerView =
                        (RecyclerView) holder.itemView.getParent();

                if (recyclerView != null) {
                    ItemTouchHelper helper =
                            (ItemTouchHelper) recyclerView.getTag();

                    if (helper != null) {
                        helper.startDrag(holder);
                    }
                }
            }

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public List<Item> getItems() {
        return items;
    }

    public void attachDragSupport(RecyclerView recyclerView) {

        ItemTouchHelper itemTouchHelper =
                new ItemTouchHelper(
                        new ItemTouchHelper.SimpleCallback(
                                ItemTouchHelper.UP |
                                        ItemTouchHelper.DOWN,
                                0) {

                            @Override
                            public boolean onMove(
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    @NonNull RecyclerView.ViewHolder target) {

                                int from = viewHolder.getAdapterPosition();

                                int to = target.getAdapterPosition();

                                if (from == RecyclerView.NO_POSITION ||
                                        to == RecyclerView.NO_POSITION) {
                                    return false;
                                }

                                Item item = items.remove(from);
                                items.add(to, item);

                                notifyItemMoved(from, to);

                                saveOrder(recyclerView.getContext());
                                if (onOrderChanged != null) {
                                    onOrderChanged.run();
                                }
                                return true;
                            }

                            @Override
                            public void onSwiped(
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    int direction) {

                            }
                        });

        recyclerView.setTag(itemTouchHelper);

        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView title;
        ImageButton dragHandle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.icon);
            title = itemView.findViewById(R.id.title);
            dragHandle = itemView.findViewById(R.id.dragHandle);
        }
    }
}