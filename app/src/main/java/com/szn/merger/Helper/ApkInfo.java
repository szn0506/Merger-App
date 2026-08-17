package com.szn.merger.Helper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.android.apksig.ApkVerifier;
import com.szn.merger.MergeTaskManager;

import java.io.File;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ApkInfo {

    private static File OUTPUT = MergeTaskManager.finalOutput;
    // === FILE APP STATE ===
    private static PackageInfo INFO;
    private static ApkVerifier.Result SIGNATURE_INFO;

    // === INSTALLED APP STATE ===
    private static PackageInfo INSTALLED_INFO;

    // === INITIALIZATION ===

    public static void init(Context context) {
        PackageManager pm = context.getPackageManager();
        int flags = PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS | PackageManager.GET_ACTIVITIES | PackageManager.GET_RECEIVERS | PackageManager.GET_SERVICES | PackageManager.GET_PROVIDERS;
        INFO = pm.getPackageArchiveInfo(OUTPUT.getAbsolutePath(), flags);
        try {
            INSTALLED_INFO = pm.getPackageInfo(INFO.packageName, PackageManager.GET_META_DATA);
            ApkVerifier verifier = new ApkVerifier.Builder(OUTPUT).build();
            SIGNATURE_INFO = verifier.verify();
        } catch (Exception e) {
            INSTALLED_INFO = null;
            SIGNATURE_INFO = null;
        }
    }

    // === MERGE PROCESS INFO ===

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
        return INFO.applicationInfo.loadLabel(context.getPackageManager()).toString();
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
        if (INFO.receivers == null) return new String[0];

        String[] result = new String[INFO.receivers.length];

        for (int i = 0; i < INFO.receivers.length; i++) {
            result[i] = INFO.receivers[i].name;
        }

        return result;
    }

    public static String[] getActivities() {
        if (INFO.activities == null) return new String[0];

        String[] result = new String[INFO.activities.length];

        for (int i = 0; i < INFO.activities.length; i++) {
            result[i] = INFO.activities[i].name;
        }

        return result;
    }

    public static String[] getServices() {
        if (INFO.services == null) return new String[0];

        String[] result = new String[INFO.services.length];

        for (int i = 0; i < INFO.services.length; i++) {
            result[i] = INFO.services[i].name;
        }

        return result;
    }

    public static String[] getProviders() {
        if (INFO.providers == null) return new String[0];

        String[] result = new String[INFO.providers.length];

        for (int i = 0; i < INFO.providers.length; i++) {
            result[i] = INFO.providers[i].name;
        }

        return result;
    }
    // === SIGNATURE INFO ===

    public static String getSigned() {
        return SIGNATURE_INFO != null && SIGNATURE_INFO.isVerified() ? "Yes" : "No";
    }

    public static String getSchemes() {
        if (SIGNATURE_INFO == null || !SIGNATURE_INFO.isVerified()) return "Not Signed";

        List<String> schemes = new ArrayList<>();
        if (SIGNATURE_INFO.isVerifiedUsingV1Scheme()) schemes.add("V1");
        if (SIGNATURE_INFO.isVerifiedUsingV2Scheme()) schemes.add("V2");
        if (SIGNATURE_INFO.isVerifiedUsingV3Scheme()) schemes.add("V3");
        if (SIGNATURE_INFO.isVerifiedUsingV31Scheme()) schemes.add("V3.1");
        if (SIGNATURE_INFO.isVerifiedUsingV4Scheme()) schemes.add("V4");

        return schemes.isEmpty() ? "Not Signed" : String.join(", ", schemes);
    }

    public static String getSourceStampVerified() {
        return SIGNATURE_INFO != null && SIGNATURE_INFO.isVerified() ? SIGNATURE_INFO.isSourceStampVerified() ? "Yes" : "No" : "Not";
    }

    public static String getSigner() {
        if (SIGNATURE_INFO == null || !SIGNATURE_INFO.isVerified()) return "Not Signed";
        return SIGNATURE_INFO.getSignerCertificates().get(0).getSubjectX500Principal().getName();
    }

    private static String getCertificateDigest(String algorithm) {
        try {
            if (SIGNATURE_INFO == null || !SIGNATURE_INFO.isVerified()) return "Not Signed";

            X509Certificate cert = SIGNATURE_INFO.getSignerCertificates().get(0);
            byte[] digest = MessageDigest.getInstance(algorithm).digest(cert.getEncoded());

            StringBuilder result = new StringBuilder();
            for (byte b : digest) {
                if (result.length() > 0) result.append(":");
                result.append(String.format("%02X", b));
            }

            return result.toString();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static String getCertificateSHA256() {
        return getCertificateDigest("SHA-256");
    }

    public static String getCertificateSHA1() {
        return getCertificateDigest("SHA-1");
    }

    public static String getCertificateMD5() {
        return getCertificateDigest("MD5");
    }

    public static String getCertificateSubject() {
        if (SIGNATURE_INFO == null || !SIGNATURE_INFO.isVerified()) return "Not Signed";
        X509Certificate cert = SIGNATURE_INFO.getSignerCertificates().get(0);
        return cert.getSubjectX500Principal().getName();
    }

    public static String getCertificateIssuer() {
        if (SIGNATURE_INFO == null || !SIGNATURE_INFO.isVerified()) return "Not Signed";
        X509Certificate cert = SIGNATURE_INFO.getSignerCertificates().get(0);
        return cert.getIssuerX500Principal().getName();
    }

    public static String getCertificateValidFrom() {
        if (SIGNATURE_INFO == null || !SIGNATURE_INFO.isVerified()) return "Not Signed";
        X509Certificate cert = SIGNATURE_INFO.getSignerCertificates().get(0);
        return cert.getNotBefore().toString();
    }

    public static String getCertificateValidUntil() {
        if (SIGNATURE_INFO == null || !SIGNATURE_INFO.isVerified()) return "Not Signed";
        X509Certificate cert = SIGNATURE_INFO.getSignerCertificates().get(0);
        return cert.getNotAfter().toString();
    }

    // === APK NATIVE LIBRARIES ===

    public static String getABI() {
        try (ZipFile zip = new ZipFile(OUTPUT)) {
            Set<String> abi = new LinkedHashSet<>();
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();

                if (name.startsWith("lib/")) {
                    String[] parts = name.split("/");
                    if (parts.length >= 3) abi.add(parts[1]);
                }
            }

            return abi.isEmpty() ? "Unknown" : String.join(", ", abi);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // === APPLICATION FLAGS ===

    public static String getSystemApp() {
        return (INFO.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 ? "Yes" : "No";
    }

    public static String getUpdatedSystemApp() {
        return (INFO.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 ? "Yes" : "No";
    }

    public static String getLargeHeap() {
        return (INFO.applicationInfo.flags & ApplicationInfo.FLAG_LARGE_HEAP) != 0 ? "Yes" : "No";
    }

    public static String getDebuggable() {
        return (INFO.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0 ? "Yes" : "No";
    }

    public static String getAllowBackups() {
        return (INFO.applicationInfo.flags & ApplicationInfo.FLAG_ALLOW_BACKUP) != 0 ? "Yes" : "No";
    }

    public static String getExtractNativeLibs() {
        return (INFO.applicationInfo.flags & ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS) != 0 ? "Yes" : "No";
    }

    public static String getSupportsRTL() {
        return (INFO.applicationInfo.flags & ApplicationInfo.FLAG_SUPPORTS_RTL) != 0 ? "Yes" : "No";
    }

    public static String getHasCode() {
        return (INFO.applicationInfo.flags & ApplicationInfo.FLAG_HAS_CODE) != 0 ? "Yes" : "No";
    }

    // === INSTALLED APK INFO ===

    public static boolean isInstalled() {
        return INSTALLED_INFO != null;
    }

    public static String getMainActivity(Context context) {
        if (!isInstalled()) return "Not Installed";
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(INSTALLED_INFO.packageName);
        List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_ALL);
        if (activities == null || activities.isEmpty()) return "None";
        return activities.get(0).activityInfo.name;
    }
    public static String getDataPath() {
        return isInstalled() ? INSTALLED_INFO.applicationInfo.dataDir : "Not Installed";
    }

    public static String getDirPath() {
        return isInstalled() ? INSTALLED_INFO.applicationInfo.deviceProtectedDataDir : "Not Installed";
    }

    public static String getApkPath() {
        return isInstalled() ? INSTALLED_INFO.applicationInfo.sourceDir : "Not Installed";
    }

    public static String getUID() {
        return isInstalled() ? String.valueOf(INSTALLED_INFO.applicationInfo.uid) : "Not Installed";
    }

    public static String getInstallTime() {
        return isInstalled() ? String.valueOf(INSTALLED_INFO.firstInstallTime) : "Not Installed";
    }

    public static String getLastUpdated() {
        return isInstalled() ? String.valueOf(INSTALLED_INFO.lastUpdateTime) : "Not Installed";
    }

    public static String getPackageInstallerName(Context context) {
        if (!isInstalled()) return "Not Installed";
        String installer = context.getPackageManager().getInstallerPackageName(getPackageName());
        return installer != null ? installer : "Unknown";
    }
}