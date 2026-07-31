package com.szn.merger;

import android.app.Activity;
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
import com.szn.merger.Utils.Processing.ProcessingManager;
import com.szn.merger.Utils.Signing.SigningManager;
import com.szn.merger.Utils.Utils;

import java.io.File;
import java.io.IOException;

public class MergeTaskManager {

    private final Activity activity;
    private final TextView logText;
    private final NestedScrollView scrollCard;
    private final ViewGroup logCard;
    private boolean isManualScroll = false;
    public MergeTaskManager(
            Activity activity,
            TextView logText,
            NestedScrollView scrollCard,
            ViewGroup logCard
    ) {
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
                    scrollCard.postDelayed(
                            () -> isManualScroll = false,
                            1000
                    );
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

        /*
         * The APK version is only available after merger.runCommand()
         * because ProcessingManager.getVersion(...) is called inside
         * Merger after the merged module has been created.
         *
         * Therefore, the final output filename cannot be created here.
         *
         * Use a temporary file for the merge output instead.
         */
        String outputDir = ProcessingManager.getDirPath(activity);

        File tempOutput = new File(outputDir, ".temp_merged.apk");

        runMerge(inputFile, tempOutput);
    }

    private void runMerge(File input, File tempOutput) {
        isManualScroll = false;
        new Thread(() -> {
            try {
                MergerOptions options = new MergerOptions();
                options.inputFile = input;
                options.outputFile = tempOutput;
                // FEATURE
                options.extractNativeLibs = ProcessingManager.isExtractNativeLibs(activity);

                StringBuilder logBuffer = new StringBuilder();

                activity.runOnUiThread(() -> logText.setText(""));

                Merger merger = new Merger(activity, options, ProcessingManager.getLogType(activity)) {
                    @Override
                    protected void onLog(String msg) {
                        activity.runOnUiThread(() -> {
                            logBuffer.append(msg).append("\n");

                            TransitionManager.beginDelayedTransition(logCard, new AutoTransition());

                            logText.setText(logBuffer.toString());

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

                        // DELAY PER LOG DITAROH DISINI (Misal 200ms per baris)
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ignored) {
                        }
                    }
                };

                merger.setEnableLog(true);

                /*
                 * MERGE
                 *
                 * Inside runCommand():
                 *
                 * 1. mergedModule is created
                 * 2. ProcessingManager.getVersion(...)
                 * 3. The merged APK is written to tempOutput
                 */
                merger.runCommand();

                /*
                 * At this point:
                 *
                 * Merger.versionName
                 * is already available.
                 *
                 * Now we can create the final output filename.
                 */

                String inputFileName = input.getName();

                int lastDot = inputFileName.lastIndexOf(".");

                String baseName = (lastDot != -1) ? inputFileName.substring(0, lastDot) : inputFileName;

                String outputName = ProcessingManager.getPrefix(activity) + baseName + ProcessingManager.getSuffix(activity) + ProcessingManager.getVersion(activity) + ProcessingManager.getTimestamp(activity) + ".apk";
                File finalOutput = new File(ProcessingManager.getDirPath(activity), outputName);

                /*
                 * Delete the final output if it already exists.
                 */
                if (finalOutput.exists()) {
                    if (!finalOutput.delete()) {
                        throw new IOException(
                                "Failed to delete existing output: "
                                        + finalOutput
                        );
                    }
                }

                /*
                 * Rename the temporary merged APK
                 * to the final output filename.
                 */
                if (!tempOutput.renameTo(finalOutput)) {
                    throw new IOException(
                            "Failed to rename merged APK to: "
                                    + finalOutput.getAbsolutePath()
                    );
                }

                // WE CALL IT FROM HERE BECAUSE MERGER CLASS DOESN'T KNOW THE FINAL FILE
                merger.logSavedFile(finalOutput);

                /*
                 * Stop auto-scroll after the merge is complete.
                 */
                activity.runOnUiThread(() -> {

                    if (scrollCard.getHandler() != null) {
                        scrollCard
                                .getHandler()
                                .removeCallbacksAndMessages(null);
                    }

                    scrollCard.fullScroll(
                            View.FOCUS_UP
                    );
                });

                /*
                 * SIGN
                 *
                 * Signing now uses the final output file
                 * with the correct version in its filename.
                 */
                File signedOutput = SigningManager.signApk(activity, finalOutput);

                String packageName =
                        Merger.packageName;

                /*
                 * INSTALL
                 */
                activity.runOnUiThread(() -> {

                    scrollCard.postDelayed(
                            () -> {

                                Utils.toast(
                                        activity,
                                        "Success: "
                                                + signedOutput.getName()
                                );

                                AutoInstallManager.setupCall(activity, signedOutput, packageName);
                            }, 300
                    );
                });

            } catch (Exception e) {

                e.printStackTrace();

                if (tempOutput.exists()) {
                    tempOutput.delete();
                }

                activity.runOnUiThread(
                        () -> Utils.toast(activity, "Error: " + e.getMessage())
                );
            }

        }).start();
    }
}