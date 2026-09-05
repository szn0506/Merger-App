package com.szn.merger.Utils.AutoDevice;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;

import com.szn.merger.PrefsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AutoDeviceManager {
    public static final List<String> selectedSplits = new ArrayList<>();
    public static final int ABI = 0;
    public static final int DPI = 1;
    public static final int LANGUAGE = 2;

    public static final List<String> ARCH_FILTERS = Arrays.asList(
            "armeabi",
            "armeabi_v7a",
            "arm64_v8a",
            "x86",
            "x86_64",
            "mips",
            "mips64"
    ); //NON-NLS

    public static final List<String> DPI_FILTERS = Arrays.asList(
            "ldpi",
            "mdpi",
            "tvdpi",
            "hdpi",
            "xhdpi",
            "xxhdpi",
            "xxxhdpi",
            "nodpi",
            "anydpi"
    ); //NON-NLS

    public static final List<String> BASE_FILTERS = Arrays.asList(
            "base",
            "master",
            "com"
    ); //NON-NLS

    private static final String KEY_AUTO_DETECT = "is_auto_detect_enabled"; //NON-NLS
    private static final String KEY_AUTO_CONFIG = "is_auto_config_enabled"; //NON-NLS
    private static final String KEY_ABI = "auto_device_abi";
    private static final String KEY_DPI = "auto_device_dpi";
    private static final String KEY_LANGUAGE = "auto_device_language";
    private static final String KEY_FALLBACK_MODE = "fallback_mode";

    public static final String FALLBACK_MODE_AVAILABLE = "available_splits";
    public static final String FALLBACK_MODE_DIALOG = "show_picker";

    private static final String KEY_ABI_CUSTOM = "auto_device_abi_custom";
    private static final String KEY_DPI_CUSTOM = "auto_device_dpi_custom";
    private static final String KEY_LANGUAGE_CUSTOM = "auto_device_language_custom";

    public static final String MODE_DISABLED = "disabled"; //NON-NLS
    public static final String MODE_FROM_DEVICE = "fromDevice"; //NON-NLS
    public static final String MODE_CUSTOM = "custom"; //NON-NLS

    public static boolean isAutoDetectEnabled(Context context) {
        return PrefsManager.getInstance(context).getBoolean(KEY_AUTO_DETECT, false);
    }

    public static void setAutoDetectEnabled(Context context, boolean enabled) {
        PrefsManager.getInstance(context).saveBoolean(KEY_AUTO_DETECT, enabled);
    }

    public static boolean isAutoConfigEnabled(Context context) {
        return PrefsManager.getInstance(context).getBoolean(KEY_AUTO_CONFIG, false);
    }

    public static void setAutoConfigEnabled(Context context, boolean enabled) {
        PrefsManager.getInstance(context).saveBoolean(KEY_AUTO_CONFIG, enabled);
    }

    private static String getKey(int caller) {
        switch (caller) {
            case ABI:
                return KEY_ABI;
            case DPI:
                return KEY_DPI;
            case LANGUAGE:
                return KEY_LANGUAGE;
            default:
                throw new IllegalArgumentException("Unknown Caller");
        }
    }

    public static void saveMode(Context context, int caller, String value) {
        PrefsManager.getInstance(context).saveString(getKey(caller), value);
    }

    public static String getMode(Context context, int caller) {
        return PrefsManager.getInstance(context).getString(getKey(caller), MODE_FROM_DEVICE);
    }

    public static void saveFallbackMode(Context context, String value) {
        PrefsManager.getInstance(context).saveString(KEY_FALLBACK_MODE, value);
    }

    public static String getFallbackMode(Context context) {
        return PrefsManager.getInstance(context).getString(
                KEY_FALLBACK_MODE,
                FALLBACK_MODE_AVAILABLE
        );
    }

    private static String getCustomKey(int caller) {
        switch (caller) {
            case ABI:
                return KEY_ABI_CUSTOM;
            case DPI:
                return KEY_DPI_CUSTOM;
            case LANGUAGE:
                return KEY_LANGUAGE_CUSTOM;
            default:
                throw new IllegalArgumentException("Unknown Caller");
        }
    }

    public static void saveCustomModes(Context context, int caller, List<String> values) {
        List<String> normalizedValues = new ArrayList<>();

        for (String value : values) {
            normalizedValues.add(normalize(value));
        }

        PrefsManager.getInstance(context).saveStringList(
                getCustomKey(caller),
                normalizedValues
        );
    }

    public static List<String> getCustomModes(Context context, int caller) {
        List<String> values = PrefsManager.getInstance(context).getStringList(
                getCustomKey(caller),
                new ArrayList<>()
        );

        List<String> normalizedValues = new ArrayList<>();

        for (String value : values) {
            normalizedValues.add(normalize(value));
        }

        return normalizedValues;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .toLowerCase(Locale.ROOT)
                .replace("-", "_");
    }

    private static boolean splitExists(String target, List<String> allEntries) {
        if (target == null || target.isEmpty()) {
            return false;
        }

        String normalizedTarget = normalize(target);

        for (String entry : allEntries) {
            String normalizedEntry = normalize(entry);

            if (normalizedEntry.contains(normalizedTarget)) {
                return true;
            }
        }

        return false;
    }

    public static List<String> listSplits(List<String> allEntries) {
        List<String> splits = new ArrayList<>();

        for (String entry : allEntries) {
            if (entry.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                splits.add(entry);
            }
        }

        return splits;
    }

    public static void prepare(Activity activity, List<String> allEntries) {
        if (!isAutoDetectEnabled(activity)) {
            return;
        }

        if (!isAutoConfigEnabled(activity)) {
            AutoDeviceActivity.showSplitsPicker(activity, allEntries);
            return;
        }

        for (int caller : new int[]{ABI, DPI, LANGUAGE}) {
            if (!MODE_CUSTOM.equals(getMode(activity, caller))) {
                continue;
            }

            List<String> customModes = getCustomModes(activity, caller);

            if (customModes.isEmpty()) {
                continue;
            }

            boolean hasAvailableCustom = false;

            for (String custom : customModes) {
                if (splitExists(custom, allEntries)) {
                    hasAvailableCustom = true;
                    break;
                }
            }

            if (!hasAvailableCustom) {
                if (FALLBACK_MODE_DIALOG.equals(getFallbackMode(activity))) {
                    AutoDeviceActivity.showSplitsPicker(activity, allEntries);
                }

                return;
            }
        }
    }

    public static boolean shouldExtract(Context context, String entryName, List<String> allEntries) {
        String name = normalize(entryName);

        if (!selectedSplits.isEmpty()
                && !selectedSplits.contains(name)) {
            return false;
        }

        boolean featureDisabled = !isAutoDetectEnabled(context);

        String targetABIDevice = getArchCPU();
        String targetDPIDevice = getScreenDPIBucket(context);
        String targetLANGUAGEDevice = getDefaultLanguage();

        boolean isBaseApk = BASE_FILTERS.stream().anyMatch(name::contains);
        boolean isABIFile = ARCH_FILTERS.stream().anyMatch(name::contains);
        boolean isDpiFile = DPI_FILTERS.stream().anyMatch(name::contains);
        boolean isLanguageFile = name.contains("config")
                && ARCH_FILTERS.stream().noneMatch(name::contains)
                && DPI_FILTERS.stream().noneMatch(name::contains);

        if (isBaseApk || featureDisabled) {
            return true;
        }

        if (isABIFile) {
            return searchMatchSplit(
                    context,
                    ABI,
                    targetABIDevice,
                    name,
                    allEntries
            );
        }

        if (isDpiFile) {
            return searchMatchSplit(
                    context,
                    DPI,
                    targetDPIDevice,
                    name,
                    allEntries
            );
        }

        if (isLanguageFile) {
            return searchMatchSplit(
                    context,
                    LANGUAGE,
                    targetLANGUAGEDevice,
                    name,
                    allEntries
            );
        }

        return true;
    }

    private static boolean searchMatchSplit(
            Context context,
            int caller,
            String device,
            String name,
            List<String> allEntries
    ) {
        String mode = getMode(context, caller);

        if (MODE_DISABLED.equals(mode)) {
            return true;
        }

        if (MODE_FROM_DEVICE.equals(mode)) {
            String normalizedDevice = normalize(device);

            if (!splitExists(normalizedDevice, allEntries)) {
                return true;
            }

            return name.contains(normalizedDevice);
        }

        if (MODE_CUSTOM.equals(mode)) {
            List<String> customModes = getCustomModes(context, caller);

            if (customModes.isEmpty()) {
                return true;
            }

            boolean hasAvailableCustom = false;

            for (String custom : customModes) {
                String target = normalize(custom);

                if (splitExists(target, allEntries)) {
                    hasAvailableCustom = true;

                    if (name.contains(target)) {
                        return true;
                    }
                }
            }

            if (!hasAvailableCustom) {
                return FALLBACK_MODE_AVAILABLE.equals(
                        getFallbackMode(context)
                );
            }

            return false;
        }

        return true;
    }

    public static String getArchCPU() {
        String[] supported = Build.SUPPORTED_ABIS;

        if (supported != null && supported.length > 0) {
            return normalize(supported[0]);
        }

        return "unknown";
    }

    public static String getScreenDPIBucket(Context context) {
        DisplayMetrics displayMetrics =
                context.getResources().getDisplayMetrics();

        int densityDPI = displayMetrics.densityDpi; //NON-NLS

        switch (densityDPI) {
            case DisplayMetrics.DENSITY_LOW:
                return "ldpi"; //NON-NLS
            case DisplayMetrics.DENSITY_MEDIUM:
                return "mdpi"; //NON-NLS
            case DisplayMetrics.DENSITY_TV:
                return "tvdpi"; //NON-NLS
            case DisplayMetrics.DENSITY_HIGH:
                return "hdpi"; //NON-NLS
            case DisplayMetrics.DENSITY_XHIGH:
                return "xhdpi"; //NON-NLS
            case DisplayMetrics.DENSITY_XXHIGH:
                return "xxhdpi"; //NON-NLS
            case DisplayMetrics.DENSITY_XXXHIGH:
                return "xxxhdpi"; //NON-NLS
            default:
                return "nodpi"; //NON-NLS
        }
    }

    public static String getDefaultLanguage() {
        return normalize(Locale.getDefault().getLanguage());
    }
}