package com.szn.merger.Utils.Processing;

import android.content.Context;

import com.szn.merger.Helper.Merger;
import com.szn.merger.PrefsManager;
import com.szn.merger.R;
import com.szn.merger.Utils.Adapter.FormatNameReorderAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProcessingManager {
    private static final String KEY_OUTPUT_DIRECTORY_PATH = "output_directory_path";
    private static final String KEY_OUTPUT_DIRECTORY_URI = "output_directory_uri";
    private static final String KEY_USE_URI_DIRECTORY = "use_uri_directory";
    private static final String KEY_FORMAT_NAME = "format_name"; //NON-NLS
    private static final String KEY_COMPRESSION_LEVEL = "compression_level"; //NON-NLS
    private static final String KEY_KEEP_ORIGINAL_NAME = "is_keep_original_file_name";
    static final String KEY_APPEND_PACKAGENAME = "is_append_package_name";
    private static final String KEY_APPEND_VERSIONNAME = "is_append_version_enabled"; //NON-NLS
    private static final String KEY_APPEND_VERSIONCODE = "is_append_version_code";
    private static final String KEY_APPEND_ABI = "is_append_abi";
    private static final String KEY_APPEND_DPI = "is_append_dpi";
    private static final String KEY_APPEND_LANGUAGE = "is_append_language";
    private static final String KEY_APPEND_SIGNINGSTATUS = "is_append_signing_status";
    private static final String KEY_APPEND_SIGNINGSCHEMES = "is_append_signing_schemes";
    private static final String KEY_APPEND_TIMESTAMP = "is_append_timestamp";
    private static final String KEY_APPEND_SDKVERSIONS = "is_append_sdk_versions";
    private static final String KEY_EXTRACT_NATIVE_LIBS = "extract_native_libs";
    private static final String KEY_PREFIX = "prefix";
    private static final String KEY_SUFFIX = "suffix";
    private static final String KEY_SAVE_TO_ORIGINAL_PATH = "save_to_original_path";
    private static final String KEY_FORMAT_ORDER = "format_name_order";

    public static String getDirPath(Context context) {
        return PrefsManager.getInstance(context).getString(KEY_OUTPUT_DIRECTORY_PATH, ""); //NON-NLS
    }

    public static void saveDirPath(Context context, String value) {
        PrefsManager.getInstance(context).saveString(KEY_OUTPUT_DIRECTORY_PATH, value);
    }

    public static boolean isUsingUriDir(Context context) {
        return PrefsManager.getInstance(context).getBoolean(KEY_USE_URI_DIRECTORY, false);
    }

    public static void setUsingUriDir(Context context, boolean value) {
        PrefsManager.getInstance(context).saveBoolean(KEY_USE_URI_DIRECTORY, value);
    }
    public static String getDirUri(Context context) {
        return PrefsManager.getInstance(context).getString(KEY_OUTPUT_DIRECTORY_URI, ""); //NON-NLS
    }

    public static void saveDirUri(Context context, String value) {
        PrefsManager.getInstance(context).saveString(KEY_OUTPUT_DIRECTORY_URI, value);
    }

    public static String isExtractNativeLibs(Context context) {
        return PrefsManager.getInstance(context).getString(KEY_EXTRACT_NATIVE_LIBS, "false"); //NON-NLS
    }

    public static void setExtractNativeLibs(Context context, boolean enabled) {
        PrefsManager.getInstance(context).saveString(KEY_EXTRACT_NATIVE_LIBS, String.valueOf(enabled));
    }
    public static String getFormatName(Context context) {
        return PrefsManager.getInstance(context).getString(KEY_FORMAT_NAME, "MyApp.apk");
    }

    public static void saveFormatName(Context context, String value) {
        PrefsManager.getInstance(context).saveString(KEY_FORMAT_NAME, value);
    }

    public static int getCompressionLevel(Context context) {
        return PrefsManager.getInstance(context)
                .getInt(KEY_COMPRESSION_LEVEL, 6);
    }

    public static void saveCompressionLevel(Context context, int value) {
        PrefsManager.getInstance(context)
                .saveInt(KEY_COMPRESSION_LEVEL, value);
    }

    public static boolean isAppendVersionNameEnabled(Context context) {
        return PrefsManager.getInstance(context)
                .getBoolean(KEY_APPEND_VERSIONNAME, false);
    }

    public static void setAppendVersionName(Context context, boolean enabled) {
        PrefsManager.getInstance(context)
                .saveBoolean(KEY_APPEND_VERSIONNAME, enabled);
    }
    public static boolean isAppendVersionCodeEnabled(Context context) {
        return PrefsManager.getInstance(context).getBoolean(KEY_APPEND_VERSIONCODE, false);
    }
    public static void setAppendVersioncode(Context context, boolean enabled) {
        PrefsManager.getInstance(context).saveBoolean(KEY_APPEND_VERSIONCODE, enabled);
    }
    public static boolean isAppendPackageNameEnabled(Context context) {
        return PrefsManager.getInstance(context).getBoolean(KEY_APPEND_PACKAGENAME, false);
    }
    public static void setAppendPackageName(Context context, boolean enabled) {
        PrefsManager.getInstance(context).saveBoolean(KEY_APPEND_PACKAGENAME, enabled);
    }
    public static boolean isAppendABIEnabled(Context context) {
        return PrefsManager.getInstance(context).getBoolean(KEY_APPEND_ABI, false);
    }
    public static void setAppendABI(Context context, boolean enabled) {
        PrefsManager.getInstance(context).saveBoolean(KEY_APPEND_ABI, enabled);
    }

    public static boolean isAppendDPIEnabled(Context context) {
        return PrefsManager.getInstance(context).getBoolean(KEY_APPEND_DPI, false);
    }

    public static void setAppendDPI(Context context, boolean enabled) {
        PrefsManager.getInstance(context).saveBoolean(KEY_APPEND_DPI, enabled);
    }

    public static boolean isAppendLanguageEnabled(Context context) {
        return PrefsManager.getInstance(context).getBoolean(KEY_APPEND_LANGUAGE, false);
    }

    public static void setAppendLanguage(Context context, boolean enabled) {
        PrefsManager.getInstance(context).saveBoolean(KEY_APPEND_LANGUAGE, enabled);
    }

    public static boolean isAppendSigningStatusEnabled(Context context) {
        return PrefsManager.getInstance(context).getBoolean(KEY_APPEND_SIGNINGSTATUS, false);
    }

    public static void setAppendSigningStatus(Context context, boolean enabled) {
        PrefsManager.getInstance(context).saveBoolean(KEY_APPEND_SIGNINGSTATUS, enabled);
    }

    public static boolean isAppendSigningSchemesEnabled(Context context) {
        return PrefsManager.getInstance(context).getBoolean(KEY_APPEND_SIGNINGSCHEMES, false);
    }

    public static void setAppendSigningSchemes(Context context, boolean enabled) {
        PrefsManager.getInstance(context).saveBoolean(KEY_APPEND_SIGNINGSCHEMES, enabled);
    }

    public static boolean isAppendTimestampEnabled(Context context) {
        return PrefsManager.getInstance(context).getBoolean(KEY_APPEND_TIMESTAMP, false);
    }

    public static void setAppendTimestamp(Context context, boolean enabled) {
        PrefsManager.getInstance(context).saveBoolean(KEY_APPEND_TIMESTAMP, enabled);
    }

    public static boolean isAppendSDKVersionsEnabled(Context context) {
        return PrefsManager.getInstance(context).getBoolean(KEY_APPEND_SDKVERSIONS, false);
    }

    public static void setAppendSDKVersions(Context context, boolean enabled) {
        PrefsManager.getInstance(context).saveBoolean(KEY_APPEND_SDKVERSIONS, enabled);
    }

    public static boolean isKeepOriginalNameEnabled(Context context) {
        return PrefsManager.getInstance(context).getBoolean(KEY_KEEP_ORIGINAL_NAME, true);
    }

    public static void setKeepOriginalName(Context context, boolean enabled) {
        PrefsManager.getInstance(context).saveBoolean(KEY_KEEP_ORIGINAL_NAME, enabled);
    }

    public static List<String> getFormatOrder(Context context) {
        return PrefsManager.getInstance(context).getStringList(KEY_FORMAT_ORDER, FormatNameReorderAdapter.DEFAULT_FORMAT_ORDER);
    }
    public static void setFormatOrder(Context context, List<String> order) {
        PrefsManager.getInstance(context).saveStringList(KEY_FORMAT_ORDER, order);
    }

    public static boolean isSaveToOriginalPathEnabled(Context context) {
        return PrefsManager.getInstance(context).getBoolean(KEY_SAVE_TO_ORIGINAL_PATH, true);
    }

    public static void setSaveToOriginalPath(Context context, boolean enabled) {
        PrefsManager.getInstance(context).saveBoolean(KEY_SAVE_TO_ORIGINAL_PATH, enabled);
    }
    public static String getPrefix(Context context) {
        return PrefsManager.getInstance(context).getString(KEY_PREFIX, "");
    }
    public static void setPrefix(Context context, String prefix) {
        PrefsManager.getInstance(context).saveString(KEY_PREFIX, prefix);
    }

    public static String getSuffix(Context context) {
        return PrefsManager.getInstance(context).getString(KEY_SUFFIX, "");
    }

    public static void setSuffix(Context context, String suffix) {
        PrefsManager.getInstance(context).saveString(KEY_SUFFIX, suffix);
    }

    public static String getPackageName(Context context) {
        return isAppendPackageNameEnabled(context) ? "_" + Merger.packageName : "";
    }
    public static String getVersionName(Context context) {
        return isAppendVersionNameEnabled(context) ? "_" + Merger.versionName : "";
    }
    public static String getVersionCode(Context context) {
        return isAppendVersionCodeEnabled(context) ? "_" + Merger.versionCode : "";
    }
    public static String getDPI(Context context) {
        return isAppendDPIEnabled(context) ? "_" + Merger.DPI : "";
    }
    public static String getABI(Context context) {
        return isAppendABIEnabled(context) ? "_" + Merger.ABI : "";
    }
    public static String getLanguage(Context context) {
        return isAppendLanguageEnabled(context) ? "_" + Merger.LANGUAGE : "";
    }
    public static String getSigningStatus(Context context) {
        return isAppendSigningStatusEnabled(context) ? "_" + (Merger.signed ? R.string.signed : R.string.not_signed) : "";
    }
    public static String getSigningSchemes(Context context) {
        return isAppendSigningSchemesEnabled(context) ? "_" + Merger.signingSchemes : "";
    }

    public static String getTimestamp(Context context) {
        return isAppendTimestampEnabled(context) ? "_" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(new Date()) : ""; //NON-NLS
    }
    public static String getSDKVersions(Context context) {
        return isAppendSDKVersionsEnabled(context) ? "_" + Merger.sdkVersion : "";
    }

    public static String getFinalOutputName(Context context, String basename) {
        StringBuilder result = new StringBuilder();

        result.append(getPrefix(context))
                .append("_");

        if (isKeepOriginalNameEnabled(context)) result.append(basename);


        result.append("_").append(getSuffix(context));

        for (String item : getFormatOrder(context)) {
            switch (item) {
                case "Package Name":
                    result.append(getPackageName(context));
                    break;

                case "Version Name":
                    result.append(getVersionName(context));
                    break;

                case "Version Code":
                    result.append(getVersionCode(context));
                    break;

                case "ABI":
                    result.append(getABI(context));
                    break;

                case "DPI":
                    result.append(getDPI(context));
                    break;

                case "Language":
                    result.append(getLanguage(context));
                    break;

                case "Signing Status":
                    result.append(getSigningStatus(context));
                    break;

                case "Signing Schemes":
                    result.append(getSigningSchemes(context));
                    break;

                case "Timestamp":
                    result.append(getTimestamp(context));
                    break;

                case "SDK Versions":
                    result.append(getSDKVersions(context));
                    break;
            }
        }

        return result.append(".apk").toString();
    }

}
