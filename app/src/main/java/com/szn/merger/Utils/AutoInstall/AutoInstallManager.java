package com.szn.merger.Utils.AutoInstall;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import com.szn.merger.CustomSwitchItem;
import com.szn.merger.PrefsManager;

import java.io.File;

public class AutoInstallManager {
    public static boolean waitingPermissionResult = false;
    public static boolean granted;
    private static final String KEY_AUTO_INSTALL = "KEY_AUTO_INSTALL";
    private static final String KEY_UNINSTALL_APP = "KEY_UNINSTALL_APP";
    private static final String KEY_DELETE_AFTER = "KEY_DELETE_AFTER";

    public static boolean isAutoInstallEnabled(Context context) {
        return PrefsManager.getInstance(context)
                .getBoolean(KEY_AUTO_INSTALL, false);
    }

    public static void setAutoInstallEnabled(Context context, boolean enabled) {
        PrefsManager.getInstance(context)
                .saveBoolean(KEY_AUTO_INSTALL, enabled);
    }
    public static boolean isUninstallApp(Context context) {
        return PrefsManager.getInstance(context)
                .getBoolean(KEY_UNINSTALL_APP, false);
    }
    public static void setUninstallAppEnabled(Context context, boolean enabled) {
        PrefsManager.getInstance(context)
                .saveBoolean(KEY_UNINSTALL_APP, enabled);
    }
    public static boolean isDeleteAfterEnabled(Context context) {
        return PrefsManager.getInstance(context)
                .getBoolean(KEY_DELETE_AFTER, false);
    }

    public static void setDeleteAfterEnabled(Context context, boolean enabled) {
        PrefsManager.getInstance(context)
                .saveBoolean(KEY_DELETE_AFTER, enabled);
    }
    private static void deleteFile(File file) {
        if (file.exists()) {
            file.delete();
        }
    }
    public static boolean isPackageInstalled(Context context, String packagname) {
        try {
            context.getPackageManager().getPackageInfo(packagname, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    public static void waitForUninstall(Activity activity, String packagename, Runnable onUninstalled) {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable check = new Runnable() {
            @Override
            public void run() {
                if (!isPackageInstalled(activity, packagename)) {
                    onUninstalled.run();
                } else {
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.post(check);
    }
    public static void waitForInstall(Activity activity, String packagename, Runnable onInstalled) {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable check = new Runnable() {
            @Override
            public void run() {
                if (isPackageInstalled(activity, packagename)) {
                    onInstalled.run();
                } else {
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.post(check);
    }

    public static void setupCall(Activity activity, File file, String packageName) {


        if (!isAutoInstallEnabled(activity)) {
            return;
        }

        if (isUninstallApp(activity)) {

            uninstallApp(activity, packageName);

            waitForUninstall(activity, packageName, () -> {

                installApkFile(activity, file);

                if (isDeleteAfterEnabled(activity)) {
                    waitForInstall(activity, packageName,
                            () -> deleteFile(file));
                }

            });

            return;
        }


        installApkFile(activity, file);

        if (isDeleteAfterEnabled(activity)) {
            waitForInstall(activity, packageName,
                    () -> deleteFile(file));
        }
    }

    public static void onResume(Activity activity, CustomSwitchItem switchItem) {
        if (!waitingPermissionResult) return;

        waitingPermissionResult = false;

        granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity.getPackageManager().canRequestPackageInstalls();

        setAutoInstallEnabled(activity, granted);

        switchItem.setChecked(granted);
    }

    public static boolean checkAndAskPermission(Context context) {

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
    public static void uninstallApp(Activity activity, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + packageName));
            activity.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
