package com.szn.merger.Utils.AutoInstall;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.szn.merger.CustomSwitchItem;
import com.szn.merger.R;
import com.szn.merger.ThemeManager;

public class AutoInstallActivity extends AppCompatActivity {

    CustomSwitchItem autoInstall, uninstallApp, deleteAfterInstall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.register(this);
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auto_install_layout);
        initView();
        setupListener();
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
    }

    private void updateState() {
        boolean isAutoInstall = AutoInstallManager.isAutoInstallEnabled(this);
        uninstallApp.setEnabled(isAutoInstall);
        deleteAfterInstall.setEnabled(isAutoInstall);
    }
}
