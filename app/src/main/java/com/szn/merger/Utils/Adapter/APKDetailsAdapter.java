package com.szn.merger.Utils.Adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Layout;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.szn.merger.R;

import java.util.ArrayList;
import java.util.List;

public class APKDetailsAdapter extends RecyclerView.Adapter<APKDetailsAdapter.ViewHolder> {

    private final List<String> titles;
    private final List<Integer> icons;
    private final List<String[][]> rows;

    public APKDetailsAdapter() {
        titles = new ArrayList<>();
        icons = new ArrayList<>();
        rows = new ArrayList<>();
    }

    public void addCard(String title, int icon, String[][] cardRows) {
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

        holder.rowsRecycler.setLayoutManager(
                new LinearLayoutManager(holder.itemView.getContext())
        );

        holder.rowsRecycler.setAdapter(
                new RowsAdapter(rows.get(position))
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

        private final String[][] rows;

        RowsAdapter(String[][] rows) {
            this.rows = rows;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType
        ) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_details_row, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.rowTitle.setText(rows[position][0]);
            holder.rowValue.setText(rows[position][1]);
            holder.rowValue.setOnTouchListener(new View.OnTouchListener() {
                final GestureDetector detector = new GestureDetector(
                        holder.itemView.getContext(),
                        new GestureDetector.SimpleOnGestureListener() {

                            @Override
                            public boolean onDown(MotionEvent e) {
                                return true;
                            }

                            @Override
                            public void onLongPress(MotionEvent e) {
                                String value = holder.rowValue.getText().toString();

                                ClipboardManager clipboard = (ClipboardManager) holder.itemView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);

                                clipboard.setPrimaryClip(
                                        ClipData.newPlainText("Value", value)
                                );
                            }

                            @Override
                            public boolean onDoubleTap(MotionEvent e) {
                                if (holder.rowValue.getMaxLines() == 1) {
                                    holder.rowValue.setMaxLines(Integer.MAX_VALUE);
                                    holder.rowValue.setBreakStrategy(
                                            Layout.BREAK_STRATEGY_HIGH_QUALITY
                                    );
                                    holder.rowValue.setHyphenationFrequency(
                                            Layout.HYPHENATION_FREQUENCY_NONE
                                    );
                                } else {
                                    holder.rowValue.setMaxLines(1);
                                }

                                return true;
                            }
                        }
                );

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    detector.onTouchEvent(event);
                    holder.rowValue.setSelected(true);
                    return true;
                }
            });
        }        @Override
        public int getItemCount() {
            return rows.length;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            TextView rowTitle;
            TextView rowValue;

            ViewHolder(@NonNull View itemView) {
                super(itemView);

                rowTitle = itemView.findViewById(R.id.rowTitle);
                rowValue = itemView.findViewById(R.id.rowValue);
            }
        }
    }
}