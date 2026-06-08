package com.szn.merger.Helper;

import android.content.Context;
import com.android.apksig.ApkSigner;
import com.szn.merger.CustomSwitchItem;
import com.szn.merger.PrefsManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

public class Signing {

    public static String KEY_AUTO_SIGN = "auto_sign";
    public static String KEYSTORE_FILE = "debug.keystore";

    public static void setupAutoSign(Context context, PrefsManager prefs, CustomSwitchItem switchItem) {
        boolean isEnabled = prefs.getBoolean(KEY_AUTO_SIGN, false);
        switchItem.setChecked(isEnabled);

        switchItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.saveBoolean(KEY_AUTO_SIGN, isChecked);
            if (isChecked) {
                // Run on a background thread to prevent the UI from freezing during the initial file copy
                new Thread(() -> {
                    try {
                        getKeystoreFile(context);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

    public static File checkAutoSign(Context context, PrefsManager prefs, File mergedApk) {
        boolean isEnabled = prefs.getBoolean(KEY_AUTO_SIGN, false);

        if (isEnabled) {
            return signApk(context, mergedApk);
        } else {
            return mergedApk;
        }
    }

    public static File getKeystoreFile(Context context) throws Exception {
        File ksFile = new File(context.getFilesDir(), KEYSTORE_FILE);
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
        return ksFile;
    }

    public static File signApk(Context context, File mergedApk) {
        try {
            File ksFile = getKeystoreFile(context);

            // FIX: Force using "JKS" type because the default Android Studio debug.keystore is JKS by standard
            KeyStore ks = KeyStore.getInstance("PKCS12");
            char[] password = "android".toCharArray();

            // Load file into the keystore
            try (FileInputStream fis = new FileInputStream(ksFile)) {
                ks.load(fis, password);
            }

            // Retrieve alias safely; if empty, fallback to the default Android debug key
            String alias = "androiddebugkey";
            if (ks.aliases().hasMoreElements()) {
                alias = ks.aliases().nextElement();
            }

            // Retrieve PrivateKey and Certificate using the same password ("android")
            PrivateKey privateKey = (PrivateKey) ks.getKey(alias, password);
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
                    .setV1SigningEnabled(true)
                    .setV2SigningEnabled(true)
                    .setV3SigningEnabled(true)
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