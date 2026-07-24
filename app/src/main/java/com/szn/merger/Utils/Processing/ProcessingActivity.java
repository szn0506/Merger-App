package com.szn.merger.Utils.Processing;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.szn.merger.CustomSwitchItem;
import com.szn.merger.R;
import com.szn.merger.ThemeManager;
import com.szn.merger.Utils.RadioAdapter;

import java.util.Arrays;

public class ProcessingActivity extends AppCompatActivity {
    MaterialCardView outputDir, prefixSuffix, compressionLevel, logType;
    CustomSwitchItem timestamp, version;
    TextView currentPath, currentFormatName, currentCompressionLevel, currentLogType;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.register(this);
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.processing_layout);

        initViews();
        loadState();
        setupListener();
    }
    private void initViews() {
        outputDir = findViewById(R.id.card_output_directory);
        prefixSuffix = findViewById(R.id.card_file_name_prefix_suffix);
        compressionLevel = findViewById(R.id.card_compression_level);
        logType = findViewById(R.id.card_log_type);
        timestamp = findViewById(R.id.switch_append_timestamp);
        version = findViewById(R.id.switch_append_version_name);
        currentPath = findViewById(R.id.current_output_directory);
        currentFormatName = findViewById(R.id.current_file_name_prefix_suffix);
        currentCompressionLevel = findViewById(R.id.current_compression_level);
        currentLogType = findViewById(R.id.current_log_type);
    }

    private void setupListener() {
        outputDir.setOnClickListener(v -> showPathDirDialog());
        prefixSuffix.setOnClickListener( v-> showFormatNameDialog());
        timestamp.setOnCheckedChangeListener(( buttonView, isChecked) -> {
          ProcessingManager.setAppendTimestampEnabled(this, isChecked);
        });
        version.setOnCheckedChangeListener(( buttonview, isChecked) -> {
            ProcessingManager.setAppendVersionEnabled(this, isChecked);
        });
        compressionLevel.setOnClickListener( v -> showCompressionLevelDialog());
        logType.setOnClickListener( v -> showLogTypeDropdown());
    }

    private void loadState() {
        timestamp.setChecked(ProcessingManager.isAppendTimestampEnabled(this));
        version.setChecked(ProcessingManager.isAppendVersionEnabled(this));
        currentPath.setText(ProcessingManager.getDirPath(this));
        currentFormatName.setText(ProcessingManager.getFormatName(this));
        currentCompressionLevel.setText(String.valueOf(ProcessingManager.getCompressionLevel(this)));
        currentLogType.setText(ProcessingManager.getLogType(this));
    }
    private void showLogTypeDropdown() {
        View view = getLayoutInflater().inflate(R.layout.log_type_dropdown, null);
        PopupWindow popupWindow = new PopupWindow(view, (int) (220 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.showAsDropDown(logType, 0, 4, Gravity.END);

        view.findViewById(R.id.defaultLog).setOnClickListener(v -> {
            currentLogType.setText(R.string.log_type_default);
            ProcessingManager.saveLogType(this, getString(R.string.log_type_default));
            popupWindow.dismiss();
        });
        view.findViewById(R.id.simpleLog).setOnClickListener(v -> {
            currentLogType.setText(R.string.log_type_simple);
            ProcessingManager.saveLogType(this, getString(R.string.log_type_simple));
            popupWindow.dismiss();
        });
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
                    selectedValue.replaceAll("\\D+", "")
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
                dialog.dismiss();
                return;
            }
            ProcessingManager.saveDirPath(this, path);
            currentPath.setText(path);
            input.setText(path);
            dialog.dismiss();
        });
    }

    private void showFormatNameDialog() {
        View view = this.getLayoutInflater().inflate(R.layout.format_name_dialog, null);
        TextInputEditText prefix = view.findViewById(R.id.inputPrefix);
        TextInputEditText suffix = view.findViewById(R.id.inputSuffix);
        MaterialButton cancel = view.findViewById(R.id.buttonCancel);
        MaterialButton confirm = view.findViewById(R.id.buttonConfirm);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
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

            String basename = "MyApp";

            if (!prefixText.isEmpty()) {
                basename = prefixText + "_" + basename;
            }

            if (!suffixText.isEmpty()) {
                basename = basename + "_" + suffixText;
            }

            String result = basename + ".apk";

            ProcessingManager.saveFormatName(this, result);

            currentFormatName.setText(result.isEmpty() ? "None" : result);

            dialog.dismiss();
        });;
    }
}
