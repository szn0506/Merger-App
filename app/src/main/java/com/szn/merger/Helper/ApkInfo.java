package com.szn.merger.Helper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.android.apksig.ApkVerifier;
import com.szn.merger.MergeTaskManager;
import com.szn.merger.R;

import java.io.File;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class ApkInfo {

    private static File OUTPUT = MergeTaskManager.finalOutput;

    private static Context CONTEXT;

    // === FILE APP STATE ===
    private static PackageInfo INFO;
    private static ApkVerifier.Result SIGNATURE_INFO;

    // === INSTALLED APP STATE ===
    private static PackageInfo INSTALLED_INFO;

    // === INITIALIZATION ===

    public static void init(Context context) {
        CONTEXT = context.getApplicationContext();

        PackageManager pm = CONTEXT.getPackageManager();

        int flags =
                PackageManager.GET_META_DATA
                        | PackageManager.GET_PERMISSIONS
                        | PackageManager.GET_ACTIVITIES
                        | PackageManager.GET_RECEIVERS
                        | PackageManager.GET_SERVICES
                        | PackageManager.GET_PROVIDERS;

        INFO = pm.getPackageArchiveInfo(
                OUTPUT.getAbsolutePath(),
                flags
        );

        try {
            INSTALLED_INFO = pm.getPackageInfo(
                    INFO.packageName,
                    PackageManager.GET_META_DATA
            );

            ApkVerifier verifier = new ApkVerifier.Builder(OUTPUT).build();
            SIGNATURE_INFO = verifier.verify();

        } catch (Exception e) {
            INSTALLED_INFO = null;
            SIGNATURE_INFO = null;
        }
    }

    // === STRING RESOURCES ===

    private static String getString(int resId) {
        return CONTEXT.getString(resId);
    }

    // === MERGE PROCESS INFO ===
    public static String getABI() {
        return Merger.ABI;
    }

    public static String getDPI() {
        return Merger.DPI;
    }

    public static String getLANGUAGE() {
        return Merger.LANGUAGE;
    }

    public static String getSplitsCount() {
        return String.valueOf(Merger.splitCount);
    }

    public static String getDexCount() {
        return String.valueOf(Merger.dexCount);
    }

    public static String getResourcesCount() {
        return String.valueOf(Merger.resourcesCount);
    }

    public static String getCompressionLevel() {
        return String.valueOf(Merger.compressionLevel);
    }

    // === APK FILE INFO ===

    public static String getAppName(Context context) {
        return INFO.applicationInfo
                .loadLabel(context.getPackageManager())
                .toString();
    }

    public static String getPackageName() {
        return INFO.packageName;
    }

    public static String getFileName() {
        return OUTPUT.getName();
    }

    public static String getPathFile() {
        return OUTPUT.getAbsolutePath();
    }

    public static String getFileSize() {
        return Merger.formatFileSize();
    }

    public static String getVersionCode() {
        return String.valueOf(INFO.getLongVersionCode());
    }

    public static String getVersionName() {
        return INFO.versionName;
    }

    public static String getMinSDK() {
        return String.valueOf(INFO.applicationInfo.minSdkVersion);
    }

    public static String getTargetSDK() {
        return String.valueOf(INFO.applicationInfo.targetSdkVersion);
    }

    // === MANIFEST COMPONENTS ===

    public static String getPermissionCount() {
        return String.valueOf(
                INFO.requestedPermissions == null
                        ? 0
                        : INFO.requestedPermissions.length
        );
    }

    public static String getReceiversCount() {
        return String.valueOf(
                INFO.receivers == null
                        ? 0
                        : INFO.receivers.length
        );
    }

    public static String getActivitiesCount() {
        return String.valueOf(
                INFO.activities == null
                        ? 0
                        : INFO.activities.length
        );
    }

    public static String getProvidersCount() {
        return String.valueOf(
                INFO.providers == null
                        ? 0
                        : INFO.providers.length
        );
    }

    public static String getServicesCount() {
        return String.valueOf(
                INFO.services == null
                        ? 0
                        : INFO.services.length
        );
    }

    public static String[] getPermission() {
        return INFO.requestedPermissions != null
                ? INFO.requestedPermissions
                : new String[0];
    }

    public static String[] getReceivers() {
        if (INFO.receivers == null) {
            return new String[0];
        }

        String[] result = new String[INFO.receivers.length];

        for (int i = 0; i < INFO.receivers.length; i++) {
            result[i] = INFO.receivers[i].name;
        }

        return result;
    }

    public static String[] getActivities() {
        if (INFO.activities == null) {
            return new String[0];
        }

        String[] result = new String[INFO.activities.length];

        for (int i = 0; i < INFO.activities.length; i++) {
            result[i] = INFO.activities[i].name;
        }

        return result;
    }

    public static String[] getServices() {
        if (INFO.services == null) {
            return new String[0];
        }

        String[] result = new String[INFO.services.length];

        for (int i = 0; i < INFO.services.length; i++) {
            result[i] = INFO.services[i].name;
        }

        return result;
    }

    public static String[] getProviders() {
        if (INFO.providers == null) {
            return new String[0];
        }

        String[] result = new String[INFO.providers.length];

        for (int i = 0; i < INFO.providers.length; i++) {
            result[i] = INFO.providers[i].name;
        }

        return result;
    }

    // === SIGNATURE INFO ===

    public static String getSigned() {
        return SIGNATURE_INFO != null && SIGNATURE_INFO.isVerified()
                ? getString(R.string.yes)
                : getString(R.string.no);
    }

    public static String getSchemes() {
        if (SIGNATURE_INFO == null || !SIGNATURE_INFO.isVerified()) {
            return getString(R.string.not_signed);
        }

        List<String> schemes = new ArrayList<>();

        if (SIGNATURE_INFO.isVerifiedUsingV1Scheme()) {
            schemes.add(getString(R.string.signature_scheme_v1));
        }

        if (SIGNATURE_INFO.isVerifiedUsingV2Scheme()) {
            schemes.add(getString(R.string.signature_scheme_v2));
        }

        if (SIGNATURE_INFO.isVerifiedUsingV3Scheme()) {
            schemes.add(getString(R.string.signature_scheme_v3));
        }

        if (SIGNATURE_INFO.isVerifiedUsingV31Scheme()) {
            schemes.add(getString(R.string.signature_scheme_v31));
        }

        if (SIGNATURE_INFO.isVerifiedUsingV4Scheme()) {
            schemes.add(getString(R.string.signature_scheme_v4));
        }

        return schemes.isEmpty()
                ? getString(R.string.not_signed)
                : String.join(", ", schemes);
    }

    public static String getSourceStampVerified() {
        if (SIGNATURE_INFO == null || !SIGNATURE_INFO.isVerified()) {
            return getString(R.string.not_available);
        }

        return SIGNATURE_INFO.isSourceStampVerified()
                ? getString(R.string.yes)
                : getString(R.string.no);
    }

    public static String getSigner() {
        if (SIGNATURE_INFO == null || !SIGNATURE_INFO.isVerified()) {
            return getString(R.string.not_signed);
        }

        return SIGNATURE_INFO
                .getSignerCertificates()
                .get(0)
                .getSubjectX500Principal()
                .getName();
    }

    private static String getCertificateDigest(String algorithm) {
        try {
            if (SIGNATURE_INFO == null || !SIGNATURE_INFO.isVerified()) {
                return getString(R.string.not_signed);
            }

            X509Certificate cert =
                    SIGNATURE_INFO.getSignerCertificates().get(0);

            byte[] digest =
                    MessageDigest.getInstance(algorithm)
                            .digest(cert.getEncoded());

            StringBuilder result = new StringBuilder();

            for (byte b : digest) {
                if (result.length() > 0) {
                    result.append(":");
                }

                result.append(
                        String.format("%02X", b) //NON-NLS
                );
            }

            return result.toString();

        } catch (Exception e) {
            return getString(R.string.unknown);
        }
    }

    public static String getCertificateSHA256() {
        return getCertificateDigest("SHA-256"); //NON-NLS
    }

    public static String getCertificateSHA1() {
        return getCertificateDigest("SHA-1"); //NON-NLS
    }

    public static String getCertificateMD5() {
        return getCertificateDigest("MD5");
    }

    public static String getCertificateSubject() {
        if (SIGNATURE_INFO == null || !SIGNATURE_INFO.isVerified()) {
            return getString(R.string.not_signed);
        }

        X509Certificate cert =
                SIGNATURE_INFO.getSignerCertificates().get(0);

        return cert.getSubjectX500Principal().getName();
    }

    public static String getCertificateIssuer() {
        if (SIGNATURE_INFO == null || !SIGNATURE_INFO.isVerified()) {
            return getString(R.string.not_signed);
        }

        X509Certificate cert =
                SIGNATURE_INFO.getSignerCertificates().get(0);

        return cert.getIssuerX500Principal().getName();
    }

    public static String getCertificateValidFrom() {
        if (SIGNATURE_INFO == null || !SIGNATURE_INFO.isVerified()) {
            return getString(R.string.not_signed);
        }

        X509Certificate cert =
                SIGNATURE_INFO.getSignerCertificates().get(0);

        return cert.getNotBefore().toString();
    }

    public static String getCertificateValidUntil() {
        if (SIGNATURE_INFO == null || !SIGNATURE_INFO.isVerified()) {
            return getString(R.string.not_signed);
        }

        X509Certificate cert =
                SIGNATURE_INFO.getSignerCertificates().get(0);

        return cert.getNotAfter().toString();
    }

    // === APPLICATION FLAGS ===

    public static String getSystemApp() {
        return (INFO.applicationInfo.flags
                & ApplicationInfo.FLAG_SYSTEM) != 0
                ? getString(R.string.yes)
                : getString(R.string.no);
    }

    public static String getUpdatedSystemApp() {
        return (INFO.applicationInfo.flags
                & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                ? getString(R.string.yes)
                : getString(R.string.no);
    }

    public static String getLargeHeap() {
        return (INFO.applicationInfo.flags
                & ApplicationInfo.FLAG_LARGE_HEAP) != 0
                ? getString(R.string.yes)
                : getString(R.string.no);
    }

    public static String getDebuggable() {
        return (INFO.applicationInfo.flags
                & ApplicationInfo.FLAG_DEBUGGABLE) != 0
                ? getString(R.string.yes)
                : getString(R.string.no);
    }

    public static String getAllowBackups() {
        return (INFO.applicationInfo.flags
                & ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
                ? getString(R.string.yes)
                : getString(R.string.no);
    }

    public static String getExtractNativeLibs() {
        return (INFO.applicationInfo.flags
                & ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS) != 0
                ? getString(R.string.yes)
                : getString(R.string.no);
    }

    public static String getSupportsRTL() {
        return (INFO.applicationInfo.flags
                & ApplicationInfo.FLAG_SUPPORTS_RTL) != 0
                ? getString(R.string.yes)
                : getString(R.string.no);
    }

    public static String getHasCode() {
        return (INFO.applicationInfo.flags
                & ApplicationInfo.FLAG_HAS_CODE) != 0
                ? getString(R.string.yes)
                : getString(R.string.no);
    }

    // === INSTALLED APK INFO ===

    public static boolean isInstalled() {
        return INSTALLED_INFO != null;
    }

    public static String getMainActivity(Context context) {
        if (!isInstalled()) {
            return getString(R.string.not_installed);
        }

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(INSTALLED_INFO.packageName);

        List<ResolveInfo> activities =
                context.getPackageManager().queryIntentActivities(
                        intent,
                        PackageManager.MATCH_ALL
                );

        if (activities == null || activities.isEmpty()) {
            return getString(R.string.none);
        }

        return activities.get(0).activityInfo.name;
    }

    public static String getDataPath() {
        return isInstalled()
                ? INSTALLED_INFO.applicationInfo.dataDir
                : getString(R.string.not_installed);
    }

    public static String getDirPath() {
        return isInstalled()
                ? INSTALLED_INFO.applicationInfo.deviceProtectedDataDir
                : getString(R.string.not_installed);
    }

    public static String getApkPath() {
        return isInstalled()
                ? INSTALLED_INFO.applicationInfo.sourceDir
                : getString(R.string.not_installed);
    }

    public static String getUID() {
        return isInstalled()
                ? String.valueOf(INSTALLED_INFO.applicationInfo.uid)
                : getString(R.string.not_installed);
    }

    public static String getInstallTime() {
        return isInstalled()
                ? String.valueOf(INSTALLED_INFO.firstInstallTime)
                : getString(R.string.not_installed);
    }

    public static String getLastUpdated() {
        return isInstalled()
                ? String.valueOf(INSTALLED_INFO.lastUpdateTime)
                : getString(R.string.not_installed);
    }

    public static String getPackageInstallerName(Context context) {
        if (!isInstalled()) {
            return getString(R.string.not_installed);
        }

        String installer =
                context.getPackageManager()
                        .getInstallerPackageName(getPackageName());

        return installer != null
                ? installer
                : getString(R.string.unknown);
    }
}
