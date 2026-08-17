package com.szn.merger;

import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.szn.merger.Helper.ApkInfo;
import com.szn.merger.Utils.Adapter.APKDetailsAdapter;
import com.szn.merger.Utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends AppCompatActivity implements SAFHelper.OnFilePickedListener, AppsDialog.OnAppExtractedListener, MergeTaskManager.OnMergeCompletedListener {

private ImageButton btnPaste, btnPicker, btnDelete;
private TextView textValid, textInvalid, splitsFound, emptyText, loadingPercent, loadingTime;
private LinearLayout logContainer;
private ImageView imageValid, imageInvalid, emptyIcon;
private MaterialCardView btnMerge, btnExtract, bottomBar, inputCard, logCard, btnSettings, logBottom, shareBottom, detailsBottom, browseBottom;
private EditText editFilePath;
private TextInputLayout textInputLayout;
private File selectedInputFile;
private SAFHelper safHelper;
private AppsDialog appsDialog;
private NestedScrollView scrollView;
private StableScrollView scrollCard;
private MergeTaskManager mergeTaskManager;
private LinearProgressIndicator loadingBar;
    private Object[][] appDetails;
    private Object[][] outputDetails;
    private Object[][] securityDetails;
    private Object[][] flagsDetails;
    private Object[][] installedDetails;
    private Object[][] manifestDetails;

@Override
protected void onCreate(Bundle savedInstanceState) {
    ThemeManager.register(this);
    ThemeManager.applyTheme(this);
    ThemeManager.applyLanguage(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    initViews();
    setupListeners();

    // Set up the animation
    Utils.animateTextChange(editFilePath, inputCard, 500, text -> {

        // Reset selectedInputFile to null if the user types manually
        if (getCurrentFocus() == editFilePath) {
            selectedInputFile = null;
        }
    });

    safHelper = new SAFHelper(this, this);
    appsDialog = new AppsDialog(this, this);
    mergeTaskManager = new MergeTaskManager(this, logContainer, scrollCard, logCard, loadingBar, this, loadingPercent, loadingTime);
}

private void initViews() {
    detailsBottom = findViewById(R.id.detailsBtn);
    browseBottom = findViewById(R.id.browseBtn);
    shareBottom = findViewById(R.id.shareBtn);
    logBottom = findViewById(R.id.logBtn);
    inputCard = findViewById(R.id.inputCard);
    scrollCard = findViewById(R.id.scrollCard);
    scrollView = findViewById(R.id.mainScroll);
    textInputLayout = findViewById(R.id.textInputLayout);
    loadingTime = findViewById(R.id.loadingTime);
    loadingBar = findViewById(R.id.loadingBar);
    loadingPercent = findViewById(R.id.loadingPercent);
    emptyIcon = findViewById(R.id.emptyIcon);
    emptyText = findViewById(R.id.emptyText);
    logCard = findViewById(R.id.logCard);
    bottomBar = findViewById(R.id.bottomBar);
    btnDelete = findViewById(R.id.btnClearLog);
    logContainer = findViewById(R.id.logContainer);
    splitsFound = findViewById(R.id.splitsFound);
    textValid = findViewById(R.id.textValid);
    textInvalid = findViewById(R.id.textInvalid);
    imageValid = findViewById(R.id.imageValid);
    imageInvalid = findViewById(R.id.imageInvalid);
    btnSettings = findViewById(R.id.settingsButton);
    btnExtract = findViewById(R.id.btnExtract);
    btnMerge = findViewById(R.id.btnConvert);
    editFilePath = findViewById(R.id.editFilePath);
    btnPicker = findViewById(R.id.btnFilePicker);
    btnPaste = findViewById(R.id.btnPaste);
}

private void setupListeners() {
    detailsBottom.setOnClickListener(v -> showMergeDetails());
    browseBottom.setOnClickListener(v -> browseApk());
    shareBottom.setOnClickListener(v -> shareApk());
    logBottom.setOnClickListener(v -> copyAllLogs());
    btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    btnPicker.setOnClickListener(v -> safHelper.launchPicker());
    btnPaste.setOnClickListener((v -> {
        String copied = Utils.getUserCopyClipBoard(this);
        editFilePath.setText(copied);
        textInputLayout.setHint("");
    }));
    editFilePath.setOnFocusChangeListener((v, hasFocus) -> {
        if (hasFocus) {
            textInputLayout.setHint("");
        } else if (editFilePath.getText().length() == 0) {
            textInputLayout.setHint(R.string.paste_path);
        }
    });
    editFilePath.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            Utils.applyCardRadiusChangeShape(MainActivity.this, inputCard, R.style.Shape_RoundedTop);
            setPathInfo();
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.toString().trim().isEmpty()) {

            }
        }
    });
    btnDelete.setOnClickListener((v -> {
        MergeTaskManager.stopMerge();
        logContainer.removeAllViews();
        loadingTime.setText("0s");
        loadingBar.setProgressCompat(0, true);
        loadingPercent.setText("0%");
        emptyIcon.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.VISIBLE);
    }));

    btnMerge.setOnClickListener((v -> {
        mergeTaskManager.startMergeFlow(selectedInputFile, editFilePath);
        emptyIcon.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);
        scrollView.post(() -> {
            int startY = scrollView.getScrollY();
            int targetY = logCard.getTop()
                    - (scrollView.getHeight() - logCard.getHeight()) / 2;

            ValueAnimator animator = ValueAnimator.ofInt(startY, targetY);
            animator.setDuration(500);

            animator.addUpdateListener(animation ->
                    scrollView.scrollTo(0, (int) animation.getAnimatedValue())
            );

            animator.start();
            });
        }));
    btnExtract.setOnClickListener(v -> appsDialog.show());
    }

    private void loadDetailsData() {
        ApkInfo.init(this);

        appDetails = new Object[][]{
                {"App Name", ApkInfo.getAppName(this), false},
                {"Package Name", ApkInfo.getPackageName(), false},
                {"Version Name", ApkInfo.getVersionName(), false},
                {"Version Code", ApkInfo.getVersionCode(), false},
                {"Min SDK", ApkInfo.getMinSDK(), false},
                {"Target SDK", ApkInfo.getTargetSDK(), false}
        };

        outputDetails = new Object[][]{
                {"File Name", ApkInfo.getFileName(), false},
                {"File Path", ApkInfo.getPathFile(), false},
                {"File Size", ApkInfo.getFileSize(), false},
                {"ABI", ApkInfo.getABI(), false},
                {"DPI", ApkInfo.getDPI(), false},
                {"Language", ApkInfo.getLANGUAGE(), false},
                {"Compression Level", ApkInfo.getCompressionLevel(), false},
                {"Splits Count", ApkInfo.getSplitsCount(), false},
                {"Dex Count", ApkInfo.getDexCount(), false},
                {"Resources Merged", ApkInfo.getResourcesCount(), false}
        };
        manifestDetails = new Object[][] {
                {"Permission", ApkInfo.getPermissionCount(), true},
                {"Receivers", ApkInfo.getReceiversCount(), true},
                {"providers", ApkInfo.getProvidersCount(), true},
                {"Activities", ApkInfo.getActivitiesCount(), true},
                {"Services", ApkInfo.getServicesCount(), true}
        };

        securityDetails = new Object[][]{
                {"Signed", ApkInfo.getSigned(), false},
                {"Signing Schemes", ApkInfo.getSchemes(), false},
                {"Signed Source Stamp", ApkInfo.getSourceStampVerified(), false},
                {"Signer", ApkInfo.getSigner(), false},
                {"SHA-256", ApkInfo.getCertificateSHA256(), false},
                {"SHA-1", ApkInfo.getCertificateSHA1(), false},
                {"MD5", ApkInfo.getCertificateMD5(), false},
                {"Subject", ApkInfo.getCertificateSubject(), false},
                {"Issuer", ApkInfo.getCertificateIssuer(), false},
                {"Valid From", ApkInfo.getCertificateValidFrom(), false},
                {"Valid Until", ApkInfo.getCertificateValidUntil(), false}
        };

        flagsDetails = new Object[][]{
                {"System App", ApkInfo.getSystemApp(), false},
                {"Updated System App", ApkInfo.getUpdatedSystemApp(), false},
                {"Debuggable", ApkInfo.getDebuggable(), false},
                {"Allow Backups", ApkInfo.getAllowBackups(), false},
                {"Extract Native Libs", ApkInfo.getExtractNativeLibs(), false},
                {"Supports RTL", ApkInfo.getSupportsRTL(), false},
                {"Has Code", ApkInfo.getHasCode(), false},
                {"Large Heap", ApkInfo.getLargeHeap(), false}
        };

        installedDetails = new Object[][]{
                {"Main Activity", ApkInfo.getMainActivity(this), false},
                {"Data Path", ApkInfo.getDataPath(), false},
                {"Dir Path", ApkInfo.getDirPath(), false},
                {"Apk Path", ApkInfo.getApkPath(), false},
                {"UID", ApkInfo.getUID(), false},
                {"Installed Time", ApkInfo.getInstallTime(), false},
                {"Last Updated", ApkInfo.getLastUpdated(), false},
                {"Package Installer", ApkInfo.getPackageInstallerName(this), false}
        };
    }
    private void showFullDetailsApkTextDialog(String text, String[] items) {
        View view = getLayoutInflater().inflate(R.layout.apk_full_details_dialog, null);
        TextView placeholder = view.findViewById(R.id.placeholder);
        TextView placeholderCount = view.findViewById(R.id.placeholderCount);
        placeholder.setText(text);
        placeholderCount.setText(items.length + " " + text);
        MaterialCardView copy = view.findViewById(R.id.copyAll);

        copy.setOnClickListener(v -> {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            String allText = TextUtils.join("\n", items);
            ClipData clip = ClipData.newPlainText(text, allText);
            clipboardManager.setPrimaryClip(clip);
        });

        RecyclerView recycler = view.findViewById(R.id.contentRecycler);

        recycler.setLayoutManager(new LinearLayoutManager(this));

        recycler.setAdapter(new APKDetailsAdapter.FullDetailsAdapter(items));
        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }
    private void showMergeDetails() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        View view = getLayoutInflater().inflate(R.layout.apk_details_bottom_sheet, null);

        ImageButton close = view.findViewById(R.id.close);
        RecyclerView cardRecycler = view.findViewById(R.id.cardRecycler);

        APKDetailsAdapter adapter = new APKDetailsAdapter(title -> {
            switch (title) {
                case "Permission" :
                    showFullDetailsApkTextDialog("Permission", ApkInfo.getPermission());
                    break;
                case "Receivers" :
                    showFullDetailsApkTextDialog("Receivers", ApkInfo.getReceivers());
                    break;
                case "Providers" :
                    showFullDetailsApkTextDialog("Providers", ApkInfo.getProviders());
                    break;
                case "Activities" :
                    showFullDetailsApkTextDialog("Activities", ApkInfo.getActivities());
                    break;
                case "Services" :
                    showFullDetailsApkTextDialog("Services", ApkInfo.getServices());
                    break;

            }
        });

        adapter.addCard("App", R.drawable.ic_android, appDetails);
        adapter.addCard("Output", R.drawable.ic_file, outputDetails);
        adapter.addCard("Manifest", R.drawable.ic_install, manifestDetails);
        adapter.addCard("Security", R.drawable.ic_security, securityDetails);
        adapter.addCard("Flags", R.drawable.ic_flag, flagsDetails);
        adapter.addCard("Installed Info", R.drawable.ic_install, installedDetails);

        cardRecycler.setLayoutManager(new LinearLayoutManager(this));
        cardRecycler.setAdapter(adapter);

        close.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
    }
    private void shareApk() {
        if (MergeTaskManager.finalOutput == null || !MergeTaskManager.finalOutput.exists()) return;
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", MergeTaskManager.finalOutput);
        Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/vnd.android.package-archive");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Apk"));
    }

    private void browseApk() {
        File apk = MergeTaskManager.finalOutput;

        if (apk == null || !apk.exists()) {
            return;
        }

        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apk);

        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setDataAndType(uri, "application/*");
        String[] allowedMimeTypes = {
                "application/zip",
                "application/x-rar-compressed",
                "application/x-7z-compressed",
                "text/plain"
        };

        intent.putExtra(Intent.EXTRA_MIME_TYPES, allowedMimeTypes);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(Intent.createChooser(intent, "Browse with"));
        } catch (ActivityNotFoundException e) {
            // No compatible app
        }
    }
private void setPathInfo() {
    String path = editFilePath.getText().toString().trim();

    if (path.isEmpty()) {
        textValid.setVisibility(View.GONE);
        imageValid.setVisibility(View.GONE);
        return;
    }

    File directory = new File(path);
    boolean exist = directory.isFile();

    splitsFound.setText(getString(R.string.splits_found).replace("0", String.valueOf(getSplitCount(path))));

    if (exist) {
        textValid.setVisibility(View.VISIBLE);
        imageValid.setVisibility(View.VISIBLE);
        textInvalid.setVisibility(View.GONE);
        imageInvalid.setVisibility(View.GONE);
    } else {
        textValid.setVisibility(View.GONE);
        imageValid.setVisibility(View.GONE);
        textInvalid.setVisibility(View.VISIBLE);
        imageInvalid.setVisibility(View.VISIBLE);
    }
}
private String getAllLogs() {
    StringBuilder logs = new StringBuilder();
    for(int i = 0; i < logContainer.getChildCount(); i++) {
        View child = logContainer.getChildAt(i);
        if (child instanceof TextView) logs.append(((TextView) child).getText()).append("\n");
    }
    return logs.toString();
}
private void copyAllLogs() {
    String logs = getAllLogs();
    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    clipboardManager.setPrimaryClip(ClipData.newPlainText("logs", logs));
}
public static int getSplitCount(String path) {
    File file = new File(path);

    int count = 0;
    if (!file.isFile()) return count;

    try (ZipFile zipFile = new ZipFile(file)) {

        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (!entry.isDirectory()
                    && entry.getName().endsWith(".apk")
                    && !entry.getName().equals("base.apk")) {
                count++;
            }
        }

    } catch (IOException e) {
        e.printStackTrace();
    }
    return count;
}

@Override
public void onMergeCompleted() {
    loadDetailsData();
    Utils.applyLayoutTransition(bottomBar);
    bottomBar.setVisibility(View.VISIBLE);
}

@Override
public void onFilePicked(File file, String fileName, int splitsCount) {
    selectedInputFile = file;
    editFilePath.setText(fileName);

    splitsFound.setText(getString(R.string.splits_found).replace("0", String.valueOf(splitsCount)));
    textValid.setVisibility(View.VISIBLE);
    imageValid.setVisibility(View.VISIBLE);
    textInvalid.setVisibility(View.GONE);
    imageInvalid.setVisibility(View.GONE);
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
public void onAppExtractionSuccess(File file, String fileName, int splitCount) {
    selectedInputFile = file;
    editFilePath.setText(fileName);

    splitsFound.setVisibility(View.VISIBLE);
    splitsFound.setText(getString(R.string.splits_found).replace("0", String.valueOf(splitCount)));
    textValid.setVisibility(View.VISIBLE);
    imageValid.setVisibility(View.VISIBLE);
    textInvalid.setVisibility(View.GONE);
    imageInvalid.setVisibility(View.GONE);
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
public boolean onOptionsItemSelected(android.view.MenuItem item) {
    if (item.getItemId() == R.id.menu_settings) {
        startActivity(new android.content.Intent(this, SettingsActivity.class));
        return true;
    }
    return super.onOptionsItemSelected(item);
}
}