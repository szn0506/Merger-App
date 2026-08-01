package com.szn.merger;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.szn.merger.Utils.AutoDevice.AutoDeviceActivity;
import com.szn.merger.Utils.AutoInstall.AutoInstallActivity;
import com.szn.merger.Utils.Processing.ProcessingActivity;
import com.szn.merger.Utils.RadioAdapter;
import com.szn.merger.Utils.Signing.SigningActivity;

import java.util.Arrays;

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
        ThemeManager.applyLanguage(this);
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
        String[] languages = getResources().getStringArray(R.array.languages);
        currentLang.setText(languages[ThemeManager.getLanguage(this)]);
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
        RecyclerView recyclerView = view.findViewById(R.id.langRecyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        String[] languages = getResources().getStringArray(R.array.languages);

        RadioAdapter adapter = new RadioAdapter(Arrays.asList(languages), ((position, value) -> {
            ThemeManager.setLanguage(this, position);
            currentLang.setText(value);
            dialog.dismiss();
        }));
        adapter.setSelectedValue(languages[ThemeManager.getLanguage(this)]);
        recyclerView.setAdapter(adapter);
        searchLang.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                adapter.filter(s.toString());
            }
        });

        dialog.setContentView(view);
        dialog.show();
    }
}