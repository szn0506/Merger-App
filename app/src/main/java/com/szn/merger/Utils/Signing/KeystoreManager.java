package com.szn.merger.Utils.Signing;

import android.content.Context;

import com.android.apksig.ApkSigner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class KeystoreManager {

    private final Context context;

    public KeystoreManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static class Item {

        public String id;

        public String name;

        public String fileName;

        public String alias;

        public String password;

        public String type;

        public transient PrivateKey privateKey;

        public transient List<X509Certificate> certificates;
    }

    public File getFolder() {

        File folder =
                new File(
                        context.getFilesDir(),
                        "keystores"
                );

        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }

    private File getMetaFile(String id) {
        return new File(getFolder(), id + ".properties");
    }

    private File getFile(Item item) {
        return new File(getFolder(), item.fileName);
    }

    public File getKeystoreFile(Item item) {
        return getFile(item);
    }

    public void save(Item item) throws IOException {

        Properties props = new Properties();

        props.setProperty("id", item.id);
        props.setProperty("name", item.name);
        props.setProperty("fileName", item.fileName);
        props.setProperty("alias", item.alias);
        props.setProperty("password", item.password);
        props.setProperty("type", item.type);

        try (FileOutputStream fos =
                     new FileOutputStream(getMetaFile(item.id))) {

            props.store(fos, null);
        }
    }

    private Item read(File meta) throws IOException {

        Properties props = new Properties();

        try (FileInputStream fis =
                     new FileInputStream(meta)) {

            props.load(fis);
        }

        Item item = new Item();

        item.id = props.getProperty("id");
        item.name = props.getProperty("name");
        item.fileName = props.getProperty("fileName");
        item.alias = props.getProperty("alias");
        item.password = props.getProperty("password");
        item.type = props.getProperty("type");

        return item;
    }

    public List<Item> getAll() {

        List<Item> list =
                new ArrayList<>();

        File[] files =
                getFolder().listFiles();

        if (files == null) {
            return list;
        }

        for (File file : files) {

            if (!file.getName().endsWith(".properties")) {
                continue;
            }

            try {
                list.add(read(file));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return list;
    }

    public void delete(Item item) {

        File keystore =
                getFile(item);

        if (keystore.exists()) {
            keystore.delete();
        }

        File meta =
                getMetaFile(item.id);

        if (meta.exists()) {
            meta.delete();
        }
    }

    public void rename(
            Item item,
            String newName
    ) throws IOException {

        item.name = newName;

        save(item);
    }

    public void load(
            Item item
    ) throws Exception {

        File file =
                getFile(item);

        KeyStore keyStore =
                KeyStore.getInstance(
                        item.type
                );

        try (
                FileInputStream fis =
                        new FileInputStream(file)
        ) {

            keyStore.load(
                    fis,
                    item.password.toCharArray()
            );
        }

        if (item.alias == null ||
                item.alias.isEmpty()) {

            item.alias =
                    keyStore.aliases()
                            .nextElement();

            save(item);
        }

        item.privateKey =
                (PrivateKey)
                        keyStore.getKey(
                                item.alias,
                                item.password.toCharArray()
                        );

        Certificate[] chain =
                keyStore.getCertificateChain(
                        item.alias
                );

        item.certificates =
                new ArrayList<>();

        if (chain != null) {

            for (Certificate c : chain) {

                item.certificates.add(
                        (X509Certificate) c
                );
            }
        }
    }

    public ApkSigner.SignerConfig getSignerConfig(
            Item item
    ) throws Exception {

        if (item.privateKey == null) {
            load(item);
        }

        return new ApkSigner
                .SignerConfig
                .Builder(
                item.alias,
                item.privateKey,
                item.certificates
        )
                .build();
    }
}