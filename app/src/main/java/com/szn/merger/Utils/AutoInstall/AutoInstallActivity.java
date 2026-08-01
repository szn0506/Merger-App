package com.szn.merger.Utils.AutoInstall;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.szn.merger.CustomSwitchItem;
import com.szn.merger.R;
import com.szn.merger.ThemeManager;

public class AutoInstallActivity extends AppCompatActivity {

    CustomSwitchItem autoInstall, uninstallApp, deleteAfterInstall;
    MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.register(this);
        ThemeManager.applyTheme(this);
        ThemeManager.applyLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auto_install_layout);
        initView();
        setupListener();
        loadState();
    }
    @Override
    protected void onResume() {
        super.onResume();
        AutoInstallManager.onResume(this, autoInstall);
        updateState();
    }

    private void initView() {
        autoInstall = findViewById(R.id.enableAutoInstall);
        uninstallApp = findViewById(R.id.uninstallApp);
        deleteAfterInstall = findViewById(R.id.deleteAfter);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupListener() {
        autoInstall.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!AutoInstallManager.checkAndAskPermission(this)) {
                    autoInstall.setChecked(false);
                    return;
                }
            }

            AutoInstallManager.setAutoInstallEnabled(this, isChecked);
            updateState();
        });
        uninstallApp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AutoInstallManager.setUninstallAppEnabled(this, isChecked);
        });
        deleteAfterInstall.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AutoInstallManager.setDeleteAfterEnabled(this, isChecked);
        });
        toolbar.setNavigationOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });
    }

    private void updateState() {
        boolean isAutoInstall = AutoInstallManager.isAutoInstallEnabled(this);
        uninstallApp.setEnabled(isAutoInstall);
        deleteAfterInstall.setEnabled(isAutoInstall);
    }
    private void loadState() {
        autoInstall.setChecked(AutoInstallManager.isAutoInstallEnabled(this));
        uninstallApp.setChecked(AutoInstallManager.isUninstallApp(this));
        deleteAfterInstall.setChecked(AutoInstallManager.isDeleteAfterEnabled(this));
    }
}
