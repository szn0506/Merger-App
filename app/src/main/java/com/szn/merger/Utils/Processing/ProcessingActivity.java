package com.szn.merger.Utils.Processing;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
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
import com.szn.merger.Utils.Adapter.FormatNameReorderAdapter;
import com.szn.merger.Utils.RadioAdapter;

import java.util.Arrays;

public class ProcessingActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    MaterialCardView outputDir, prefixSuffix, compressionLevel;
    CustomSwitchItem extractNativeLibs;
    TextView currentPath, currentFormatName, currentCompressionLevel;
    FormatNameReorderAdapter formatNameReorderAdapter;

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
        currentPath.setText(ProcessingManager.isSaveToOriginalPathEnabled(this) ? getString(R.string.save_to_original_path_preview) : ProcessingManager.getDirPath(this));
        currentFormatName.setText(getString(R.string.preview_format) + ": " + ProcessingManager.getFormatName(this));
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
        View view = getLayoutInflater().inflate(R.layout.output_path_dialog, null);

        TextInputEditText input = view.findViewById(R.id.input);
        MaterialButton btnConfirm = view.findViewById(R.id.buttonConfirm);
        MaterialButton btnCancel = view.findViewById(R.id.buttonCancel);
        CustomSwitchItem saveToOriginalPath = view.findViewById(R.id.saveToOriginalPath);
        input.setText(!ProcessingManager.isSaveToOriginalPathEnabled(this) ? ProcessingManager.getDirPath(this) : "");
        final boolean[] saveOriginal = {
                ProcessingManager.isSaveToOriginalPathEnabled(this)
        };

        saveToOriginalPath.setChecked(saveOriginal[0]);

        saveToOriginalPath.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveOriginal[0] = isChecked;

            ProcessingManager.setSaveToOriginalPath(this, isChecked);

            if (isChecked) {
                input.setText("");
            }
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0 && saveOriginal[0]) {
                    saveToOriginalPath.setChecked(false);
                } else if (s.length() == 0) {
                    saveToOriginalPath.setChecked(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String path = input.getText().toString().trim();

            if (!path.isEmpty() && !ProcessingManager.isSaveToOriginalPathEnabled(this)) {
                ProcessingManager.saveDirPath(this, path);
                currentPath.setText(path);
            } else {
                currentPath.setText(R.string.save_to_original_path_preview);
            }

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
        TextView previewText = view.findViewById(R.id.preview);

        RecyclerView recyclerView = view.findViewById(R.id.reorderList);
        formatNameReorderAdapter = new FormatNameReorderAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(formatNameReorderAdapter);
        formatNameReorderAdapter.attachDragSupport(recyclerView);
        formatNameReorderAdapter.setOnOrderChangedListener(() -> previewText.setText(refreshFormatPreview()));
        // restore state
        keepOriginalName.setChecked(ProcessingManager.isKeepOriginalNameEnabled(this));
        packageName.setChecked(ProcessingManager.isAppendPackageNameEnabled(this));
        versionName.setChecked(ProcessingManager.isAppendVersionNameEnabled(this));
        versionCode.setChecked(ProcessingManager.isAppendVersionCodeEnabled(this));
        ABI.setChecked(ProcessingManager.isAppendABIEnabled(this));
        DPI.setChecked(ProcessingManager.isAppendDPIEnabled(this));
        LANGUAGE.setChecked(ProcessingManager.isAppendLanguageEnabled(this));
        signingStatus.setChecked(ProcessingManager.isAppendSigningStatusEnabled(this));
        signingSchemes.setChecked(ProcessingManager.isAppendSigningSchemesEnabled(this));
        timestamp.setChecked(ProcessingManager.isAppendTimestampEnabled(this));
        sdkVersions.setChecked(ProcessingManager.isAppendSDKVersionsEnabled(this));

        prefix.setText(ProcessingManager.getPrefix(this));
        suffix.setText(ProcessingManager.getSuffix(this));

        previewText.setText(refreshFormatPreview());
        // init
        keepOriginalName.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ProcessingManager.setKeepOriginalName(this, isChecked);
            updateState(keepOriginalName);
            formatNameReorderAdapter.checkEnabledItems();
            previewText.setText(refreshFormatPreview());
        });

        packageName.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ProcessingManager.setAppendPackageName(this, isChecked);
            updateState(keepOriginalName);
            formatNameReorderAdapter.checkEnabledItems();
            previewText.setText(refreshFormatPreview());
        });

        versionName.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ProcessingManager.setAppendVersionName(this, isChecked);
            updateState(keepOriginalName);
            formatNameReorderAdapter.checkEnabledItems();
            previewText.setText(refreshFormatPreview());
        });

        versionCode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ProcessingManager.setAppendVersioncode(this, isChecked);
            updateState(keepOriginalName);
            formatNameReorderAdapter.checkEnabledItems();
            previewText.setText(refreshFormatPreview());
        });

        ABI.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ProcessingManager.setAppendABI(this, isChecked);
            updateState(keepOriginalName);
            formatNameReorderAdapter.checkEnabledItems();
            previewText.setText(refreshFormatPreview());
        });

        DPI.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ProcessingManager.setAppendDPI(this, isChecked);
            updateState(keepOriginalName);
            formatNameReorderAdapter.checkEnabledItems();
            previewText.setText(refreshFormatPreview());
        });

        LANGUAGE.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ProcessingManager.setAppendLanguage(this, isChecked);
            updateState(keepOriginalName);
            formatNameReorderAdapter.checkEnabledItems();
            previewText.setText(refreshFormatPreview());
        });

        signingStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ProcessingManager.setAppendSigningStatus(this, isChecked);
            updateState(keepOriginalName);
            formatNameReorderAdapter.checkEnabledItems();
            previewText.setText(refreshFormatPreview());
        });

        signingSchemes.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ProcessingManager.setAppendSigningSchemes(this, isChecked);
            updateState(keepOriginalName);
            formatNameReorderAdapter.checkEnabledItems();
            previewText.setText(refreshFormatPreview());
        });

        timestamp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ProcessingManager.setAppendTimestamp(this, isChecked);
            updateState(keepOriginalName);
            formatNameReorderAdapter.checkEnabledItems();
            previewText.setText(refreshFormatPreview());
        });

        sdkVersions.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ProcessingManager.setAppendSDKVersions(this, isChecked);
            updateState(keepOriginalName);
            formatNameReorderAdapter.checkEnabledItems();
            previewText.setText(refreshFormatPreview());
        });

        prefix.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                ProcessingManager.setPrefix(ProcessingActivity.this, charSequence.toString());
                previewText.setText(refreshFormatPreview());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        suffix.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                ProcessingManager.setSuffix(ProcessingActivity.this, charSequence.toString());
                previewText.setText(refreshFormatPreview());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);
        dialog.show();

        cancel.setOnClickListener( v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
                ProcessingManager.saveFormatName(this, refreshFormatPreview());
                currentFormatName.setText((refreshFormatPreview()));
                dialog.dismiss();
        });
    }
    void updateState(CustomSwitchItem keepOriginalName) {
        boolean isAnyFeatureEnabled =
                ProcessingManager.isAppendPackageNameEnabled(this)
                        || ProcessingManager.isAppendVersionNameEnabled(this)
                        || ProcessingManager.isAppendVersionCodeEnabled(this)
                        || ProcessingManager.isAppendABIEnabled(this)
                        || ProcessingManager.isAppendDPIEnabled(this)
                        || ProcessingManager.isAppendLanguageEnabled(this)
                        || ProcessingManager.isAppendSigningStatusEnabled(this)
                        || ProcessingManager.isAppendSigningSchemesEnabled(this)
                        || ProcessingManager.isAppendTimestampEnabled(this)
                        || ProcessingManager.isAppendSDKVersionsEnabled(this);

        keepOriginalName.setEnabled(isAnyFeatureEnabled);
        if (!isAnyFeatureEnabled) {
            ProcessingManager.setKeepOriginalName(this, true);
            keepOriginalName.setChecked(true);
        }
    }

    String refreshFormatPreview() {
        String defaultPreview[] = {"MyApp", ".apk"};
        StringBuilder preview = new StringBuilder();

        preview.append(!ProcessingManager.getPrefix(this).isEmpty() ? ProcessingManager.getPrefix(this) + "_" : "");

        preview.append(ProcessingManager.isKeepOriginalNameEnabled(this) ? defaultPreview[0] : "");

        for (FormatNameReorderAdapter.Item item : formatNameReorderAdapter.getItems()) {

            switch (item.title) {

                case "Package Name":
                    preview.append(getString(R.string.preview_package_name));
                    break;

                case "Version Name":
                    preview.append(getString(R.string.preview_version_name));
                    break;

                case "Version Code":
                    preview.append(getString(R.string.preview_version_code));
                    break;

                case "ABI":
                    preview.append(getString(R.string.preview_abi));
                    break;

                case "DPI":
                    preview.append(getString(R.string.preview_dpi));
                    break;

                case "Language":
                    preview.append(getString(R.string.preview_language));
                    break;

                case "Signing Status":
                    preview.append(getString(R.string.preview_signing_status));
                    break;

                case "Signing Schemes":
                    preview.append(getString(R.string.preview_signing_schemes));
                    break;

                case "Timestamp":
                    preview.append(getString(R.string.preview_timestamp));
                    break;

                case "SDK Versions":
                    preview.append(getString(R.string.preview_sdk_versions));
                    break;
            }
        }

        preview.append(!ProcessingManager.getSuffix(this).isEmpty() ? "_" + ProcessingManager.getSuffix(this) : "");

        preview.append(defaultPreview[1]);

        return preview.toString();
    }
}
