package com.szn.merger.Utils.AutoDevice;

import static com.szn.merger.Utils.AutoDevice.AutoDeviceManager.isAutoConfigEnabled;
import static com.szn.merger.Utils.AutoDevice.AutoDeviceManager.isAutoDetectEnabled;
import static com.szn.merger.Utils.AutoDevice.AutoDeviceManager.listSplits;
import static com.szn.merger.Utils.AutoDevice.AutoDeviceManager.selectedSplits;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.szn.merger.CustomSwitchItem;
import com.szn.merger.R;
import com.szn.merger.ThemeManager;
import com.szn.merger.Utils.CheckBoxAdapter;
import com.szn.merger.Utils.RadioAdapter;
import com.szn.merger.Utils.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class AutoDeviceActivity extends AppCompatActivity {
    BottomSheetDialog bottomSheetDialog;
    private RecyclerView recyclerCustom;
    TextView title, ABIMode, DPIMode, LANGUAGEMode;
    EditText textOnSearch;
    ImageButton backButton;
    MaterialToolbar toolbar;
    View universalPage, customPage;
    public static int currentCaller;
    MaterialRadioButton disabledRadio, fromDeviceRadio, customRadio;
    CustomSwitchItem autoDetect, autoConfig;

    LinearLayout ABILinear, DPILinear, LANGUAGELinear;
    MaterialCardView gotoABI, gotoDPI, gotoLANGUAGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.register(this);
        ThemeManager.applyTheme(this);
        ThemeManager.applyLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auto_device_layout);

        initViews();      // Initialize all views from the layout
        loadState();      // Restore saved switch states from SharedPreferences
        setupListener();  // Register listeners to handle user interactions
        updateState();    // Update the UI based on the current state
    }

    private void initViews() {
        // Bind all layout views to their corresponding variables
        autoDetect = findViewById(R.id.AutoDetect);
        autoConfig = findViewById(R.id.AutoConfig);
        ABILinear = findViewById(R.id.ABI);
        DPILinear = findViewById(R.id.DPI);
        LANGUAGELinear = findViewById(R.id.LANGUAGE);
        gotoABI = findViewById(R.id.gotoABI);
        gotoDPI = findViewById(R.id.gotoDPI);
        gotoLANGUAGE = findViewById(R.id.gotoLANGUAGE);
        ABIMode = findViewById(R.id.ABImode_placeholder);
        DPIMode = findViewById(R.id.DPImode_placeholder);
        LANGUAGEMode = findViewById(R.id.LANGUAGEmode_placeholder);
        toolbar = findViewById(R.id.toolbar);
    }
    private void loadState() {
        autoDetect.setChecked(isAutoDetectEnabled(this));
        autoConfig.setChecked(isAutoConfigEnabled(this));
        toolbar.setNavigationOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });
        restorePlaceholder(AutoDeviceManager.ABI, ABIMode);
        restorePlaceholder(AutoDeviceManager.DPI, DPIMode);
        restorePlaceholder(AutoDeviceManager.LANGUAGE, LANGUAGEMode);
    }

    private void restorePlaceholder(int caller, TextView placeholder) {

        String mode = AutoDeviceManager.getMode(this, caller);

        switch (mode) {

            case AutoDeviceManager.MODE_DISABLED:
                placeholder.setText(R.string.mode_disabled);
                break;

            case AutoDeviceManager.MODE_FROM_DEVICE:
                placeholder.setText(R.string.mode_from_device);
                break;

            default:
                placeholder.setText(getString(R.string.custom_mode, mode));
                break;
        }
    }

    private void setupListener() {

        // Listen for Auto Detect state changes
        autoDetect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AutoDeviceManager.setAutoDetectEnabled(this, isChecked);
            updateState(); // Refresh the UI after the state changes
        });

        // Listen for Auto Config state changes
        autoConfig.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AutoDeviceManager.setAutoConfigEnabled(this, isChecked);
            updateState();
        });
        ABILinear.setOnClickListener(v -> gotoABI.performClick());
        DPILinear.setOnClickListener(v -> gotoDPI.performClick());
        LANGUAGELinear.setOnClickListener(v -> gotoLANGUAGE.performClick());
        gotoABI.setOnClickListener(view -> {
            currentCaller = AutoDeviceManager.ABI;
            showUniversalBottomSheet();
        });
        gotoDPI.setOnClickListener(view -> {
            currentCaller = AutoDeviceManager.DPI;
            showUniversalBottomSheet();
        });
        gotoLANGUAGE.setOnClickListener(view -> {
            currentCaller = AutoDeviceManager.LANGUAGE;
            showUniversalBottomSheet();
        });
    }

    private void showUniversalBottomSheet() {
        AutoDeviceManager.getMode(this, currentCaller);
        bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.auto_device_bottom_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        universalPage = bottomSheetView.findViewById(R.id.universalPage);
        customPage = bottomSheetView.findViewById(R.id.customPage);
        title = bottomSheetView.findViewById(R.id.text_placeholder);
        textOnSearch = bottomSheetView.findViewById(R.id.search_placeholder);
        backButton = bottomSheetView.findViewById(R.id.backButton);
        customRadio = bottomSheetView.findViewById(R.id.radioCustom);
        recyclerCustom = bottomSheetView.findViewById(R.id.recyclerCustom);
        disabledRadio = bottomSheetView.findViewById(R.id.radioDisabled);
        fromDeviceRadio = bottomSheetView.findViewById(R.id.radioFromdevice);

        restoreRadioState();
        bottomSheetDialog.show();
        customRadio.setOnClickListener(v -> {
            customRadio.setChecked(false);
            showCustomPage();
        });

        disabledRadio.setOnClickListener(v -> {
            selectMode(
                    AutoDeviceManager.MODE_DISABLED,
                    getString(R.string.mode_disabled)
            );
        });

        fromDeviceRadio.setOnClickListener(v -> {
            selectMode(
                    AutoDeviceManager.MODE_FROM_DEVICE,
                    getString(R.string.mode_from_device)
            );
        });
    }

    public static void showSplitsPicker(Activity activity, List<String> allEntries) {
        AutoDeviceManager.selectedSplits.clear();

        if ((!isAutoDetectEnabled(activity) || isAutoConfigEnabled(activity))) {
            return;
        }
        List<String> splits = listSplits(allEntries);
        CountDownLatch latch = new CountDownLatch(1);

        activity.runOnUiThread(() -> {

            View view = activity.getLayoutInflater().inflate(R.layout.dialog_select_split, null);

            RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
            MaterialButton button = view.findViewById(R.id.btnOk);
            MaterialButton selectAll = view.findViewById(R.id.selectAll);
            TextView selectedCount = view.findViewById(R.id.selected);

            recyclerView.setLayoutManager(new LinearLayoutManager(activity));

            int total = splits.size();

            CheckBoxAdapter adapter = new CheckBoxAdapter(
                    splits,
                    (position, value, selectedCount1) -> {
                        selectedCount.setText(
                                activity.getString(
                                        R.string.selected_count,
                                        selectedCount1,
                                        total
                                )
                        );
                    }
            );
            selectedCount.setText(activity.getString(R.string.selected_count, adapter.getSelectedCount(), adapter.getItemCount()));

            selectAll.setOnClickListener(view1 -> adapter.toggleSelectAll());
            recyclerView.setAdapter(adapter);

            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setView(view)
                    .create();

            button.setOnClickListener(v -> {
                selectedSplits.clear();
                selectedSplits.addAll(adapter.getCheckedItems());

                dialog.dismiss();
                latch.countDown();
            });
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    private void showCustomPage() {
        recyclerCustom.setLayoutManager(new LinearLayoutManager(this));
        universalPage.setVisibility(View.GONE);
        customPage.setVisibility(View.VISIBLE);
        String[] list;

        switch (currentCaller) {
            case AutoDeviceManager.ABI:
                list = getResources().getStringArray(R.array.abi_list);
                updateTextPlaceholder(title, textOnSearch, getString(R.string.label_abi));
                break;
            case AutoDeviceManager.DPI:
                list = getResources().getStringArray(R.array.dpi_list);
                updateTextPlaceholder(title, textOnSearch, getString(R.string.label_dpi));
                break;
            case AutoDeviceManager.LANGUAGE:
                list = getResources().getStringArray(R.array.language_list);
                updateTextPlaceholder(title, textOnSearch, getString(R.string.label_language));
                break;
            default:
                list = new String[0];
        }

        RadioAdapter adapter = new RadioAdapter(Arrays.asList(list), (position, value) -> {
            selectMode(value, getString(R.string.custom_mode, value));
        });

        String savedMode = AutoDeviceManager.getMode(this, currentCaller);

        if (!savedMode.equals(AutoDeviceManager.MODE_DISABLED)
                && !savedMode.equals(AutoDeviceManager.MODE_FROM_DEVICE)) {
            adapter.setSelectedValue(savedMode);
        }

        recyclerCustom.setAdapter(adapter);
        Utils.animateTextChange(textOnSearch, recyclerCustom, 150, adapter::filter);


        backButton.setOnClickListener(v -> {
            universalPage.setVisibility(View.VISIBLE);
            customPage.setVisibility(View.GONE);
        });
    }

    private void restoreRadioState() {

        disabledRadio.setChecked(false);
        fromDeviceRadio.setChecked(false);
        customRadio.setChecked(false);
        customRadio.setText(R.string.mode_custom);

        String mode = AutoDeviceManager.getMode(this, currentCaller);

        if (mode.equals(AutoDeviceManager.MODE_DISABLED)) {

            disabledRadio.setChecked(true);

        } else if (mode.equals(AutoDeviceManager.MODE_FROM_DEVICE)) {

            fromDeviceRadio.setChecked(true);

        } else if (!mode.isEmpty()) {

            customRadio.setChecked(true);
            customRadio.setText(getString(R.string.custom_mode, mode));
        }
    }
    private void selectMode(String value, String text) {
        AutoDeviceManager.saveMode(this, currentCaller, value);
        updateCurrentPlaceholder(text);
        bottomSheetDialog.dismiss();
    }

    private void updateCurrentPlaceholder(String text) {
        switch (currentCaller) {
            case AutoDeviceManager.ABI:
                ABIMode.setText(text);
                break;

            case AutoDeviceManager.DPI:
                DPIMode.setText(text);
                break;

            case AutoDeviceManager.LANGUAGE:
                LANGUAGEMode.setText(text);
                break;
        }
    }
    private void updateTextPlaceholder(TextView title, EditText textOnSearch, String pickerOf) {
        title.setText(getString(R.string.select_picker, pickerOf));
        textOnSearch.setHint(getString(R.string.search_picker, pickerOf));
    }


    private void updateState() {

        boolean autoDetectEnabled = isAutoDetectEnabled(this);
        boolean autoConfigEnabled = isAutoConfigEnabled(this);

        autoConfig.setEnabled(autoDetectEnabled);

        boolean ruleEnabled = autoDetectEnabled && autoConfigEnabled;

        ABILinear.setAlpha(ruleEnabled ? 1f : 0.5f);
        DPILinear.setAlpha(ruleEnabled ? 1f : 0.5f);
        LANGUAGELinear.setAlpha(ruleEnabled ? 1f : 0.5f);

        gotoABI.setClickable(ruleEnabled);
        gotoABI.setFocusable(ruleEnabled);

        gotoDPI.setClickable(ruleEnabled);
        gotoDPI.setFocusable(ruleEnabled);

        gotoLANGUAGE.setClickable(ruleEnabled);
        gotoLANGUAGE.setFocusable(ruleEnabled);

    }
}
