package com.szn.merger.Utils.Signing;

import android.util.Log;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public final class KeystoreGenerator {

    public final String keystoreName;
    public final String alias;
    public final String password;

    public String keystoreType = "PKCS12";

    public String keyAlgorithm = "RSA";
    public int keySize = 2048;

    public String signatureAlgorithm = "SHA256withRSA";

    public int validityYears = 30;

    public String commonName = "";

    public String organizationUnit = "Android"; //NON-NLS
    public String organization = "Android"; //NON-NLS
    public String locality = "Unknown"; //NON-NLS
    public String state = "Unknown"; //NON-NLS
    public String country = "US";


    public KeystoreGenerator(
            String keystoreName,
            String alias,
            String password
    ) {
        this.keystoreName = keystoreName;
        this.alias = alias;
        this.password = password;
    }


    public KeystoreManager.Item generate(
            File directory
    ) throws Exception {


        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException(
                    "Cannot create directory"
            );
        }


        String id =
                UUID.randomUUID()
                        .toString();


        String extension =
                keystoreType.equalsIgnoreCase("PKCS12")
                        ? ".p12"
                        : ".jks";


        String fileName =
                keystoreName + extension;


        File output =
                new File(
                        directory,
                        fileName
                );


        Log.d(
                "KEYSTORE", //NON-NLS
                "Output: " + output.getAbsolutePath()
        );


        if (Security.getProvider("BC") == null) {
            Security.addProvider(
                    new BouncyCastleProvider()
            );
        }


        SecureRandom random =
                new SecureRandom();


        KeyPairGenerator generator =
                KeyPairGenerator.getInstance(
                        keyAlgorithm
                );


        generator.initialize(
                keySize,
                random
        );


        KeyPair keyPair =
                generator.generateKeyPair();



        Calendar calendar =
                Calendar.getInstance();


        Date notBefore =
                calendar.getTime();


        calendar.add(
                Calendar.YEAR,
                validityYears
        );


        Date notAfter =
                calendar.getTime();



        BigInteger serial =
                new BigInteger(
                        64,
                        random
                );



        String cn =
                commonName.isEmpty()
                        ? alias
                        : commonName;



        X500Name subject =
                new X500Name(
                        "CN=" + cn + //NON-NLS
                                ", OU=" + organizationUnit + //NON-NLS
                                ", O=" + organization + //NON-NLS //NON-NLS
                                ", L=" + locality +
                                ", ST=" + state + //NON-NLS
                                ", C=" + country //NON-NLS
                );



        JcaX509v3CertificateBuilder builder =
                new JcaX509v3CertificateBuilder(
                        subject,
                        serial,
                        notBefore,
                        notAfter,
                        subject,
                        keyPair.getPublic()
                );



        ContentSigner signer =
                new JcaContentSignerBuilder(
                        signatureAlgorithm
                )
                        .build(
                                keyPair.getPrivate()
                        );



        X509CertificateHolder holder =
                builder.build(signer);



        X509Certificate certificate =
                new JcaX509CertificateConverter()
                        .getCertificate(holder);



        KeyStore keyStore =
                KeyStore.getInstance(
                        keystoreType
                );


        keyStore.load(
                null,
                null
        );


        keyStore.setKeyEntry(
                alias,
                keyPair.getPrivate(),
                password.toCharArray(),
                new Certificate[]{
                        certificate
                }
        );



        try (
                FileOutputStream fos =
                        new FileOutputStream(output)
        ) {

            keyStore.store(
                    fos,
                    password.toCharArray()
            );

            fos.flush();
        }


        Log.d(
                "KEYSTORE",
                "Created: " + output.exists()
        );



        KeystoreManager.Item item =
                new KeystoreManager.Item();


        item.id = id;
        item.name = keystoreName;
        item.fileName = fileName;
        item.alias = alias;
        item.password = password;
        item.type = keystoreType;


        return item;
    }
}