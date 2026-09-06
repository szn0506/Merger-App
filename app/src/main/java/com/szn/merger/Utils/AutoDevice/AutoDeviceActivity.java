package com.szn.merger.Utils.AutoDevice;

import static com.szn.merger.Utils.AutoDevice.AutoDeviceManager.isAutoConfigEnabled;
import static com.szn.merger.Utils.AutoDevice.AutoDeviceManager.isAutoDetectEnabled;
import static com.szn.merger.Utils.AutoDevice.AutoDeviceManager.listSplits;
import static com.szn.merger.Utils.AutoDevice.AutoDeviceManager.selectedSplits;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
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
import com.szn.merger.Utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class AutoDeviceActivity extends AppCompatActivity {
    BottomSheetDialog bottomSheetDialog;
    private RecyclerView recyclerCustom;
    ImageView availableSplitsCheck, dialogSelectorCheck;
    TextView title, ABIMode, DPIMode, LANGUAGEMode, customPlaceholder, currentFallbackOption;
    EditText textOnSearch;
    ImageButton backButton, nextButton;
    MaterialToolbar toolbar;
    View universalPage, customPage, fallbackDialog;
    public static int currentCaller;
    private CheckBoxAdapter customAdapter;
    MaterialRadioButton disabledRadio, fromDeviceRadio, customRadio;
    CustomSwitchItem autoDetect, autoConfig;
    LinearLayout ABILinear, DPILinear, LANGUAGELinear;
    MaterialCardView gotoABI, gotoDPI, gotoLANGUAGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        ThemeManager.applyLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auto_device_layout);

        initViews();
        loadState();
        setupListener();
        updateState();
    }

    private void initViews() {
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
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
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

            case AutoDeviceManager.MODE_CUSTOM:
                List<String> customModes = AutoDeviceManager.getCustomModes(this, caller);

                if (customModes.isEmpty()) {
                    AutoDeviceManager.saveMode(this, caller, AutoDeviceManager.MODE_DISABLED);
                    placeholder.setText(R.string.mode_disabled);
                } else {
                    placeholder.setText(getString(R.string.custom_mode, android.text.TextUtils.join(", ", customModes)));
                }
                break;

            default:
                placeholder.setText(getString(R.string.custom_mode, mode));
                break;
        }
    }

    private void setupListener() {
        autoDetect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AutoDeviceManager.setAutoDetectEnabled(this, isChecked);
            updateState();
        });

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

    private void showFallbackPopup(View anchor) {
        View availableSplit = fallbackDialog.findViewById(R.id.availableSplit);
        View splitPicker = fallbackDialog.findViewById(R.id.split_picker);

        PopupWindow popupWindow = new PopupWindow(fallbackDialog, (int) (280 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(10 * getResources().getDisplayMetrics().density);
        popupWindow.showAsDropDown(anchor, 0, 4, Gravity.END);

        availableSplit.setOnClickListener(v -> {
            AutoDeviceManager.saveFallbackMode(this, AutoDeviceManager.FALLBACK_MODE_AVAILABLE);
            currentFallbackOption.setText(R.string.use_available_split);
            availableSplitsCheck.setVisibility(View.VISIBLE);
            dialogSelectorCheck.setVisibility(View.GONE);
            popupWindow.dismiss();
        });

        splitPicker.setOnClickListener(v -> {
            AutoDeviceManager.saveFallbackMode(this, AutoDeviceManager.FALLBACK_MODE_DIALOG);
            currentFallbackOption.setText(R.string.show_split_selector);
            dialogSelectorCheck.setVisibility(View.VISIBLE);
            availableSplitsCheck.setVisibility(View.GONE);
            popupWindow.dismiss();
        });
    }

    private void showUniversalBottomSheet() {
        bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.auto_device_bottom_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        MaterialCardView disabledCard = bottomSheetView.findViewById(R.id.disabledCard),
                fromDeviceCard = bottomSheetView.findViewById(R.id.fromDeviceCard),
                customCard = bottomSheetView.findViewById(R.id.customCard),
                fallbackCard = bottomSheetView.findViewById(R.id.fallback);

        currentFallbackOption = bottomSheetView.findViewById(R.id.currentFallbackOption);
        fallbackCard.setOnClickListener(anchor -> showFallbackPopup(anchor));

        universalPage = bottomSheetView.findViewById(R.id.universalPage);
        customPage = bottomSheetView.findViewById(R.id.customPage);

        title = bottomSheetView.findViewById(R.id.text_placeholder);
        textOnSearch = bottomSheetView.findViewById(R.id.search_placeholder);
        backButton = bottomSheetView.findViewById(R.id.backButton);
        nextButton = bottomSheetView.findViewById(R.id.nextCustom);
        recyclerCustom = bottomSheetView.findViewById(R.id.recyclerCustom);
        customPlaceholder = bottomSheetView.findViewById(R.id.customSubtitle);
        customRadio = bottomSheetView.findViewById(R.id.radioCustom);
        disabledRadio = bottomSheetView.findViewById(R.id.radioDisabled);
        fromDeviceRadio = bottomSheetView.findViewById(R.id.radioFromdevice);

        nextButton.setOnClickListener(v -> customCard.performClick());
        customRadio.setOnClickListener(v -> customCard.performClick());
        disabledRadio.setOnClickListener(v -> disabledCard.performClick());
        fromDeviceRadio.setOnClickListener(v -> fromDeviceCard.performClick());

        restoreRadioState();
        bottomSheetDialog.show();

        disabledCard.setOnClickListener(v -> {
            disabledRadio.setChecked(true);
            fromDeviceRadio.setChecked(false);
            customRadio.setChecked(false);
            selectMode(AutoDeviceManager.MODE_DISABLED, getString(R.string.mode_disabled));
        });

        fromDeviceCard.setOnClickListener(v -> {
            disabledRadio.setChecked(false);
            fromDeviceRadio.setChecked(true);
            customRadio.setChecked(false);
            selectMode(AutoDeviceManager.MODE_FROM_DEVICE, getString(R.string.mode_from_device));
        });

        customCard.setOnClickListener(v -> showCustomPage());

        bottomSheetDialog.setOnDismissListener(dialog -> {
            if (customPage != null && customPage.getVisibility() == View.VISIBLE && customAdapter != null) {
                List<String> customModes = getStorageValues(customAdapter.getCheckedItems());
                updateCustomUI(customModes);
            }
        });
    }

    public static void showSplitsPicker(Activity activity, List<String> allEntries) {
        AutoDeviceManager.selectedSplits.clear();
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

            CheckBoxAdapter adapter = new CheckBoxAdapter(splits, (position, value, selectedCount1) -> {
                selectedCount.setText(activity.getString(R.string.selected_count, selectedCount1, total));
            });

            adapter.setCheckedItems(AutoDeviceManager.BASE_FILTERS);
            adapter.setDisabled(AutoDeviceManager.BASE_FILTERS);
            selectedCount.setText(activity.getString(R.string.selected_count, adapter.getSelectedCount(), adapter.getItemCount()));

            selectAll.setOnClickListener(view1 -> {
                adapter.toggleSelectAll();
                selectAll.setText(R.string.deselect_all);
            });
            recyclerView.setAdapter(adapter);

            AlertDialog dialog = new AlertDialog.Builder(activity).setView(view).create();

            button.setOnClickListener(v -> {
                selectedSplits.clear();
                selectedSplits.addAll(adapter.getCheckedItems());
                dialog.dismiss();
                latch.countDown();
            });

            dialog.show();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
                int dialogWidth = (int) (metrics.widthPixels * 0.92f);
                int dialogHeight = (int) (metrics.heightPixels * 0.8f);

                dialog.getWindow().setLayout(
                        dialogWidth,
                        dialogHeight
                );
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void updateCustomUI(List<String> customModes) {
        if (customModes.isEmpty()) {
            AutoDeviceManager.saveCustomModes(this, currentCaller, customModes);

            disabledRadio.setChecked(true);
            fromDeviceRadio.setChecked(false);
            customRadio.setChecked(false);
            customPlaceholder.setText(R.string.choose_the_setting_manually);

            AutoDeviceManager.saveMode(this, currentCaller, AutoDeviceManager.MODE_DISABLED);
            updateCurrentPlaceholder(getString(R.string.mode_disabled));
            return;
        }

        AutoDeviceManager.saveCustomModes(this, currentCaller, customModes);
        AutoDeviceManager.saveMode(this, currentCaller, AutoDeviceManager.MODE_CUSTOM);

        String text = getString(R.string.custom_mode, android.text.TextUtils.join(", ", customModes));

        disabledRadio.setChecked(false);
        fromDeviceRadio.setChecked(false);
        customRadio.setChecked(true);
        customPlaceholder.setText(text);
        updateCurrentPlaceholder(text);
    }

    private List<String> getStorageValues(List<String> checkedItems) {
        List<String> values = new ArrayList<>();

        for (String item : checkedItems) {
            if (currentCaller == AutoDeviceManager.LANGUAGE) {
                int start = item.lastIndexOf('(');
                int end = item.lastIndexOf(')');

                if (start != -1 && end > start) {
                    values.add(item.substring(start + 1, end));
                }
            } else {
                values.add(item);
            }
        }

        return values;
    }

    private List<String> getDisplayValues(List<String> savedValues, String[] displayList) {
        if (currentCaller != AutoDeviceManager.LANGUAGE) {
            return savedValues;
        }

        List<String> displayValues = new ArrayList<>();

        for (String savedValue : savedValues) {
            for (String displayValue : displayList) {
                int start = displayValue.lastIndexOf('(');
                int end = displayValue.lastIndexOf(')');

                if (start != -1 && end > start) {
                    String code = displayValue.substring(start + 1, end);

                    if (code.equals(savedValue)) {
                        displayValues.add(displayValue);
                        break;
                    }
                }
            }
        }

        return displayValues;
    }

    private void showCustomPage() {
        fallbackDialog = getLayoutInflater().inflate(R.layout.fallback_dropdown, null);
        availableSplitsCheck = fallbackDialog.findViewById(R.id.check_available_splits);
        dialogSelectorCheck = fallbackDialog.findViewById(R.id.check_split_selector);
        boolean isUseAvailableSplits = AutoDeviceManager.getFallbackMode(this).equals(AutoDeviceManager.FALLBACK_MODE_AVAILABLE);
        if (isUseAvailableSplits) {
            currentFallbackOption.setText(R.string.use_available_split);
            availableSplitsCheck.setVisibility(View.VISIBLE);
            dialogSelectorCheck.setVisibility(View.GONE);
        } else {
            currentFallbackOption.setText(R.string.show_split_selector);
            dialogSelectorCheck.setVisibility(View.VISIBLE);
            availableSplitsCheck.setVisibility(View.GONE);
        }
        currentFallbackOption.setText(AutoDeviceManager.getFallbackMode(this).equals(AutoDeviceManager.FALLBACK_MODE_AVAILABLE) ? R.string.use_available_split : R.string.show_split_selector);

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
                break;
        }

        final CheckBoxAdapter[] adapterRef = new CheckBoxAdapter[1];

        customAdapter = new CheckBoxAdapter(Arrays.asList(list), (position, value, selectedCount) -> {
            List<String> customModes = getStorageValues(adapterRef[0].getCheckedItems());
            AutoDeviceManager.saveCustomModes(this, currentCaller, customModes);
        });

        adapterRef[0] = customAdapter;

        List<String> savedCustomModes = AutoDeviceManager.getCustomModes(this, currentCaller);
        customAdapter.setCheckedItems(getDisplayValues(savedCustomModes, list));

        recyclerCustom.setAdapter(customAdapter);

        Utils.animateTextChange(textOnSearch, recyclerCustom, 150, customAdapter::filter);

        backButton.setOnClickListener(v -> {
            List<String> customModes = getStorageValues(customAdapter.getCheckedItems());
            updateCustomUI(customModes);
            universalPage.setVisibility(View.VISIBLE);
            customPage.setVisibility(View.GONE);
        });
    }

    private void restoreRadioState() {
        disabledRadio.setChecked(false);
        fromDeviceRadio.setChecked(false);
        customRadio.setChecked(false);
        customPlaceholder.setText(R.string.choose_the_setting_manually);

        String mode = AutoDeviceManager.getMode(this, currentCaller);

        if (mode.equals(AutoDeviceManager.MODE_DISABLED)) {
            disabledRadio.setChecked(true);
        } else if (mode.equals(AutoDeviceManager.MODE_FROM_DEVICE)) {
            fromDeviceRadio.setChecked(true);
        } else if (mode.equals(AutoDeviceManager.MODE_CUSTOM)) {
            List<String> customModes = AutoDeviceManager.getCustomModes(this, currentCaller);

            if (!customModes.isEmpty()) {
                customRadio.setChecked(true);
                customPlaceholder.setText(getString(R.string.custom_mode, android.text.TextUtils.join(", ", customModes)));
            }
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

        ABILinear.setClickable(ruleEnabled);
        DPILinear.setClickable(ruleEnabled);
        LANGUAGELinear.setClickable(ruleEnabled);

        ABILinear.setFocusable(ruleEnabled);
        DPILinear.setFocusable(ruleEnabled);
        LANGUAGELinear.setFocusable(ruleEnabled);

        gotoABI.setClickable(ruleEnabled);
        gotoABI.setFocusable(ruleEnabled);
        gotoDPI.setClickable(ruleEnabled);
        gotoDPI.setFocusable(ruleEnabled);
        gotoLANGUAGE.setClickable(ruleEnabled);
        gotoLANGUAGE.setFocusable(ruleEnabled);
    }
}