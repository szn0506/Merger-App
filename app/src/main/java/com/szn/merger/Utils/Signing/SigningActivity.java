package com.szn.merger.Utils.Signing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.szn.merger.CustomSwitchItem;
import com.szn.merger.R;
import com.szn.merger.ThemeManager;

public class SigningActivity extends AppCompatActivity {
    private CustomSwitchItem signSwitch;
    private MaterialCardView signSchemes;
    private static MaterialCheckBox V1, V2, V3, V4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.register(this);
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signing_layout);
        initView();
        setupListener();
    }

    private void initView() {
        signSwitch = findViewById(R.id.SignSwitch);
        signSchemes = findViewById(R.id.signSchemes);
    }
    private void setupListener() {
        signSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SigningManager.setSignEnabled(this, isChecked);
        });
        signSchemes.setOnClickListener(v -> {
            showBottomSheet();
        });
    }
    private void showBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.sign_schemes_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();

        V1 = bottomSheetView.findViewById(R.id.checkV1);
        V2 = bottomSheetView.findViewById(R.id.checkV2);
        V3 = bottomSheetView.findViewById(R.id.checkV3);
        V4 = bottomSheetView.findViewById(R.id.checkV4);

        restoreState();

        V1.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV1Enabled(this, isChecked));

        V2.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV2Enabled(this, isChecked));

        V3.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV3Enabled(this, isChecked));

        V4.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV4Enabled(this, isChecked));
    }
    private void restoreState() {
        V1.setChecked(SigningManager.isV1Enabled(this));
        V2.setChecked(SigningManager.isV2Enabled(this));
        V3.setChecked(SigningManager.isV3Enabled(this));
        V4.setChecked(SigningManager.isV4Enabled(this));
    }
}
