package com.szn.merger.Utils.AutoDevice;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;

import com.szn.merger.PrefsManager;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AutoDeviceManager {
    public static final int ABI = 0;
    public static final int DPI = 1;
    public static final int LANGUAGE = 2;
    private static final String KEY_AUTO_DETECT = "is_auto_detect_enabled";
    private static final String KEY_AUTO_CONFIG = "is_auto_config_enabled";
    private static final String KEY_ABI = "auto_device_abi";
    private static final String KEY_DPI = "auto_device_dpi";
    private static final String KEY_LANGUAGE = "auto_device_language";

    public static final String MODE_DISABLED = "disabled";
    public static final String MODE_FROM_DEVICE = "fromDevice";

    public static boolean isAutoDetectEnabled(Context context) {
        return PrefsManager.getInstance(context)
                .getBoolean(KEY_AUTO_DETECT, false);
    }

    public static void setAutoDetectEnabled(Context context, boolean enabled) {
        PrefsManager.getInstance(context)
                .saveBoolean(KEY_AUTO_DETECT, enabled);
    }

    public static boolean isAutoConfigEnabled(Context context) {
        return PrefsManager.getInstance(context)
                .getBoolean(KEY_AUTO_CONFIG, false);
    }

    public static void setAutoConfigEnabled(Context context, boolean enabled) {
        PrefsManager.getInstance(context)
                .saveBoolean(KEY_AUTO_CONFIG, enabled);
    }

    private static String getKey(int caller) {
        switch (caller) {
            case ABI:
                return  KEY_ABI;
            case DPI:
                return KEY_DPI;
            case LANGUAGE:
                return KEY_LANGUAGE;
            default:
                throw new IllegalArgumentException("Unkown Caller");
        }
    }
    public static void saveMode(Context context, int caller, String value) {
        PrefsManager.getInstance(context).saveString(getKey(caller), value);
    }
    public static String getMode(Context context, int caller) {
        return PrefsManager.getInstance(context).getString(getKey(caller), MODE_FROM_DEVICE);
    }

    private static boolean splitExists(String target, List<String> allEntries) {
        if (target == null || target.isEmpty()) {
            return false;
        }
        for (String entry : allEntries) {
            if (entry.toLowerCase().contains(target.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    public static boolean shouldExtract(Context context, String entryName, List<String> allEntries) {
        List<String> ARCH_FILTERS = Arrays.asList("v7a", "v8a", "x86", "arm");
        List<String> DPI_FILTERS = Arrays.asList("ldpi", "mdpi", "tvdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi", "nodpi", "anydpi");
        List<String> BASE_FILTERS = Arrays.asList("base", "master", "com");

        String name = entryName.toLowerCase();

        boolean featureDisabled = (!isAutoDetectEnabled(context));
        String selectedABI = getMode(context, ABI);
        String selectedDPI = getMode(context, DPI);
        String selectedLANGUAGE = getMode(context, LANGUAGE);

        String targetABIDevice = getArchCPU();
        String targetDPIDevice = getScreenDPIBucket(context);
        String targetLANGUAGEDevice = getDefaultLanguage();

        boolean isBaseApk = BASE_FILTERS.stream().anyMatch(name::contains);
        boolean isABIFile = ARCH_FILTERS.stream().anyMatch(name::contains);
        boolean isDpiFile = DPI_FILTERS.stream().anyMatch(name::contains);
        boolean isLanguageFile = name.contains("config") && ARCH_FILTERS.stream().noneMatch(name::contains) && DPI_FILTERS.stream().noneMatch(name::contains);

        // base apk wajib lolos dalam kondisi apapun dan kalau fitur mati atau semua mode disabled semua akan lolos
        if (isBaseApk || featureDisabled) return true;

        if (isABIFile) return searchMatchSplit(selectedABI, targetABIDevice, name, allEntries);
        if (isDpiFile) return searchMatchSplit(selectedDPI, targetDPIDevice, name, allEntries);
        if (isLanguageFile) return  searchMatchSplit(selectedLANGUAGE, targetLANGUAGEDevice, name, allEntries);

        return true;

    }
    private static boolean searchMatchSplit(String selected, String device, String name, List<String> allEntries) {
        // kalau mode from device, pakai punya device kalau custom ya pakai custom selected
        String target = MODE_FROM_DEVICE.equals(selected) ? device : selected;

        if (MODE_DISABLED.equals(selected)) return true;

        if (!splitExists(target, allEntries)) return true;

        return name.contains(target);
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

        switch (densityDPI) {
            case DisplayMetrics.DENSITY_LOW:
                return "ldpi";
            case DisplayMetrics.DENSITY_MEDIUM:
                return "mdpi";
            case DisplayMetrics.DENSITY_TV:
                return "tvdpi";
            case DisplayMetrics.DENSITY_HIGH:
                return "hdpi";
            case DisplayMetrics.DENSITY_XHIGH:
                return "xhdpi";
            case DisplayMetrics.DENSITY_XXHIGH:
                return "xxhdpi";
            case DisplayMetrics.DENSITY_XXXHIGH:
                return "xxxhdpi";
            default:
                return "nodpi";
        }
    }

    public static String getDefaultLanguage() {
        return Locale.getDefault().getLanguage();
    }
}
