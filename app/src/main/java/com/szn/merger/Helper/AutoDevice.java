package com.szn.merger.Helper;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;

import com.szn.merger.CustomSwitchItem;
import com.szn.merger.PrefsManager;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AutoDevice {

    public static final String KEY_AUTO_DETECT = "is_auto_detect_enabled";
    public static final String KEY_ARCH = "device_arch";
    public static final String KEY_DPI = "device_dpi";
    public static final String KEY_LANGUAGE = "device_language";

    public static void setupAutoDetectSwitch(Context context, CustomSwitchItem switchItem) {
        PrefsManager prefs = PrefsManager.getInstance(context);
        boolean isEnabled = prefs.getBoolean(KEY_AUTO_DETECT, false);
        switchItem.setChecked(isEnabled);

        switchItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.saveBoolean(KEY_AUTO_DETECT, isChecked);

            if (isChecked) {
                prefs.saveString(KEY_ARCH, getArchCPU());
                prefs.saveString(KEY_DPI, getScreenDPIBucket(context));
                prefs.saveString(KEY_LANGUAGE, getDefaultLanguage());
            } else {
                prefs.remove(KEY_ARCH);
                prefs.remove(KEY_DPI);
                prefs.remove(KEY_LANGUAGE);
            }
        });
    }

    private static boolean splitExists(String target, List<String> allEntries) {
        if (target == null || target.isEmpty()) {
            return false;
        }
        for (String entry : allEntries) {
            if (entry.contains(target)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldExtract(Context context, String entryName, List<String> allEntries) {
        PrefsManager prefs = PrefsManager.getInstance(context);

        boolean autoDetect = prefs.getBoolean(KEY_AUTO_DETECT, false);
        String targetArch = prefs.getString(KEY_ARCH, "").toLowerCase();
        String targetDpi = prefs.getString(KEY_DPI, "").toLowerCase();
        String targetLang = prefs.getString(KEY_LANGUAGE, "").toLowerCase();

        List<String> ARCH_FILTERS = Arrays.asList("v7a", "v8a", "x86", "arm");
        List<String> DPI_FILTERS = Arrays.asList(".mdpi", ".hdpi", ".xhdpi", ".xxhdpi", ".xxxhdpi");
        List<String> BASE_FILTERS = Arrays.asList("base", "master", "com");

        String name = entryName.toLowerCase();

        boolean isBaseApk = BASE_FILTERS.stream().anyMatch(name::contains);
        boolean isArchFile = ARCH_FILTERS.stream().anyMatch(name::contains);
        boolean isDpiFile = DPI_FILTERS.stream().anyMatch(name::contains);

        boolean isLanguageFile = name.contains("config")
                && ARCH_FILTERS.stream().noneMatch(name::contains)
                && DPI_FILTERS.stream().noneMatch(name::contains);

        if (isBaseApk) return true;
        if (!autoDetect) return true;

        if (isArchFile) {
            if (targetArch.isEmpty()) return true;
            if (!splitExists(targetArch, allEntries)) return true;
            return name.contains(targetArch);
        }

        if (isDpiFile) {
            if (targetDpi.isEmpty()) return true;
            if (!splitExists(targetDpi, allEntries)) return true;
            return name.contains(targetDpi);
        }

        if (isLanguageFile) {
            if (targetLang.isEmpty()) return true;
            if (!splitExists(targetLang, allEntries)) return true;
            return name.contains(targetLang);
        }

        return true;
    }

    public static String getArchCPU() {
        String[] supported = Build.SUPPORTED_ABIS;
        if (supported != null && supported.length > 0) {
            // Directly replace hyphen (-) with underscore (_) when read from the system
            return supported[0].replace("-", "_");
        }
        return "unknown";
    }

    public static String getScreenDPIBucket(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int densityDPI = displayMetrics.densityDpi;

        if (densityDPI <= DisplayMetrics.DENSITY_MEDIUM) return ".mdpi";
        if (densityDPI <= DisplayMetrics.DENSITY_HIGH) return ".hdpi";
        if (densityDPI <= DisplayMetrics.DENSITY_XHIGH) return ".xhdpi";
        if (densityDPI <= DisplayMetrics.DENSITY_XXHIGH) return ".xxhdpi";

        return ".xxxhdpi";
    }

    public static String getDefaultLanguage() {
        return "." + Locale.getDefault().getLanguage();
    }
}