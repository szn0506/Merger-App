package com.szn.merger;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.materialswitch.MaterialSwitch;

public class CustomSwitchItem extends LinearLayout {

    private final MaterialSwitch switchButton;

    public CustomSwitchItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context)
                .inflate(R.layout.switch_item, this, true);

        TextView titleView = findViewById(R.id.txt_title);
        ImageView iconView = findViewById(R.id.img_icon);

        switchButton = findViewById(R.id.switch_action);

        TypedArray array = context.obtainStyledAttributes(
                attrs,
                R.styleable.CustomSwitchItem
        );

        titleView.setText(
                array.getString(R.styleable.CustomSwitchItem_itemTitle)
        );

        int iconRes = array.getResourceId(
                R.styleable.CustomSwitchItem_itemIcon,
                0
        );

        if (iconRes != 0) {
            iconView.setImageResource(iconRes);
            iconView.setVisibility(VISIBLE);
        }

        array.recycle();

        setOnClickListener(v -> {

            // prevent double toggle
            if (!switchButton.isPressed()) {
                switchButton.toggle();
            }
        });
    }

    public void setChecked(boolean checked) {
        switchButton.setChecked(checked);
    }

    public void setOnCheckedChangeListener(
            CompoundButton.OnCheckedChangeListener listener
    ) {
        switchButton.setOnCheckedChangeListener(listener);
    }
}