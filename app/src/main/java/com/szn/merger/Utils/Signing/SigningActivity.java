package com.szn.merger.Utils.Signing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.szn.merger.CustomSwitchItem;
import com.szn.merger.R;
import com.szn.merger.ThemeManager;

public class SigningActivity extends AppCompatActivity {
    private CustomSwitchItem signSwitch;
    private MaterialCardView signSchemes;
    private static MaterialCheckBox V1, V2, V3, V4;
    private TextView currentSchemes;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.register(this);
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signing_layout);
        initView();
        setupListener();
        loadState();
        updatePlaceholder();
    }

    private void initView() {
        signSwitch = findViewById(R.id.SignSwitch);
        signSchemes = findViewById(R.id.signSchemes);
        currentSchemes = findViewById(R.id.currentSchemes);
        toolbar = findViewById(R.id.toolbar);
    }
    private void setupListener() {
        signSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SigningManager.setSignEnabled(this, isChecked);
        });
        signSchemes.setOnClickListener(v -> {
            showBottomSheet();
        });
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
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
        MaterialButton doneButton = bottomSheetView.findViewById(R.id.doneButton);
        restoreState();

        V1.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV1Enabled(this, isChecked));

        V2.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV2Enabled(this, isChecked));

        V3.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV3Enabled(this, isChecked));

        V4.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV4Enabled(this, isChecked));
        doneButton.setOnClickListener(v -> {
            updatePlaceholder();
            bottomSheetDialog.dismiss();
        });
    }
    private void restoreState() {
        V1.setChecked(SigningManager.isV1Enabled(this));
        V2.setChecked(SigningManager.isV2Enabled(this));
        V3.setChecked(SigningManager.isV3Enabled(this));
        V4.setChecked(SigningManager.isV4Enabled(this));
    }
    private void updatePlaceholder() {
        StringBuilder schemes = new StringBuilder();

        if (SigningManager.isV1Enabled(this)) schemes.append("V1");
        if (SigningManager.isV2Enabled(this)) {
            if (schemes.length() > 0) schemes.append(", ");
            schemes.append("V2");
        }
        if (SigningManager.isV3Enabled(this)) {
            if (schemes.length() > 0) schemes.append(", ");
            schemes.append("V3");
        }
        if (SigningManager.isV4Enabled(this)) {
            if (schemes.length() > 0) schemes.append(", ");
            schemes.append("V4");
        }

        currentSchemes.setText(schemes.length() > 0 ? schemes.toString() : "None");
    }
    private void loadState() {
        signSwitch.setChecked(SigningManager.isSignEnabled(this));
    }
}
