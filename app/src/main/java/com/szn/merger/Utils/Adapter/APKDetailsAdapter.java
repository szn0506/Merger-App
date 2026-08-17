package com.szn.merger.Utils.Adapter;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.text.LineBreaker;
import android.text.Layout;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.szn.merger.R;
import com.szn.merger.Utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class APKDetailsAdapter
        extends RecyclerView.Adapter<APKDetailsAdapter.ViewHolder> {

    public interface OnRowNavigationClickListener {
        void onNavigationClick(String title);
    }

    private final List<String> titles;
    private final List<Integer> icons;
    private final List<Object[][]> rows;
    private final OnRowNavigationClickListener listener;

    public APKDetailsAdapter(
            OnRowNavigationClickListener listener
    ) {
        titles = new ArrayList<>();
        icons = new ArrayList<>();
        rows = new ArrayList<>();
        this.listener = listener;
    }

    public void addCard(
            String title,
            int icon,
            Object[][] cardRows
    ) {
        titles.add(title);
        icons.add(icon);
        rows.add(cardRows);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_details_card, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        holder.cardTitle.setText(titles.get(position));
        holder.cardIcon.setImageResource(icons.get(position));

        if (holder.rowsRecycler.getLayoutManager() == null) {
            holder.rowsRecycler.setLayoutManager(
                    new LinearLayoutManager(
                            holder.itemView.getContext()
                    )
            );
        }

        holder.rowsRecycler.setAdapter(
                new RowsAdapter(
                        rows.get(position),
                        listener
                )
        );
    }

    @Override
    public int getItemCount() {
        return titles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView cardIcon;
        TextView cardTitle;
        RecyclerView rowsRecycler;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            cardIcon = itemView.findViewById(R.id.cardIcon);
            cardTitle = itemView.findViewById(R.id.cardTitle);
            rowsRecycler = itemView.findViewById(R.id.rowsRecycler);
        }
    }

    private static class RowsAdapter
            extends RecyclerView.Adapter<RowsAdapter.ViewHolder> {

        private final Object[][] rows;
        private final OnRowNavigationClickListener listener;

        RowsAdapter(
                Object[][] rows,
                OnRowNavigationClickListener listener
        ) {
            this.rows = rows;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType
        ) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(
                            R.layout.item_details_row,
                            parent,
                            false
                    );

            return new ViewHolder(view);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(
                @NonNull ViewHolder holder,
                int position
        ) {
            holder.rowTitle.setText(
                    (String) rows[position][0]
            );

            holder.rowValue.setText(
                    (String) rows[position][1]
            );

            boolean navigation = (boolean) rows[position][2];

            holder.rowNav.setVisibility(
                    navigation ? View.VISIBLE : View.GONE
            );
            holder.rowNav.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();

                if (pos != RecyclerView.NO_POSITION) {
                    listener.onNavigationClick(
                            (String) rows[pos][0]
                    );
                }
            });

            holder.itemView.setOnClickListener(v -> holder.rowNav.performClick());
            holder.rowTitle.setOnClickListener(v -> holder.rowNav.performClick());
            holder.rowValue.setMaxLines(1);
            holder.rowValue.setEllipsize(null);
            holder.rowValue.setForeground(null);

            Runnable updateFade = () -> {
                TextView valueView = holder.rowValue;
                Layout layout = valueView.getLayout();

                String text = valueView.getText().toString();

                if (layout == null
                        || layout.getLineCount() == 0
                        || valueView.getPaint().measureText(text)
                        <= valueView.getWidth()) {

                    valueView.setForeground(null);
                    return;
                }

                int fadeWidth = (int) (
                        32 * valueView.getResources()
                                .getDisplayMetrics().density
                );

                GradientDrawable fade =
                        (GradientDrawable)
                                AppCompatResources.getDrawable(
                                        valueView.getContext(),
                                        R.drawable.fade_edge
                                ).mutate();

                fade.setSize(fadeWidth, 1);

                valueView.setForegroundGravity(
                        Gravity.END | Gravity.FILL_VERTICAL
                );

                valueView.setForeground(fade);
            };

            holder.rowValue.post(updateFade);

            holder.rowValue.setOnTouchListener(
                    new View.OnTouchListener() {

                        final GestureDetector detector =
                                new GestureDetector(
                                        holder.itemView.getContext(),
                                        new GestureDetector
                                                .SimpleOnGestureListener() {

                                            @Override
                                            public boolean onDown(
                                                    MotionEvent e
                                            ) {
                                                return true;
                                            }

                                            @Override
                                            public void onLongPress(
                                                    MotionEvent e
                                            ) {
                                                String value =
                                                        holder.rowValue
                                                                .getText()
                                                                .toString();

                                                ClipboardManager clipboard =
                                                        (ClipboardManager)
                                                                holder.itemView
                                                                        .getContext()
                                                                        .getSystemService(
                                                                                Context.CLIPBOARD_SERVICE
                                                                        );

                                                if (clipboard != null) {
                                                    clipboard.setPrimaryClip(
                                                            ClipData.newPlainText(
                                                                    "Value",
                                                                    value
                                                            )
                                                    );
                                                }
                                            }

                                            @Override
                                            public boolean onDoubleTap(
                                                    MotionEvent e
                                            ) {
                                                if (holder.rowValue
                                                        .getMaxLines() == 1) {

                                                    Utils.applyLayoutTransition(
                                                            holder.rowValue
                                                    );

                                                    holder.rowValue
                                                            .setMaxLines(
                                                                    Integer.MAX_VALUE
                                                            );

                                                    holder.rowValue
                                                            .setEllipsize(
                                                                    null
                                                            );

                                                    holder.rowValue
                                                            .setBreakStrategy(
                                                                    LineBreaker
                                                                            .BREAK_STRATEGY_HIGH_QUALITY
                                                            );

                                                    holder.rowValue
                                                            .setHyphenationFrequency(
                                                                    Layout
                                                                            .HYPHENATION_FREQUENCY_NONE
                                                            );

                                                    holder.rowValue
                                                            .setForeground(
                                                                    null
                                                            );

                                                } else {

                                                    Utils.applyLayoutTransition(
                                                            holder.rowValue
                                                    );

                                                    holder.rowValue
                                                            .setMaxLines(1);

                                                    holder.rowValue
                                                            .setEllipsize(
                                                                    null
                                                            );

                                                    holder.rowValue.post(
                                                            updateFade
                                                    );
                                                }

                                                return true;
                                            }
                                        }
                                );

                        @Override
                        public boolean onTouch(
                                View v,
                                MotionEvent event
                        ) {
                            detector.onTouchEvent(event);
                            holder.rowValue.setSelected(true);
                            return true;
                        }
                    }
            );
        }

        @Override
        public int getItemCount() {
            return rows.length;
        }

        static class ViewHolder
                extends RecyclerView.ViewHolder {

            TextView rowTitle;
            TextView rowValue;
            ImageButton rowNav;
            LinearLayout rootView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);

                rowTitle = itemView.findViewById(
                        R.id.rowTitle
                );

                rowValue = itemView.findViewById(
                        R.id.rowValue
                );

                rowNav = itemView.findViewById(
                        R.id.rowNav
                );
            }
        }
    }

    public static class FullDetailsAdapter extends RecyclerView.Adapter<FullDetailsAdapter.ViewHolder> {

        private final String[] items;

        public FullDetailsAdapter(String[] items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.apk_full_details_text, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.text.setText(items[position]);

            holder.copy.setOnClickListener(v -> {
                ClipboardManager clipboard =
                        (ClipboardManager) v.getContext()
                                .getSystemService(Context.CLIPBOARD_SERVICE);

                ClipData clip = ClipData.newPlainText(
                        "Permission",
                        items[position]
                );

                clipboard.setPrimaryClip(clip);
            });
        }

        @Override
        public int getItemCount() {
            return items.length;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            TextView text;
            ImageButton copy;

            ViewHolder(@NonNull View itemView) {
                super(itemView);

                text = itemView.findViewById(R.id.text);
                copy = itemView.findViewById(R.id.copy);
            }
        }
    }
}