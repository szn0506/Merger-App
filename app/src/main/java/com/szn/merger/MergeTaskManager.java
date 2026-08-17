package com.szn.merger;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.reandroid.apkeditor.merge.MergerOptions;
import com.szn.merger.Helper.Merger;
import com.szn.merger.Utils.AutoInstall.AutoInstallManager;
import com.szn.merger.Utils.Processing.ProcessingManager;
import com.szn.merger.Utils.Signing.SigningManager;
import com.szn.merger.Utils.Utils;

import java.io.File;
import java.io.IOException;

public class MergeTaskManager {

    private final Activity activity;
    private final TextView loadingPercent;
    private ValueAnimator bottomAnimator;
    private final TextView loadingTime;
    private final StableScrollView scrollCard;
    private final ViewGroup logCard;
    private final ViewGroup logContainer;
    private final LinearProgressIndicator loadingBar;
    private final OnMergeCompletedListener listener;
    private static final Handler timerHandler = new Handler(Looper.getMainLooper());

    private boolean userTouchingScroll;
    private boolean bottomLocked = true;
    private int touchSlop;
    private int currentProgress;
    private long startTime;
    private static Runnable timerRunnable;
    public static File finalOutput;

    public MergeTaskManager(Activity activity, ViewGroup logContainer, StableScrollView scrollCard, ViewGroup logCard, LinearProgressIndicator loadingBar, OnMergeCompletedListener listener, TextView loadingPercent, TextView loadingTime) {
        this.activity = activity;
        this.logContainer = logContainer;
        this.scrollCard = scrollCard;
        this.logCard = logCard;
        this.loadingBar = loadingBar;
        this.listener = listener;
        this.loadingPercent = loadingPercent;
        this.loadingTime = loadingTime;
        touchSlop = ViewConfiguration.get(activity).getScaledTouchSlop();
        setupScrollLock();
    }

    private void setupScrollLock() {
        scrollCard.setOnTouchListener(new View.OnTouchListener() {
            private float downY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        userTouchingScroll = true;
                        downY = event.getY();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getY() - downY) > touchSlop)
                            bottomLocked = false;
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        userTouchingScroll = false;
                        if (isAtBottom()) {
                            bottomLocked = true;
                            forceBottom();
                        } else {
                            bottomLocked = false;
                        }
                        break;
                }

                return false;
            }
        });

        scrollCard.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!bottomLocked || userTouchingScroll) return;
            forceBottom();
        });

        scrollCard.post(() -> {
            bottomLocked = true;
            forceBottom();
        });
    }

    private boolean isAtBottom() {
        View child = scrollCard.getChildAt(0);
        if (child == null) return true;

        int maxScroll = Math.max(0, child.getHeight() - scrollCard.getHeight());
        return scrollCard.getScrollY() >= maxScroll - touchSlop;
    }

    private void forceBottom() {
        if (!bottomLocked || userTouchingScroll) return;

        View child = scrollCard.getChildAt(0);
        if (child == null) return;

        int maxScroll = Math.max(
                0,
                child.getHeight() - scrollCard.getHeight()
        );

        int currentY = scrollCard.getScrollY();

        if (currentY == maxScroll) return;

        if (bottomAnimator != null) {
            bottomAnimator.cancel();
        }

        bottomAnimator = ValueAnimator.ofInt(currentY, maxScroll);
        bottomAnimator.setDuration(180);

        bottomAnimator.addUpdateListener(animation -> {
            if (!bottomLocked || userTouchingScroll) {
                animation.cancel();
                return;
            }

            scrollCard.scrollTo(
                    0,
                    (int) animation.getAnimatedValue()
            );
        });

        bottomAnimator.start();
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                long totalSeconds = elapsed / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;

                loadingTime.setText(String.format("%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        };

        timerHandler.post(timerRunnable);
    }

    private static void stopTimer() {
        if (timerRunnable == null) return;

        timerHandler.removeCallbacks(timerRunnable);
        timerRunnable = null;
    }

    private void updateProgress(String msg) {
        String log = msg.trim();
        int progress;

        if (log.startsWith("Extracting to:")) progress = 5;
        else if (log.startsWith("Searching apk")) progress = 10;
        else if (log.startsWith("Found apk")) progress = 15;
        else if (log.startsWith("Found modules:")) progress = 20;
        else if (log.startsWith("Merging:")) progress = 40;
        else if (log.startsWith("Added [")) progress = 50;
        else if (log.startsWith("Sanitizing manifest")) progress = 60;
        else if (log.startsWith("Removed-attribute")) progress = 65;
        else if (log.startsWith("Removed-element")) progress = 70;
        else if (log.startsWith("Removed-table-entry")) progress = 75;
        else if (log.startsWith("Applying:")) progress = 80;
        else if (log.startsWith("Writing APK")) progress = 85;
        else if (log.startsWith("Buffering compress")) progress = 88;
        else if (log.startsWith("Writing files:")) progress = 93;
        else if (log.startsWith("Writing signature")) progress = 97;
        else if (log.startsWith("Saved to:")) progress = 100;
        else return;

        animateProgress(progress);
    }

    private void animateProgress(int target) {
        if (target <= currentProgress) return;

        currentProgress = target;
        loadingPercent.setText(currentProgress + "%");
        loadingBar.setProgressCompat(currentProgress, true);
    }

    public void startMergeFlow(File selectedInputFile, EditText editFilePath) {
        currentProgress = 0;
        Utils.hideKeyboard(activity);

        File inputFile;

        if (selectedInputFile != null && selectedInputFile.exists()) {
            inputFile = selectedInputFile;
        } else {
            if (!Utils.hasStoragePermission(activity)) {
                Utils.requestStoragePermission(activity);
                return;
            }

            String path = editFilePath.getText().toString().trim();

            if (path.isEmpty()) {
                Utils.toast(activity, "Path field is empty!");
                return;
            }

            inputFile = new File(path);
        }

        String outputDir = ProcessingManager.getDirPath(activity);
        File tempOutput = new File(outputDir, ".temp_merged.apk");

        runMerge(inputFile, tempOutput);
    }

    public static void stopMerge() {
        Merger.stopMerge();
        stopTimer();
    }

    public interface OnMergeCompletedListener {
        void onMergeCompleted();
    }

    private void addLogView(String msg) {
        TextView logView = new TextView(activity);
        logView.setText(msg.trim());
        logView.setTextSize(13);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setPadding(20, 0, 20, 0);
        logView.setTextIsSelectable(true);
        logView.setAlpha(0f);
        logContainer.addView(logView);

        logView.animate()
                .alpha(1f)
                .setDuration(250)
                .start();

        logView.post(() -> {
            if (!bottomLocked || userTouchingScroll) return;
            forceBottom();
        });
    }

    private void runMerge(File input, File tempOutput) {
        startTimer();

        new Thread(() -> {
            try {
                MergerOptions options = new MergerOptions();
                options.inputFile = input;
                options.outputFile = tempOutput;
                options.extractNativeLibs = ProcessingManager.isExtractNativeLibs(activity);

                activity.runOnUiThread(() -> {
                    bottomLocked = true;
                    userTouchingScroll = false;
                    logContainer.removeAllViews();
                    scrollCard.resetScrollbarSize();
                    scrollCard.scrollTo(0, 0);
                });

                Merger merger = new Merger(
                        activity,
                        options
                ) {
                    @Override
                    protected void onLog(String msg) {
                        if (Merger.stopped) return;
                        activity.runOnUiThread(() -> {
                            if (Merger.stopped) return;
                            addLogView(msg);
                            updateProgress(msg);
                        });

                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ignored) {
                        }
                    }
                };

                merger.setEnableLog(true);
                merger.runCommand();

                stopTimer();

                String inputFileName = input.getName();
                int lastDot = inputFileName.lastIndexOf(".");
                String baseName = lastDot != -1 ? inputFileName.substring(0, lastDot) : inputFileName;

                String outputName = ProcessingManager.getPrefix(activity)
                        + baseName
                        + ProcessingManager.getSuffix(activity)
                        + ProcessingManager.getVersion(activity)
                        + ProcessingManager.getTimestamp(activity)
                        + ".apk";

                finalOutput = new File(
                        ProcessingManager.getDirPath(activity),
                        outputName
                );

                if (finalOutput.exists() && !finalOutput.delete())
                    throw new IOException("Failed to delete existing output: " + finalOutput);

                if (!tempOutput.renameTo(finalOutput))
                    throw new IOException("Failed to rename merged APK to: " + finalOutput.getAbsolutePath());

                merger.logSavedFile(finalOutput);

                SigningManager.signApk(activity, finalOutput);
                String packageName = Merger.packageName;

                activity.runOnUiThread(() -> {
                    scrollCard.postDelayed(() -> {
                        Utils.toast(activity, "Success: " + finalOutput.getName());
                        AutoInstallManager.setupCall(activity, finalOutput, packageName);

                        if (listener != null)
                            listener.onMergeCompleted();
                    }, 300);
                });

            } catch (Exception e) {
                if (tempOutput.exists())
                    tempOutput.delete();

                activity.runOnUiThread(() -> {
                    if (e.getMessage() != null && !"Merge stopped".equals(e.getMessage()))
                        Utils.toast(activity, "Error: " + e.getMessage());
                });
            }
        }).start();
    }
}