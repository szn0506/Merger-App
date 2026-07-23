package com.szn.merger;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.szn.merger.Utils.AutoDevice.AutoDeviceActivity;
import com.szn.merger.Utils.AutoInstall.AutoInstallActivity;
import com.szn.merger.Utils.Processing.ProcessingActivity;
import com.szn.merger.Utils.Signing.SigningActivity;

public class SettingsActivity extends AppCompatActivity {

    private MaterialCardView languageButton;
    private MaterialCardView themeCard;

    private TextView currentTheme;
    private TextView currentLang;
    private MaterialCardView autoDetect, autoInstall, sign, process;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.register(this);
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setupListeners();
    }

    private void initViews() {
        languageButton = findViewById(R.id.layout_language);
        themeCard = findViewById(R.id.card_theme);
        currentLang = findViewById(R.id.text_current_language);
        currentTheme = findViewById(R.id.text_current_theme);
        autoDetect = findViewById(R.id.AutoDevice);
        autoInstall = findViewById(R.id.AutoInstall);
        sign = findViewById(R.id.Signing);
        process = findViewById(R.id.Process);

        // 1. Inflate both Switch components from XML
        CustomSwitchItem materialYou = findViewById(R.id.MaterialYou);
        CustomSwitchItem pureBlack = findViewById(R.id.PureBlack);

        // 2. Connect both switches to their manager to keep them synchronized and prevent conflicts
        ThemeManager.setupMaterialYouSwitch(this, materialYou);
        ThemeManager.setupPureBlackSwitch(this, pureBlack);

        // THE KEY POINT HERE: All popup logic bound to the card is handled inside this single line
        ThemeManager.setupThemePopup(this, themeCard, currentTheme);
    }

    private void setupListeners() {
        languageButton.setOnClickListener(v -> showLanguageSheet());
        autoDetect.setOnClickListener(v -> startActivity(new Intent(this, AutoDeviceActivity.class)));
        autoInstall.setOnClickListener(view -> startActivity(new Intent(this, AutoInstallActivity.class)));
        sign.setOnClickListener(view -> startActivity(new Intent(this, SigningActivity.class)));
        process.setOnClickListener(view -> startActivity(new Intent(this, ProcessingActivity.class)));
    }


    // PLACEHOLDER FOR LANGUAGE FEATURE
    private void showLanguageSheet() {
        View view = getLayoutInflater().inflate(R.layout.lang_sheet, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        EditText searchLang = view.findViewById(R.id.search_lang);
        RadioGroup radioGroup = view.findViewById(R.id.radioGroupLang);

        searchLang.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                filterLanguages(radioGroup, s.toString());
            }
        });

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = view.findViewById(checkedId);
            if (selected != null) {
                currentLang.setText(selected.getText());
                dialog.dismiss();
            }
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void filterLanguages(RadioGroup group, String query) {
        String lowerQuery = query.toLowerCase().trim();
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof RadioButton) {
                RadioButton rb = (RadioButton) child;
                boolean isMatch = rb.getText().toString().toLowerCase().contains(lowerQuery);
                rb.setVisibility(isMatch ? View.VISIBLE : View.GONE);
            }
        }
    }
}