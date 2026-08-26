package com.szn.merger.Utils.Processing;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.szn.merger.CustomSwitchItem;
import com.szn.merger.R;
import com.szn.merger.ThemeManager;
import com.szn.merger.Utils.RadioAdapter;

import java.util.Arrays;

public class ProcessingActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    MaterialCardView outputDir, prefixSuffix, compressionLevel;
    CustomSwitchItem extractNativeLibs;
    TextView currentPath, currentFormatName, currentCompressionLevel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        ThemeManager.applyLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.processing_layout);
        initViews();
        loadState();
        setupListener();
    }
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        extractNativeLibs = findViewById(R.id.extract_native_libs);
        outputDir = findViewById(R.id.card_output_directory);
        prefixSuffix = findViewById(R.id.card_file_name_prefix_suffix);
        compressionLevel = findViewById(R.id.card_compression_level);
        currentPath = findViewById(R.id.current_output_directory);
        currentFormatName = findViewById(R.id.current_file_name_prefix_suffix);
        currentCompressionLevel = findViewById(R.id.current_compression_level);
    }

    private void setupListener() {
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        extractNativeLibs.setOnCheckedChangeListener((buttonView, isChecked) -> ProcessingManager.setExtractNativeLibs(this, isChecked));
        outputDir.setOnClickListener(v -> showPathDirDialog());
        prefixSuffix.setOnClickListener( v-> showFormatNameBottomSheet());
        compressionLevel.setOnClickListener(v -> showCompressionLevelDialog());
    }

    private void loadState() {
        currentPath.setText(ProcessingManager.getDirPath(this));
        String savedFormat = ProcessingManager.getFormatName(this);
        if (!savedFormat.isEmpty()) currentFormatName.setText(savedFormat + ".apk");
        else currentFormatName.setText("MyApp.apk");
        currentCompressionLevel.setText(String.valueOf(ProcessingManager.getCompressionLevel(this)));
    }

    private void showCompressionLevelDialog() {
        View view = this.getLayoutInflater().inflate(R.layout.compression_level_dialog, null);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        MaterialButton cancel = view.findViewById(R.id.buttonCancel);
        MaterialButton confirm = view.findViewById(R.id.buttonConfirm);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        RadioAdapter adapter = new RadioAdapter(Arrays.asList(getResources().getStringArray(R.array.compression_levels)), (position, value) -> {

        });
        recyclerView.setAdapter(adapter);
        adapter.setSelectedValue(String.valueOf(ProcessingManager.getCompressionLevel(this)));
        dialog.show();

        cancel.setOnClickListener( v -> dialog.dismiss());
        confirm.setOnClickListener( v-> {
            String selectedValue = adapter.getSelectedValue();
            int compressionLevel = Integer.parseInt(
                    selectedValue.replaceAll("\\D+", "") //NON-NLS
            );
            ProcessingManager.saveCompressionLevel(this, compressionLevel);
            currentCompressionLevel.setText(String.valueOf(compressionLevel));
            dialog.dismiss();
        });
    }

    private void showPathDirDialog() {
        View view = this.getLayoutInflater().inflate(R.layout.output_path_dialog, null);
        TextInputEditText input = view.findViewById(R.id.input);
        MaterialButton btnConfirm = view.findViewById(R.id.buttonConfirm);
        MaterialButton btnCancel = view.findViewById(R.id.buttonCancel);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        input.setText(ProcessingManager.getDirPath(this));
        dialog.show();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String path = input.getText().toString().trim();
            if (path.isEmpty()) {
                path = "/storage/emulated/0/Download"; //NON-NLS
                ProcessingManager.saveDirPath(this, path);
                currentPath.setText(path);
                input.setText(path);
                dialog.dismiss();
                return;
            }
            ProcessingManager.saveDirPath(this, path);
            currentPath.setText(path);
            input.setText(path);
            dialog.dismiss();
        });
    }

    private void showFormatNameBottomSheet() {
        View view = this.getLayoutInflater().inflate(R.layout.format_name_dialog, null);
        TextInputEditText prefix = view.findViewById(R.id.inputPrefix);
        TextInputEditText suffix = view.findViewById(R.id.inputSuffix);
        MaterialButton cancel = view.findViewById(R.id.buttonCancel);
        MaterialButton confirm = view.findViewById(R.id.buttonConfirm);
        CustomSwitchItem keepOriginalName = view.findViewById(R.id.keepFileOriginalName),
        packageName = view.findViewById(R.id.packageName),
        versionName = view.findViewById(R.id.versionName),
        versionCode = view.findViewById(R.id.versionCode),
        ABI = view.findViewById(R.id.ABI),
        DPI = view.findViewById(R.id.DPI),
        LANGUAGE = view.findViewById(R.id.LANGUAGE),
        signingStatus = view.findViewById(R.id.signingStatus),
        signingSchemes = view.findViewById(R.id.signingSchemes),
        timestamp = view.findViewById(R.id.timestamp),
        sdkVersions = view.findViewById(R.id.sdkVersions);

        // init
        keepOriginalName.setOnCheckedChangeListener((buttonView, isChecked) -> ProcessingManager.setKeepOriginalName(this, isChecked));
        packageName.setOnCheckedChangeListener((buttonView, isChecked) -> ProcessingManager.setAppendPackageName(this, isChecked));
        versionName.setOnCheckedChangeListener((buttonView, isChecked) -> ProcessingManager.setAppendVersionName(this, isChecked));
        versionCode.setOnCheckedChangeListener((buttonView, isChecked) -> ProcessingManager.setAppendVersioncode(this, isChecked));
        ABI.setOnCheckedChangeListener((buttonView, isChecked) -> ProcessingManager.setAppendABI(this, isChecked));
        DPI.setOnCheckedChangeListener((buttonView, isChecked) -> ProcessingManager.setAppendDPI(this, isChecked));
        LANGUAGE.setOnCheckedChangeListener((buttonView, isChecked) -> ProcessingManager.setAppendLanguage(this, isChecked));
        signingStatus.setOnCheckedChangeListener((buttonView, isChecked) -> ProcessingManager.setAppendSigningStatus(this, isChecked));
        signingSchemes.setOnCheckedChangeListener((buttonView, isChecked) -> ProcessingManager.setAppendSigningSchemes(this, isChecked));
        timestamp.setOnCheckedChangeListener((buttonView, isChecked) -> ProcessingManager.setAppendTimestamp(this, isChecked));
        sdkVersions.setOnCheckedChangeListener((buttonView, isChecked) -> ProcessingManager.setAppendSDKVersions(this, isChecked));

        // restore state
        packageName.setChecked(ProcessingManager.isAppendPackageNameEnabled(this));
        versionName.setChecked(ProcessingManager.isAppendVersionNameEnabled(this));
        versionCode.setChecked(ProcessingManager.isAppendVersioncCodeEnabled(this));
        ABI.setChecked(ProcessingManager.isAppendABIEnabled(this));
        DPI.setChecked(ProcessingManager.isAppendDPIEnabled(this));
        LANGUAGE.setChecked(ProcessingManager.isAppendLanguageEnabled(this));
        signingStatus.setChecked(ProcessingManager.isAppendSigningStatusEnabled(this));
        signingSchemes.setChecked(ProcessingManager.isAppendSigningSchemesEnabled(this));
        timestamp.setChecked(ProcessingManager.isAppendTimestampEnabled(this));
        sdkVersions.setChecked(ProcessingManager.isAppendSDKVersionsEnabled(this));

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);
        dialog.show();

        String savedPrefix = ProcessingManager.getPrefix(this);
        String savedSuffix = ProcessingManager.getSuffix(this);

        if (savedPrefix != null && !savedPrefix.equals("_") && !savedPrefix.isEmpty()) {
            prefix.setText(savedPrefix);
        }

        if (savedSuffix != null && !savedSuffix.equals("_") && !savedSuffix.isEmpty()) {
            suffix.setText(savedSuffix.replace("_.apk", ""));
        }
        dialog.show();

        cancel.setOnClickListener( v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            String prefixText = prefix.getText().toString().trim();
            String suffixText = suffix.getText().toString().trim();

            if (prefixText.equals("_")) prefixText = "";
            if (suffixText.equals("_")) suffixText = "";

            ProcessingManager.setPrefix(this, prefixText);
            ProcessingManager.setSuffix(this, suffixText);

            String basename = "MyApp";

            String result = basename + ".apk";

            ProcessingManager.saveFormatName(this, result);

            currentFormatName.setText(result);

            dialog.dismiss();
        });;
    }
}
