package com.szn.merger.Utils.Signing;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Enumeration;
import java.util.UUID;

public final class KeystoreImporter {

    private final Context context;

    public KeystoreImporter(Context context) {
        this.context = context.getApplicationContext();
    }

    private String getFileName(Uri uri, Context context) {

        String result = null;

        Cursor cursor = context.getContentResolver()
                .query(uri, null, null, null, null);

        if (cursor != null) {

            if (cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                if (index >= 0) {
                    result = cursor.getString(index);
                }
            }

            cursor.close();
        }

        return result;
    }
    public KeystoreManager.Item importKeystore(Uri uri, String name, String password, String type) throws Exception {
        File folder = new File(context.getFilesDir(),"keystores");

        if (!folder.exists()) folder.mkdirs();

        String extension = type.equalsIgnoreCase("PKCS12")
                        ? ".p12"
                        : ".jks";

        String id =
                UUID.randomUUID()
                        .toString();

        String fileName = getFileName(uri, context) + extension;

        File output = new File(folder, fileName);

        try (InputStream input = context.getContentResolver().openInputStream(uri);
                FileOutputStream outputStream = new FileOutputStream(output)
        ) {

            byte[] buffer = new byte[8192];

            int length;

            while ((length = input.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length
                );
            }
        }

        KeyStore keyStore = KeyStore.getInstance(type);
        try (FileInputStream fis = new FileInputStream(output)) {
            keyStore.load(fis, password.toCharArray());
        }
        String alias = null;

        Enumeration<String> aliases = keyStore.aliases();

        while (aliases.hasMoreElements()) {
            String current = aliases.nextElement();

            if (keyStore.isKeyEntry(current)) {
                alias = current;
                break;
            }
        }

        if (alias == null) {
            throw new Exception("No private key entry found");
        }
        KeystoreManager.Item item = new KeystoreManager.Item();

        item.id = id;
        item.name = name;
        item.fileName = fileName;
        item.alias = alias;
        item.password = password;
        item.type = type;

        return item;
    }
}