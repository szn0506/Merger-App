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

        Utils.animateTextChange(editFilePath, inputCard, 500, text -> {
            if (getCurrentFocus() == editFilePath) {
                selectedInputFile = null;
            }
        });

        safHelper = new SAFHelper(this, this);
        appsDialog = new AppsDialog(this, this);
        mergeTaskManager = new MergeTaskManager(this, logContainer, scrollCard, loadingBar, this, loadingPercent, loadingTime);
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
        btnPaste.setOnClickListener(v -> {
            String copied = Utils.getUserCopyClipBoard(this);
            editFilePath.setText(copied);
            textInputLayout.setHint("");
        });
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
                setPathInfo();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        btnDelete.setOnClickListener(v -> {
            MergeTaskManager.stopMerge();
            logContainer.removeAllViews();
            loadingTime.setText("0s");
            loadingBar.setProgressCompat(0, true);
            loadingPercent.setText("0%");
            emptyIcon.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.VISIBLE);
        });

        btnMerge.setOnClickListener(v -> {
            mergeTaskManager.startMergeFlow(selectedInputFile, editFilePath);
            emptyIcon.setVisibility(View.GONE);
            emptyText.setVisibility(View.GONE);
            scrollView.post(() -> {
                int startY = scrollView.getScrollY();
                int targetY = logCard.getTop() - (scrollView.getHeight() - logCard.getHeight()) / 2;

                ValueAnimator animator = ValueAnimator.ofInt(startY, targetY);
                animator.setDuration(500);

                animator.addUpdateListener(animation ->
                        scrollView.scrollTo(0, (int) animation.getAnimatedValue())
                );

                animator.start();
            });
        });

        btnExtract.setOnClickListener(v -> appsDialog.show());
    }

    private void loadDetailsData() {
        ApkInfo.init(this);

        appDetails = new Object[][]{
                {getString(R.string.app_name), ApkInfo.getAppName(this), false},
                {getString(R.string.package_name), ApkInfo.getPackageName(), false},
                {getString(R.string.version_name), ApkInfo.getVersionName(), false},
                {getString(R.string.version_code), ApkInfo.getVersionCode(), false},
                {getString(R.string.min_sdk), ApkInfo.getMinSDK(), false},
                {getString(R.string.target_sdk), ApkInfo.getTargetSDK(), false}
        };

        outputDetails = new Object[][]{
                {getString(R.string.file_name), ApkInfo.getFileName(), false},
                {getString(R.string.file_path), ApkInfo.getPathFile(), false},
                {getString(R.string.file_size), ApkInfo.getFileSize(), false},
                {getString(R.string.abi), ApkInfo.getABI(), false},
                {getString(R.string.dpi), ApkInfo.getDPI(), false},
                {getString(R.string.language), ApkInfo.getLANGUAGE(), false},
                {getString(R.string.compression_level), ApkInfo.getCompressionLevel(), false},
                {getString(R.string.splits_count), ApkInfo.getSplitsCount(), false},
                {getString(R.string.dex_count), ApkInfo.getDexCount(), false},
                {getString(R.string.resources_merged), ApkInfo.getResourcesCount(), false}
        };

        manifestDetails = new Object[][]{
                {getString(R.string.permission), ApkInfo.getPermissionCount(), true},
                {getString(R.string.receivers), ApkInfo.getReceiversCount(), true},
                {getString(R.string.providers), ApkInfo.getProvidersCount(), true},
                {getString(R.string.activities), ApkInfo.getActivitiesCount(), true},
                {getString(R.string.services), ApkInfo.getServicesCount(), true}
        };

        securityDetails = new Object[][]{
                {getString(R.string.signed), ApkInfo.getSigned(), false},
                {getString(R.string.signing_schemes), ApkInfo.getSchemes(), false},
                {getString(R.string.signed_source_stamp), ApkInfo.getSourceStampVerified(), false},
                {getString(R.string.signer), ApkInfo.getSigner(), false},
                {getString(R.string.sha256), ApkInfo.getCertificateSHA256(), false},
                {getString(R.string.sha1), ApkInfo.getCertificateSHA1(), false},
                {getString(R.string.md5), ApkInfo.getCertificateMD5(), false},
                {getString(R.string.subject), ApkInfo.getCertificateSubject(), false},
                {getString(R.string.issuer), ApkInfo.getCertificateIssuer(), false},
                {getString(R.string.valid_from), ApkInfo.getCertificateValidFrom(), false},
                {getString(R.string.valid_until), ApkInfo.getCertificateValidUntil(), false}
        };

        flagsDetails = new Object[][]{
                {getString(R.string.system_app), ApkInfo.getSystemApp(), false},
                {getString(R.string.updated_system_app), ApkInfo.getUpdatedSystemApp(), false},
                {getString(R.string.debuggable), ApkInfo.getDebuggable(), false},
                {getString(R.string.allow_backups), ApkInfo.getAllowBackups(), false},
                {getString(R.string.extract_native_libs), ApkInfo.getExtractNativeLibs(), false},
                {getString(R.string.supports_rtl), ApkInfo.getSupportsRTL(), false},
                {getString(R.string.has_code), ApkInfo.getHasCode(), false},
                {getString(R.string.large_heap), ApkInfo.getLargeHeap(), false}
        };

        installedDetails = new Object[][]{
                {getString(R.string.main_activity), ApkInfo.getMainActivity(this), false},
                {getString(R.string.data_path), ApkInfo.getDataPath(), false},
                {getString(R.string.dir_path), ApkInfo.getDirPath(), false},
                {getString(R.string.apk_path), ApkInfo.getApkPath(), false},
                {getString(R.string.uid), ApkInfo.getUID(), false},
                {getString(R.string.installed_time), ApkInfo.getInstallTime(), false},
                {getString(R.string.last_updated), ApkInfo.getLastUpdated(), false},
                {getString(R.string.package_installer), ApkInfo.getPackageInstallerName(this), false}
        };
    }

    private void showFullDetailsApkTextDialog(String text, String[] items) {
        View view = getLayoutInflater().inflate(R.layout.apk_full_details_dialog, null);
        TextView placeholder = view.findViewById(R.id.placeholder);
        TextView placeholderCount = view.findViewById(R.id.placeholderCount);
        placeholder.setText(text);
        placeholderCount.setText(getString(R.string.details_count, items.length, text));
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
            if (title.equals(getString(R.string.permission))) {
                showFullDetailsApkTextDialog(
                        getString(R.string.permission),
                        ApkInfo.getPermission()
                );

            } else if (title.equals(getString(R.string.receivers))) {
                showFullDetailsApkTextDialog(
                        getString(R.string.receivers),
                        ApkInfo.getReceivers()
                );

            } else if (title.equals(getString(R.string.providers))) {
                showFullDetailsApkTextDialog(
                        getString(R.string.providers),
                        ApkInfo.getProviders()
                );

            } else if (title.equals(getString(R.string.activities))) {
                showFullDetailsApkTextDialog(
                        getString(R.string.activities),
                        ApkInfo.getActivities()
                );

            } else if (title.equals(getString(R.string.services))) {
                showFullDetailsApkTextDialog(
                        getString(R.string.services),
                        ApkInfo.getServices()
                );
            }
        });

        adapter.addCard(getString(R.string.app), R.drawable.ic_android, appDetails);
        adapter.addCard(getString(R.string.output), R.drawable.ic_file, outputDetails);
        adapter.addCard(getString(R.string.manifest), R.drawable.ic_install, manifestDetails);
        adapter.addCard(getString(R.string.security), R.drawable.ic_security, securityDetails);
        adapter.addCard(getString(R.string.flags), R.drawable.ic_flag, flagsDetails);
        adapter.addCard(getString(R.string.installed_info), R.drawable.ic_install, installedDetails);

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

        startActivity(Intent.createChooser(intent, getString(R.string.share_apk)));
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
            startActivity(Intent.createChooser(intent, getString(R.string.browse_with)));
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

        splitsFound.setText(getString(R.string.splits_found, getSplitCount(path)));

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
        for (int i = 0; i < logContainer.getChildCount(); i++) {
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
    protected void onResume() {
        super.onResume();
        editFilePath.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                textInputLayout.setHint("");
            } else if (editFilePath.getText().length() == 0) {
                textInputLayout.setHint(R.string.paste_path);
            }
        });
    }

    @Override
    public void onMergeCompleted() {
        Utils.applyLayoutTransition(bottomBar);
        bottomBar.setVisibility(View.VISIBLE);
        loadDetailsData();
    }

    @Override
    public void onFilePicked(File file, String fileName, int splitsCount) {
        selectedInputFile = file;
        editFilePath.setText(fileName);

        splitsFound.setText(getString(R.string.splits_found, splitsCount));
        textValid.setVisibility(View.VISIBLE);
        imageValid.setVisibility(View.VISIBLE);
        textInvalid.setVisibility(View.GONE);
        imageInvalid.setVisibility(View.GONE);

        Utils.toast(this, getString(R.string.selected_file, fileName));
    }

    @Override
    public void onAppExtractionStart(String message) {
        runOnUiThread(() -> {
            editFilePath.setText(R.string.extracting);
            Utils.toast(this, message);
        });
    }

    @Override
    public void onAppExtractionSuccess(File file, String fileName, int splitCount) {
        selectedInputFile = file;
        editFilePath.setText(fileName);

        splitsFound.setVisibility(View.VISIBLE);
        splitsFound.setText(getString(R.string.splits_found, splitCount));
        textValid.setVisibility(View.VISIBLE);
        imageValid.setVisibility(View.VISIBLE);
        textInvalid.setVisibility(View.GONE);
        imageInvalid.setVisibility(View.GONE);

        Utils.toast(this, getString(R.string.successfully_extracted, fileName));
    }

    @Override
    public void onAppExtractionFailed(String errorMsg) {
        editFilePath.setText("");
        Utils.toast(this, getString(R.string.failed_to_extract, errorMsg));
    }

    @Override
    public void onError(String errorMsg) {
        Utils.toast(this, getString(R.string.failed_to_process_file, errorMsg));
    }
}