package com.szn.merger.Helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import com.szn.merger.CustomSwitchItem;
import com.szn.merger.PrefsManager;

import java.io.File;

public class AutoInstalation {

        public static String KEY_AUTO_INSTALL = "auto_installation_enabled";
        public static boolean waitingPermissionResult = false;

        public static void setupAutoDetectSwitch(Context context, CustomSwitchItem switchItem) {
            PrefsManager prefs = PrefsManager.getInstance(context);
            boolean isEnabled = prefs.getBoolean(KEY_AUTO_INSTALL, false);
            switchItem.setChecked(isEnabled);

            switchItem.setOnCheckedChangeListener((buttonView, isChecked) -> {

                if (isChecked && !checkAndAskPermission(context)) {
                    switchItem.setChecked(false);
                    prefs.saveBoolean(KEY_AUTO_INSTALL, false);
                    return;
                }

                prefs.saveBoolean(KEY_AUTO_INSTALL, isChecked);
            });
        }

        public static void onResume(Activity activity, CustomSwitchItem switchItem) {
            if (!waitingPermissionResult) return;

            waitingPermissionResult = false;

            boolean granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity.getPackageManager().canRequestPackageInstalls();
            PrefsManager prefs = PrefsManager.getInstance(activity);
            prefs.saveBoolean(KEY_AUTO_INSTALL, granted);

            switchItem.setChecked(granted);
        }

        private static boolean checkAndAskPermission(Context context) {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return true;
            }

            if (!(context instanceof Activity)) {
                return false;
            }

            Activity activity = (Activity) context;

            if (activity.getPackageManager().canRequestPackageInstalls()) {
                return true;
            }

            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName())
            );

            waitingPermissionResult = true;
            activity.startActivity(intent);

            return false;
        }

        public static void installApkFile(Activity activity, File apkFile) {
            PrefsManager prefs = PrefsManager.getInstance(activity);

            if (!prefs.getBoolean(KEY_AUTO_INSTALL, false)) return;

            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                Uri uri;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                    uri = FileProvider.getUriForFile(
                            activity,
                            activity.getPackageName() + ".fileprovider",
                            apkFile
                    );

                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                } else {

                    uri = Uri.fromFile(apkFile);
                }

                intent.setDataAndType(
                        uri,
                        "application/vnd.android.package-archive"
                );

                activity.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
