package com.szn.merger.Utils.Processing;

import android.content.Context;

import com.szn.merger.Helper.Merger;
import com.szn.merger.PrefsManager;
import com.szn.merger.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProcessingManager {
    private static final String KEY_OUTPUT_DIRECTORY_PATH = "output_directory_path";
    private static final String KEY_FORMAT_NAME = "format_name";
    private static final String KEY_COMPRESSION_LEVEL = "compression_level";
    private static final String KEY_APPEND_TIMESTAMP = "is_append_timestamp_enabled";
    private static final String KEY_APPEND_VERSION = "is_append_version_enabled";
    private static final String KEY_LOG_TYPE = "log_type";
    private static final String KEY_EXTRACT_NATIVE_LIBS = "extract_native_libs";

    public static String getDirPath(Context context) {
        return PrefsManager.getInstance(context)
                .getString(KEY_OUTPUT_DIRECTORY_PATH, "storage/0/emulated/Download");
    }

    public static void saveDirPath(Context context, String value) {
        PrefsManager.getInstance(context)
                .saveString(KEY_OUTPUT_DIRECTORY_PATH, value);
    }

    public static String getFormatName(Context context) {
        return PrefsManager.getInstance(context)
                .getString(KEY_FORMAT_NAME, "");
    }

    public static void saveFormatName(Context context, String value) {
        if (value.endsWith(".apk")) {
            value = value.substring(0, value.length() - 4);
        }

        PrefsManager.getInstance(context)
                .saveString(KEY_FORMAT_NAME, value);
    }

    public static int getCompressionLevel(Context context) {
        return PrefsManager.getInstance(context)
                .getInt(KEY_COMPRESSION_LEVEL, 6);
    }

    public static void saveCompressionLevel(Context context, int value) {
        PrefsManager.getInstance(context)
                .saveInt(KEY_COMPRESSION_LEVEL, value);
    }

    public static boolean isAppendTimestampEnabled(Context context) {
        return PrefsManager.getInstance(context)
                .getBoolean(KEY_APPEND_TIMESTAMP, false);
    }

    public static void setAppendTimestampEnabled(Context context, boolean enabled) {
        PrefsManager.getInstance(context)
                .saveBoolean(KEY_APPEND_TIMESTAMP, enabled);
    }

    public static boolean isAppendVersionEnabled(Context context) {
        return PrefsManager.getInstance(context)
                .getBoolean(KEY_APPEND_VERSION, false);
    }

    public static void setAppendVersionEnabled(Context context, boolean enabled) {
        PrefsManager.getInstance(context)
                .saveBoolean(KEY_APPEND_VERSION, enabled);
    }

    public static String getLogType(Context context) {
        return PrefsManager.getInstance(context)
                .getString(KEY_LOG_TYPE, context.getString(R.string.log_type_default));
    }

    public static void saveLogType(Context context, String value) {
        PrefsManager.getInstance(context)
                .saveString(KEY_LOG_TYPE, value);
    }

    public static String isExtractNativeLibs(Context context) {
        return PrefsManager.getInstance(context).getString(KEY_EXTRACT_NATIVE_LIBS, "false");
    }
    public static void setExtractNativeLibs(Context context, boolean enabled) {
        PrefsManager.getInstance(context).saveString(KEY_EXTRACT_NATIVE_LIBS, String.valueOf(enabled));
    }
    public static String getPrefix(Context context) {
        String formatName = getFormatName(context);
        int index = formatName.indexOf("MyApp");

        if (index == -1) {
            return "";
        }

        return formatName.substring(0, index);
    }

    public static String getSuffix(Context context) {
        String formatName = getFormatName(context);
        int index = formatName.indexOf("MyApp");

        if (index == -1) {
            return "";
        }

        return formatName.substring(index + "MyApp".length());
    }
    public static String getTimestamp(Context context) {
        if (!isAppendTimestampEnabled(context)) return "";
        return "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }
    public static String getVersion(Context context) {
        if (!isAppendVersionEnabled(context)) return "";
        return "_" + Merger.versionName;
    }
}
