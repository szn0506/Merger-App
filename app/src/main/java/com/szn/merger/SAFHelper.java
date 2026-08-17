package com.szn.merger;

import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SAFHelper {

    public interface OnFilePickedListener {
        void onFilePicked(File file, String fileName, int splitsCount);
        void onError(String errorMsg);
    }

    private final AppCompatActivity activity;
    private final OnFilePickedListener listener;
    private final ActivityResultLauncher<String[]> filePickerLauncher;

    public SAFHelper(AppCompatActivity activity, OnFilePickedListener listener) {
        this.activity = activity;
        this.listener = listener;

        // Initialize the launcher inside the constructor (safe because it is called during onCreate)
        this.filePickerLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), uri -> {
                    if (uri != null) {
                        handleUri(uri);
                    }
                }
        );
    }

    public void launchPicker() {
        filePickerLauncher.launch(new String[]{"*/*"});
    }

    public void handleUri(Uri uri) {
        try {
            DocumentFile documentFile = DocumentFile.fromSingleUri(activity, uri);
            String fileName = (documentFile != null) ? documentFile.getName() : "temp_file.apks";

            File tempFile = new File(activity.getCacheDir(), fileName);
            try (InputStream is = activity.getContentResolver().openInputStream(uri);
                 FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            }
            int splitCount = getSplitCount(tempFile);

            if (listener != null) {
                listener.onFilePicked(tempFile, fileName, splitCount);
            }
        } catch (Exception e) {
            if (listener != null) {
                listener.onError(e.getMessage());
            }
        }
    }

    private int getSplitCount(File apksFile) {
        int count = 0;

        try (ZipFile zipFile = new ZipFile(apksFile)) {
            java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (!entry.isDirectory()
                        && entry.getName().toLowerCase().endsWith(".apk")
                        && !entry.getName().equalsIgnoreCase("base.apk")) {
                    count++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }
}