package com.szn.merger;

import android.app.Activity;
import android.os.Environment;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;

import com.reandroid.apkeditor.merge.MergerOptions;
import com.szn.merger.Helper.Merger;
import com.szn.merger.Utils.AutoInstall.AutoInstallManager;
import com.szn.merger.Utils.Signing.SigningManager;
import com.szn.merger.Utils.Utils;

import java.io.File;

public class MergeTaskManager {

    private final Activity activity;
    private final TextView logText;
    private final NestedScrollView scrollCard;
    private final ViewGroup logCard;

    private boolean isManualScroll = false;
    private final boolean[] isDoneWrapper = {false};

    public MergeTaskManager(Activity activity, TextView logText, NestedScrollView scrollCard, ViewGroup logCard) {
        this.activity = activity;
        this.logText = logText;
        this.scrollCard = scrollCard;
        this.logCard = logCard;

        setupScrollTouchListener();
    }

    private void setupScrollTouchListener() {
        scrollCard.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                case android.view.MotionEvent.ACTION_MOVE:
                    isManualScroll = true;
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    scrollCard.postDelayed(() -> isManualScroll = false, 1000);
                    break;
            }
            return false;
        });
    }

    public void startMergeFlow(File selectedInputFile, EditText editFilePath) {
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

        String inputFileName = inputFile.getName();
        int lastDot = inputFileName.lastIndexOf(".");
        String outputName = (lastDot != -1) ? inputFileName.substring(0, lastDot) + "_merged.apk" : inputFileName + "_merged.apk";

        File inputDir = inputFile.getParentFile();
        if (inputDir == null) {
            inputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        }

        File outputFile = new File(inputDir, outputName);
        runMerge(inputFile, outputFile);
    }

    private void runMerge(File input, File output) {
        isManualScroll = false;

        new Thread(() -> {
            try {
                MergerOptions options = new MergerOptions();
                options.inputFile = input;
                options.outputFile = output;

                StringBuilder logBuffer = new StringBuilder();
                activity.runOnUiThread(() -> logText.setText(""));

                Merger merger = new Merger(activity, options) {
                    @Override
                    public void logMessage(String msg) {
                        // FOR UI LOG UPDATE PURPOSES ONLY
                        activity.runOnUiThread(() -> {
                            logBuffer.append(msg).append("\n");
                            TransitionManager.beginDelayedTransition(logCard, new AutoTransition());
                            logText.setText(logBuffer.toString());

                            // Auto-scroll continues running as incoming logs are processed
                            scrollCard.postOnAnimation(new Runnable() {
                                @Override
                                public void run() {
                                    if (!isManualScroll) {
                                        scrollCard.fullScroll(View.FOCUS_DOWN);
                                        scrollCard.postOnAnimation(this);
                                    }
                                }
                            });
                        });
                    }
                };

                merger.setEnableLog(true);

                // 1. Run the merge process (This thread blocks here until COMPLETELY FINISHED)
                merger.runCommand();

                // 2. THE FOLLOWING CODE WILL ONLY EXECUTE ONCE THE APKS MERGE IS COMPLETED
                activity.runOnUiThread(() -> {
                    // Stop scroll animations and bring it back to the top post-process completion
                    if (scrollCard.getHandler() != null) {
                        scrollCard.getHandler().removeCallbacksAndMessages(null);
                    }
                    scrollCard.fullScroll(View.FOCUS_UP);
                });

                // 3. Run the heavy signing process on this background thread
                File finalOutput = SigningManager.signApk(activity, output);
                String packageName = Merger.packageName;
                // 4. Once signing is complete, trigger the installation on the UI Thread
                activity.runOnUiThread(() -> {
                    scrollCard.postDelayed(() -> {

                        Utils.toast(activity, "Success: " + finalOutput.getName());
                        AutoInstallManager.setupCall(activity, finalOutput, packageName);
                    }, 300);
                });

            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() ->
                        Utils.toast(activity, "Error: " + e.getMessage())
                );
            }
        }).start();
    }
}