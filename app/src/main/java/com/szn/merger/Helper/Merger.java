/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.szn.merger.Helper;

import android.app.Activity;
import android.content.Context;

import com.reandroid.apk.ApkBundle;
import com.reandroid.apk.ApkModule;
import com.reandroid.apkeditor.CommandExecutor;
import com.reandroid.apkeditor.Util;
import com.reandroid.apkeditor.common.AndroidManifestHelper;
import com.reandroid.apkeditor.merge.MergerOptions;
import com.reandroid.app.AndroidManifest;
import com.reandroid.archive.ArchiveEntry;
import com.reandroid.archive.ArchiveFile;
import com.reandroid.archive.InputSource;
import com.reandroid.archive.ZipEntryMap;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.container.SpecTypePair;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResConfig;
import com.reandroid.arsc.value.ResValue;
import com.reandroid.arsc.value.ValueType;
import com.reandroid.utils.HexUtil;
import com.szn.merger.Utils.AutoDevice.AutoDeviceActivity;
import com.szn.merger.Utils.AutoDevice.AutoDeviceManager;
import com.szn.merger.Utils.Processing.ProcessingManager;
import com.szn.merger.Utils.Signing.SigningManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;
public class Merger extends CommandExecutor<MergerOptions> {
    public static String packageName;
    public static String versionName;
    public static String versionCode;

    public static String DPI;
    public static String ABI;
    public static String LANGUAGE;
    public static boolean signed;
    public static String signingSchemes;
    public static String sdkVersion;
    public static int compressionLevel;
    public static int splitCount;
    public static int dexCount;
    public static int resourcesCount;
    public static volatile boolean stopped = false;
    private final Context mContext; //  CONTEXT
    private List<String> allEntries = new ArrayList<>();
    private static long outputSize;

    public Merger(Context context, MergerOptions options) {
        super(options, "[MERGE] ");
        this.mContext = context;
    }

    @Override
    public void logMessage(String message) {
        super.logMessage(message);
        onLog(message);
    }

    public static void stopMerge() {
        stopped = true;
    }

    private void checkStopped() throws IOException {
        if (stopped) {
            throw new IOException("Merge stopped");
        }
    }

    public void logSavedFile(File file) {
        logMessage("Saved to: " + file.getAbsolutePath());
    }

    @Override
    public void runCommand() throws IOException {
        stopped = false;

        checkStopped();

        MergerOptions options = getOptions();

        delete(options.outputFile);
        checkStopped();

        File dir = options.inputFile;
        boolean extracted = false;

        if (dir.isFile()) {
            checkStopped();
            dir = extractFile(dir);
            checkStopped();
            extracted = true;
        }

        checkStopped();
        logMessage("Searching apk files ...");

        ApkBundle bundle = new ApkBundle();
        checkStopped();

        bundle.setAPKLogger(this);
        checkStopped();

        bundle.loadApkDirectory(dir, extracted);
        checkStopped();

        logMessage("Found modules: " + bundle.getApkModuleList().size());
        checkStopped();

        for (ApkModule apkModule : bundle.getApkModuleList()) {

            checkStopped();

            String protect = Util.isProtected(apkModule);
            checkStopped();

            if (protect != null) {
                logMessage(options.inputFile.getAbsolutePath());
                checkStopped();

                logMessage(protect);
                checkStopped();

                return;
            }
        }

        checkStopped();

        ApkModule mergedModule = bundle.mergeModules(options.validateModules);

        checkStopped();

        packageName = mergedModule.getAndroidManifest().getPackageName();
        checkStopped();

        versionName = mergedModule.getAndroidManifest().getVersionName();
        checkStopped();

        versionCode = mergedModule.getAndroidManifest().getVersionCode().toString();
        checkStopped();

        DPI = getDPI(mergedModule);
        checkStopped();

        ABI = getABI(mergedModule);
        checkStopped();

        LANGUAGE = getLanguage(mergedModule);
        checkStopped();

        int minSdk = mergedModule.getAndroidManifest().getMinSdkVersion();
        int targetSdk = mergedModule.getAndroidManifest().getTargetSdkVersion();

        sdkVersion = minSdk + "-" + targetSdk;
        checkStopped();

        splitCount = getSplitCount(bundle);
        checkStopped();

        dexCount = getDexCount(mergedModule);
        checkStopped();

        resourcesCount = getResourceCount(mergedModule);
        checkStopped();

        if (options.resDirName != null) {

            checkStopped();

            logMessage(
                    "Renaming resources root dir: "
                            + options.resDirName
            );
            checkStopped();

            mergedModule.setResourcesRootDir(
                    options.resDirName
            );
            checkStopped();
        }

        if (options.validateResDir) {

            checkStopped();

            logMessage("Validating resources dir ...");
            checkStopped();

            mergedModule.validateResourcesDir();
            checkStopped();
        }

        if (options.cleanMeta) {

            checkStopped();

            logMessage("Clearing META-INF ...");
            checkStopped();

            clearMeta(mergedModule);
            checkStopped();
        }

        checkStopped();

        sanitizeManifest(mergedModule);
        checkStopped();

        mergedModule.refreshTable();
        checkStopped();

        mergedModule.refreshManifest();
        checkStopped();

        applyExtractNativeLibs(mergedModule, options.getExtractNativeLibs());

        checkStopped();

        compressionLevel = ProcessingManager.getCompressionLevel(mContext);
        checkStopped();

        ApkWriter.compressionLevel = compressionLevel;

        checkStopped();

        logMessage("Writing APK ...");
        checkStopped();

        mergedModule.writeApk(options.outputFile);
        checkStopped();

        SigningManager.signApk(mContext, options.outputFile);

        signed = SigningManager.isSignEnabled(mContext);

        StringJoiner schemes = new StringJoiner("-");

        if (SigningManager.isV1Enabled(mContext)) schemes.add("V1");
        if (SigningManager.isV2Enabled(mContext)) schemes.add("V2");
        if (SigningManager.isV3Enabled(mContext)) schemes.add("V3");
        if (SigningManager.isV4Enabled(mContext)) schemes.add("V4");

        signingSchemes = schemes.toString();
        checkStopped();

        outputSize = options.outputFile.length();
        checkStopped();

        mergedModule.close();
        checkStopped();

        bundle.close();
        checkStopped();

        if (extracted) {

            checkStopped();

            Util.deleteDir(dir);
            checkStopped();

            dir.deleteOnExit();
            checkStopped();
        }
    }
    private String getAppName(ApkModule apkModule) {
        if (!apkModule.hasAndroidManifest()) return "Unknown";

        AndroidManifestBlock manifest = apkModule.getAndroidManifest();
        ResXmlElement application = manifest.getApplicationElement();
        if (application == null) return "Unknown";

        ResXmlAttribute label = application.searchAttributeByResourceId(AndroidManifest.ID_label);
        if (label == null) return "Unknown";

        if (label.getValueType() == ValueType.STRING) {
            String str = label.getValueAsString();
            if (str != null && !str.isEmpty()) return str;
        }

        // 2. Jika bernilai REFERENCE (@string/app_name), Resolve lewat TableBlock (resources.arsc)
        if (apkModule.hasTableBlock()) {
            TableBlock tableBlock = apkModule.getTableBlock();
            int resId = label.getData(); // Ambil Resource ID (misal 0x7f110000)

            ResourceEntry resourceEntry = tableBlock.getResource(resId);
            if (resourceEntry != null) {
                Entry entry = resourceEntry.get(); // Ambil default entry
                if (entry != null && entry.getResValue() != null) {
                    String appName = entry.getResValue().getValueAsString();
                    if (appName != null && !appName.isEmpty()) {
                        return appName;
                    }
                }
            }
        }

        // Fallback jika tidak ketemu string-nya di arsc
        String fallback = label.getValueAsString();
        return fallback != null ? fallback : "Unknown";
    }

    private String getABI(ApkModule apkModule) {
        if (apkModule == null) return "";

        StringBuilder result = new StringBuilder();

        Iterator<InputSource> iterator =
                apkModule.getZipEntryMap().iterator();

        while (iterator.hasNext()) {
            InputSource input = iterator.next();
            String name = input.getName();

            if (!name.startsWith("lib/")) continue;

            String[] parts = name.split("/");

            if (parts.length < 3) continue;

            String abi = parts[1];

            if (result.indexOf(abi) == -1) {
                if (result.length() > 0) {
                    result.append(", ");
                }

                result.append(abi);
            }
        }

        return result.toString();
    }
    private String getDPI(ApkModule apkModule) {
        if (apkModule == null || !apkModule.hasTableBlock()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        TableBlock tableBlock = apkModule.getTableBlock();

        Iterator<ResourceEntry> resources = tableBlock.getResources();

        while (resources.hasNext()) {
            ResourceEntry resource = resources.next();

            for (Entry entry : resource) {
                if (entry == null || entry.getResValue() == null) {
                    continue;
                }

                ResConfig config = entry.getResConfig();
                if (config == null) {
                    continue;
                }

                ResConfig.Density density = config.getDensity();
                if (density == null) {
                    continue;
                }

                String value = density.toString();

                if (result.indexOf(value) == -1) {
                    if (result.length() > 0) {
                        result.append(", ");
                    }

                    result.append(value);
                }
            }
        }

        return result.toString();
    }

    private String getLanguage(ApkModule apkModule) {
        if (!apkModule.hasTableBlock()) return "";

        StringBuilder result = new StringBuilder();

        Iterator<ResConfig> iterator =
                apkModule.getTableBlock().getResConfigs();

        while (iterator.hasNext()) {
            ResConfig config = iterator.next();
            String language = config.getLanguage();

            if (language != null && !language.isEmpty()) {
                if (result.indexOf(language) == -1) {
                    if (result.length() > 0) result.append(", ");
                    result.append(language);
                }
            }
        }

        return result.toString();
    }

    private int getResourceCount(ApkModule apkModule) {
        if (apkModule == null || !apkModule.hasTableBlock()) {
            return 0;
        }

        int count = 0;
        Iterator<ResourceEntry> iterator =
                apkModule.getTableBlock().getResources();

        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }

        return count;
    }
    private int getDexCount(ApkModule apkModule) {
        if (apkModule == null) return 0;

        return apkModule.listDexFiles().size();
    }

    public static String formatFileSize() {
        if (outputSize < 1024) return outputSize + " B";
        if (outputSize < 1024 * 1024) return String.format("%.2f KB", outputSize / 1024.0);
        if (outputSize < 1024L * 1024L * 1024L) return String.format("%.2f MB", outputSize / (1024.0 * 1024.0));
        return String.format("%.2f GB", outputSize / (1024.0 * 1024.0 * 1024.0)
        );
    }

    private int getSplitCount(ApkBundle bundle) {
        return Math.max(0, bundle.getApkModuleList().size() - 1);
    }

    protected void onLog(String message) {
        // UI callback
    }
    private File extractFile(File file) throws IOException {
        File tmp = toTmpDir(file);
        logMessage("Extracting to: " + tmp);
        if(tmp.exists()){
            logMessage("Delete: " + tmp);
            Util.deleteDir(tmp);
        }
        tmp.deleteOnExit();
        ArchiveFile archive = new ArchiveFile(file);
        fixFilePermissions(archive);

        allEntries.clear();
        Iterator<ArchiveEntry> iterator = archive.iterator();
        while (iterator.hasNext()) {
            allEntries.add(iterator.next().getName().toLowerCase());
        }

        AutoDeviceActivity.showSplitsPicker((Activity) mContext, allEntries);

        // replace the call to custom one
        Predicate<ArchiveEntry> filter =
                entry -> AutoDeviceManager.shouldExtract(
                        mContext,
                        entry.getName(),
                        allEntries
                );
        int count = archive.extractAll(tmp, filter, this);
        archive.close();
        if(count == 0){
            throw new IOException("No *.apk files found on: " + file);
        }
        return tmp;
    }
    private void fixFilePermissions(ArchiveFile archive) {
        int rw_all = 438; // equivalent to chmod 666
        Iterator<ArchiveEntry> iterator = archive.iterator();
        while (iterator.hasNext()) {
            ArchiveEntry entry = iterator.next();
            entry.getCentralEntryHeader()
                    .getFilePermissions().permissions(rw_all);
        }
    }
    private File toTmpDir(File file){
        String name = file.getName();
        name = HexUtil.toHex8("tmp_", name.hashCode());
        File dir = file.getParentFile();
        File tmp;
        if(dir == null){
            tmp = new File(name);
        }else {
            tmp = new File(dir, name);
        }
        tmp = Util.ensureUniqueFile(tmp);
        return tmp;
    }
    private void sanitizeManifest(ApkModule apkModule) {
        if(!apkModule.hasAndroidManifest()){
            return;
        }
        AndroidManifestBlock manifest = apkModule.getAndroidManifest();
        logMessage("Sanitizing manifest ...");

        AndroidManifestHelper.removeAttributeFromManifestById(manifest, AndroidManifest.ID_requiredSplitTypes, this);
        AndroidManifestHelper.removeAttributeFromManifestById(manifest, AndroidManifest.ID_splitTypes, this);
        AndroidManifestHelper.removeAttributeFromManifestByName(manifest, AndroidManifest.NAME_splitTypes, this);

        AndroidManifestHelper.removeAttributeFromManifestByName(manifest, AndroidManifest.NAME_requiredSplitTypes, this);
        AndroidManifestHelper.removeAttributeFromManifestByName(manifest, AndroidManifest.NAME_splitTypes, this);
        AndroidManifestHelper.removeAttributeFromManifestAndApplication(manifest, AndroidManifest.ID_isSplitRequired, this, AndroidManifest.NAME_isSplitRequired);
        ResXmlElement application = manifest.getApplicationElement();
        List<ResXmlElement> splitMetaDataElements = AndroidManifestHelper.listSplitRequired(application);
        boolean splits_removed = false;
        for(ResXmlElement meta : splitMetaDataElements){
            if(!splits_removed){
                splits_removed = removeSplitsTableEntry(meta, apkModule);
            }
            logMessage("Removed-element : <" + meta.getName() + "> name=\""
                    + AndroidManifestBlock.getAndroidNameValue(meta) + "\"");
            application.remove(meta);
        }
        manifest.refresh();
    }
    private boolean removeSplitsTableEntry(ResXmlElement metaElement, ApkModule apkModule) {
        ResXmlAttribute nameAttribute = metaElement.searchAttributeByResourceId(AndroidManifest.ID_name);
        if(nameAttribute == null){
            return false;
        }
        if(!"com.android.vending.splits".equals(nameAttribute.getValueAsString())){
            return false;
        }
        ResXmlAttribute valueAttribute=metaElement.searchAttributeByResourceId(
                AndroidManifest.ID_value);
        if(valueAttribute==null){
            valueAttribute=metaElement.searchAttributeByResourceId(
                    AndroidManifest.ID_resource);
        }
        if(valueAttribute == null
                || valueAttribute.getValueType() != ValueType.REFERENCE){
            return false;
        }
        if(!apkModule.hasTableBlock()){
            return false;
        }
        TableBlock tableBlock = apkModule.getTableBlock();
        ResourceEntry resourceEntry = tableBlock.getResource(valueAttribute.getData());
        if(resourceEntry == null){
            return false;
        }
        ZipEntryMap zipEntryMap = apkModule.getZipEntryMap();
        for(Entry entry : resourceEntry){
            if(entry == null){
                continue;
            }
            ResValue resValue = entry.getResValue();
            if(resValue == null){
                continue;
            }
            String path = resValue.getValueAsString();
            logMessage("Removed-table-entry : " + path);
            //Remove file entry
            zipEntryMap.remove(path);
            // It's not safe to destroy entry, resource id might be used in dex code.
            // Better replace it with boolean value.
            entry.setNull(true);
            SpecTypePair specTypePair = entry.getTypeBlock()
                    .getParentSpecTypePair();
            specTypePair.removeNullEntries(entry.getId());
        }
        return true;
    }
}
