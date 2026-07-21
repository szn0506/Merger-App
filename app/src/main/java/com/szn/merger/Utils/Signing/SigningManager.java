package com.szn.merger.Utils.Signing;

import android.content.Context;

import com.android.apksig.ApkSigner;
import com.szn.merger.PrefsManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

public class SigningManager {
    private static File ksFile;

    private static String DEFAULT_ALIAS = "androiddebugkey";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final char[] PASSWORD = "android".toCharArray();

    public static final String KEY_AUTO_SIGN = "auto_sign";
    public static final String KEYSTORE_FILE = "debug.keystore";

    public static final String KEY_V1 = "v1";
    public static final String KEY_V2 = "v2";
    public static final String KEY_V3 = "v3";
    public static final String KEY_V4 = "v4";

    public static boolean isSignEnabled(Context context) {
        return PrefsManager.getInstance(context)
                .getBoolean(KEY_AUTO_SIGN, false);
    }

    public static void setSignEnabled(Context context, boolean enabled) {
        PrefsManager.getInstance(context)
                .saveBoolean(KEY_AUTO_SIGN, enabled);
    }

    public static boolean isV1Enabled(Context context) {
        return PrefsManager.getInstance(context)
                .getBoolean(KEY_V1, true);
    }

    public static void setV1Enabled(Context context, boolean enabled) {
        PrefsManager.getInstance(context)
                .saveBoolean(KEY_V1, enabled);
    }

    public static boolean isV2Enabled(Context context) {
        return PrefsManager.getInstance(context)
                .getBoolean(KEY_V2, true);
    }

    public static void setV2Enabled(Context context, boolean enabled) {
        PrefsManager.getInstance(context)
                .saveBoolean(KEY_V2, enabled);
    }

    public static boolean isV3Enabled(Context context) {
        return PrefsManager.getInstance(context)
                .getBoolean(KEY_V3, true);
    }

    public static void setV3Enabled(Context context, boolean enabled) {
        PrefsManager.getInstance(context)
                .saveBoolean(KEY_V3, enabled);
    }

    public static boolean isV4Enabled(Context context) {
        return PrefsManager.getInstance(context)
                .getBoolean(KEY_V4, false);
    }

    public static void setV4Enabled(Context context, boolean enabled) {
        PrefsManager.getInstance(context)
                .saveBoolean(KEY_V4, enabled);
    }

    public static void getKeystoreFile(Context context) {
        ksFile = new File(context.getFilesDir(), KEYSTORE_FILE);

        try {
            if (!ksFile.exists()) {
                try (
                        InputStream inputStream = context.getAssets().open(KEYSTORE_FILE);
                        FileOutputStream fileOutputStream = new FileOutputStream(ksFile)
                ) {
                    byte[] buffer = new byte[8192];
                    int len;

                    while ((len = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, len);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File signApk(Context context, File mergedApk) {
        if (!isSignEnabled(context)) return mergedApk;
        try {
            if (ksFile == null) {
                getKeystoreFile(context);
            }

            KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);

            // Load file into the keystore
            try (FileInputStream fis = new FileInputStream(ksFile)) {
                ks.load(fis, PASSWORD);
            }

            // Retrieve alias safely; if empty, fallback to the default Android debug key
            String alias = DEFAULT_ALIAS;
            if (ks.aliases().hasMoreElements()) {
                alias = ks.aliases().nextElement();
            }

            // Retrieve PrivateKey and Certificate using the same password ("android")
            PrivateKey privateKey = (PrivateKey) ks.getKey(alias, PASSWORD);
            X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

            if (privateKey == null || cert == null) {
                throw new Exception("PrivateKey or Certificate not found inside debug.keystore!");
            }

            File signedApk = new File(mergedApk.getParent(), mergedApk.getName().replace(".apk", "_signed.apk"));

            // Signer Configuration
            ApkSigner.SignerConfig signerConfig =
                    new ApkSigner.SignerConfig.Builder(
                            alias,
                            privateKey,
                            Collections.singletonList(cert)
                    ).build();

            ApkSigner signer = new ApkSigner.Builder(Collections.singletonList(signerConfig))
                    .setInputApk(mergedApk)
                    .setOutputApk(signedApk)
                    .setV1SigningEnabled(isV1Enabled(context))
                    .setV2SigningEnabled(isV2Enabled(context))
                    .setV3SigningEnabled(isV3Enabled(context))
                    .setV4SigningEnabled(isV4Enabled(context))
                    .build();

            // Execute the signing process (This process takes a few seconds depending on the APK size)
            signer.sign();

            // Delete the original raw file only if the signing process succeeds completely without errors
            if (mergedApk.exists()) {
                mergedApk.delete();
            }

            return signedApk;

        } catch (Exception e) {
            e.printStackTrace();
            return mergedApk;
        }
    }
}