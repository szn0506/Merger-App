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
        return PrefsManager.getInstance(context).getString(KEY_FALLBACK_MODE, FALLBACK_MODE_AVAILABLE);
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
        PrefsManager.getInstance(context).saveStringList(getCustomKey(caller), values);
    }

    public static List<String> getCustomModes(Context context, int caller) {
        return PrefsManager.getInstance(context).getStringList(getCustomKey(caller), new ArrayList<>());
    }

    private static boolean splitExists(String target, List<String> allEntries) {
        if (target == null || target.isEmpty()) {
            return false;
        }

        String normalizedTarget = target.toLowerCase(Locale.ROOT);

        // Check whether the requested configuration exists anywhere in the archive.
        for (String entry : allEntries) {
            if (entry.toLowerCase(Locale.ROOT).contains(normalizedTarget)) {
                return true;
            }
        }

        return false;
    }

    public static List<String> listSplits(List<String> allEntries) {
        List<String> splits = new ArrayList<>();

        // Only APK entries are shown in the manual split picker.
        for (String entry : allEntries) {
            if (entry.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                splits.add(entry);
            }
        }

        return splits;
    }

    public static void prepare(Activity activity, List<String> allEntries) {
        // Auto Detect OFF: do not apply any automatic split selection.
        if (!isAutoDetectEnabled(activity)) return;

        // Auto Config OFF: let the user manually choose the splits.
        if (!isAutoConfigEnabled(activity)) {
            AutoDeviceActivity.showSplitsPicker(activity, allEntries);
            return;
        }

        // Auto Config ON: only custom modes need to be checked here.
        for (int caller : new int[]{ABI, DPI, LANGUAGE}) {
            if (!MODE_CUSTOM.equals(getMode(activity, caller))) {
                continue;
            }

            List<String> customModes = getCustomModes(activity, caller);

            // An empty custom list means there is no custom restriction.
            if (customModes.isEmpty()) {
                continue;
            }

            boolean hasAvailableCustom = false;

            // Check whether at least one of the selected custom values exists in the APK.
            for (String custom : customModes) {
                if (splitExists(custom, allEntries)) {
                    hasAvailableCustom = true;
                    break;
                }
            }

            if (!hasAvailableCustom) {
                // None of the selected custom values exist.
                // Either fall back to the picker or allow all available splits.
                if (FALLBACK_MODE_DIALOG.equals(getFallbackMode(activity))) {
                    AutoDeviceActivity.showSplitsPicker(activity, allEntries);
                }
                return;
            }
        }
    }

    public static boolean shouldExtract(Context context, String entryName, List<String> allEntries) {
        String name = entryName.toLowerCase(Locale.ROOT);

        // When the picker has a selection, only the selected entries are extracted.
        if (!selectedSplits.isEmpty() && !selectedSplits.contains(name)) {
            return false;
        }

        List<String> ARCH_FILTERS = Arrays.asList("v7a", "v8a", "x86_64", "x86", "arm"); //NON-NLS
        List<String> DPI_FILTERS = Arrays.asList("ldpi", "mdpi", "tvdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi", "nodpi", "anydpi"); //NON-NLS
        List<String> BASE_FILTERS = Arrays.asList("base", "master", "com"); //NON-NLS

        boolean featureDisabled = !isAutoDetectEnabled(context);

        // Get the device configuration used for automatic matching.
        String targetABIDevice = getArchCPU();
        String targetDPIDevice = getScreenDPIBucket(context);
        String targetLANGUAGEDevice = getDefaultLanguage();

        boolean isBaseApk = BASE_FILTERS.stream().anyMatch(name::contains);
        boolean isABIFile = ARCH_FILTERS.stream().anyMatch(name::contains);
        boolean isDpiFile = DPI_FILTERS.stream().anyMatch(name::contains);
        boolean isLanguageFile = name.contains("config")
                && ARCH_FILTERS.stream().noneMatch(name::contains)
                && DPI_FILTERS.stream().noneMatch(name::contains);

        // Base APKs and disabled Auto Detect are always extracted.
        if (isBaseApk || featureDisabled) {
            return true;
        }

        if (isABIFile) {
            return searchMatchSplit(context, ABI, targetABIDevice, name, allEntries);
        }

        if (isDpiFile) {
            return searchMatchSplit(context, DPI, targetDPIDevice, name, allEntries);
        }

        if (isLanguageFile) {
            return searchMatchSplit(context, LANGUAGE, targetLANGUAGEDevice, name, allEntries);
        }

        // Unknown/unclassified entries are kept.
        return true;
    }

    private static boolean searchMatchSplit(Context context, int caller, String device, String name, List<String> allEntries) {
        String mode = getMode(context, caller);

        // Disabled mode does not filter this configuration type.
        if (MODE_DISABLED.equals(mode)) {
            return true;
        }

        if (MODE_FROM_DEVICE.equals(mode)) {
            // If the device configuration does not exist, keep all available splits.
            if (!splitExists(device, allEntries)) {
                return true;
            }

            // Keep only the split matching the device configuration.
            return name.contains(device.toLowerCase(Locale.ROOT));
        }

        if (MODE_CUSTOM.equals(mode)) {
            List<String> customModes = getCustomModes(context, caller);

            // No custom selection means there is no restriction.
            if (customModes.isEmpty()) {
                return true;
            }

            boolean hasAvailableCustom = false;

            for (String custom : customModes) {
                String target = custom.toLowerCase(Locale.ROOT);

                if (splitExists(target, allEntries)) {
                    hasAvailableCustom = true;

                    // Keep every available split selected by the user.
                    if (name.contains(target)) {
                        return true;
                    }
                }
            }

            if (!hasAvailableCustom) {
                // None of the selected custom configurations exist.
                // "available_splits" keeps all available splits; "show_picker"
                // is handled once in prepare() before extraction starts.
                return FALLBACK_MODE_AVAILABLE.equals(getFallbackMode(context));
            }

            // At least one custom configuration exists, so unselected
            // configurations of this type are excluded.
            return false;
        }

        return true;
    }

    public static String getArchCPU() {
        String[] supported = Build.SUPPORTED_ABIS;

        if (supported != null && supported.length > 0) {
            // Replace hyphen (-) with underscore (_) when reading the system ABI.
            return supported[0].replace("-", "_");
        }

        return "unknown";
    }

    public static String getScreenDPIBucket(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
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
        return Locale.getDefault().getLanguage();
    }
}