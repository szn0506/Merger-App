package com.szn.merger;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.szn.merger.Utils.Utils;

import java.io.File;

public class MainActivity extends AppCompatActivity implements SAFHelper.OnFilePickedListener, AppsDialog.OnAppExtractedListener {
    private EditText editFilePath;
    private File selectedInputFile;
    private SAFHelper safHelper;
    private AppsDialog appsDialog;
    private MergeTaskManager mergeTaskManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.register(this);
        ThemeManager.applyTheme(this);
        ThemeManager.applyLanguage(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        TextView logText = findViewById(R.id.logtext);
        NestedScrollView scrollCard = findViewById(R.id.scroll);
        scrollCard.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        scrollCard.setFocusable(true);
        scrollCard.setFocusableInTouchMode(true);
        scrollCard.requestFocus();

        editFilePath = findViewById(R.id.editFilePath);
        Button btnMerge = findViewById(R.id.btnConvert);
        ImageView btnFilePicker = findViewById(R.id.btnFilePicker);
        Button btnExtract = findViewById(R.id.btnExtract);

        // Find the container view
        ViewGroup inputCard = findViewById(R.id.inputCard);

        // Set up the animation
        Utils.animateTextChange(editFilePath, inputCard, 500, text -> {
            // Reset selectedInputFile to null if the user types manually
            if (getCurrentFocus() == editFilePath) {
                selectedInputFile = null;
            }
        });

        safHelper = new SAFHelper(this, this);
        appsDialog = new AppsDialog(this, this);
        mergeTaskManager = new MergeTaskManager(this, logText, scrollCard, findViewById(R.id.logCard));

        btnFilePicker.setOnClickListener(v -> safHelper.launchPicker());
        btnExtract.setOnClickListener(v -> appsDialog.show());
        btnMerge.setOnClickListener(v -> mergeTaskManager.startMergeFlow(selectedInputFile, editFilePath));
    }

    @Override
    public void onFilePicked(File file, String fileName) {
        selectedInputFile = file;
        editFilePath.setText(fileName);
        Utils.toast(this, "Selected file: " + fileName);
    }

    @Override
    public void onAppExtractionStart(String message) {
        runOnUiThread(() -> {
            editFilePath.setText("Extracting...");
            Utils.toast(this, message);
        });
    }

    @Override
    public void onAppExtractionSuccess(File file, String fileName) {
        selectedInputFile = file;
        editFilePath.setText(fileName);
        Utils.toast(this, "Successfully extracted: " + fileName);
    }

    @Override
    public void onAppExtractionFailed(String errorMsg) {
        editFilePath.setText("");
        Utils.toast(this, "Failed to extract: " + errorMsg);
    }

    @Override
    public void onError(String errorMsg) {
        Utils.toast(this, "Failed to process file: " + errorMsg);
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            startActivity(new android.content.Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}