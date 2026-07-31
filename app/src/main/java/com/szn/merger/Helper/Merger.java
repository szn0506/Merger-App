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
import com.reandroid.archive.ZipEntryMap;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.container.SpecTypePair;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResValue;
import com.reandroid.arsc.value.ValueType;
import com.reandroid.utils.HexUtil;
import com.szn.merger.Utils.AutoDevice.AutoDeviceActivity;
import com.szn.merger.Utils.AutoDevice.AutoDeviceManager;
import com.szn.merger.Utils.Processing.ProcessingManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
public class Merger extends CommandExecutor<MergerOptions> {
    public static final String LOG_DEFAULT = "Default";
    public static final String LOG_SIMPLE = "Simple";
    public static String packageName;
    public static String versionName;
    private final String logMode;
    private final Context mContext; //  CONTEXT
    private List<String> allEntries = new ArrayList<>();

    public Merger(Context context, MergerOptions options, String logMode) {
        super(options, "[MERGE] ");
        this.mContext = context;
        this.logMode = logMode;
    }

    @Override
    public void logMessage(String message) {
        if (LOG_DEFAULT.equals(logMode)) {
            super.logMessage(message);
            onLog(message);
        }
    }

    private void simpleLog(String message) {
        if (LOG_SIMPLE.equals(logMode)) {
            super.logMessage(message);
            onLog(message);
        }
    }

    public void logSavedFile(File file) {
        logMessage("Saved to: " + file.getAbsolutePath());

        if (LOG_SIMPLE.equals(logMode)) {
            onLog("Saved APK        : " + file.getAbsolutePath());
        }
    }
    @Override
    public void runCommand() throws IOException {

        MergerOptions options = getOptions();

        delete(options.outputFile);

        File dir = options.inputFile;
        boolean extracted = false;

        if (dir.isFile()) {
            dir = extractFile(dir);
            extracted = true;
        }
        logMessage("Searching apk files ...");

        ApkBundle bundle = new ApkBundle();
        bundle.setAPKLogger(this);
        bundle.loadApkDirectory(dir, extracted);

        logMessage("Found modules: " + bundle.getApkModuleList().size());

        for (ApkModule apkModule : bundle.getApkModuleList()) {
            String protect = Util.isProtected(apkModule);

            if (protect != null) {
                logMessage(options.inputFile.getAbsolutePath());
                logMessage(protect);
                return;
            }
        }

        ApkModule mergedModule = bundle.mergeModules(options.validateModules);

        // =========================
        // APP INFORMATION
        // =========================

        String appName = getAppName(mergedModule);
        packageName = mergedModule.getAndroidManifest().getPackageName();
        versionName = mergedModule.getAndroidManifest().getVersionName();
        int splitCount = getSplitCount(bundle);
        int dexCount = getDexCount(mergedModule);

        simpleLog("App Name              : " + appName);

        simpleLog("Package               : " + packageName);

        simpleLog("Version Name          : " + versionName);

        simpleLog("Splits                : " + splitCount);

        simpleLog("DEX Files             : " + dexCount);

        // =========================
        // PROCESSING
        // =========================

        if (options.resDirName != null) {
            logMessage("Renaming resources root dir: " + options.resDirName);

            mergedModule.setResourcesRootDir(options.resDirName);
        }

        if (options.validateResDir) {
            logMessage("Validating resources dir ...");

            mergedModule.validateResourcesDir();
        }

        if (options.cleanMeta) {
            logMessage("Clearing META-INF ...");

            clearMeta(mergedModule);
        }

        sanitizeManifest(mergedModule);

        mergedModule.refreshTable();
        mergedModule.refreshManifest();

        // =========================
        // EXTRACT NATIVE LIBS
        // =========================

        String extractNativeLibs = options.extractNativeLibs;

        simpleLog("Extract Native Libs   : " + extractNativeLibs);

        logMessage("Applying extract Native Libs   : " + extractNativeLibs);

        applyExtractNativeLibs(mergedModule, options.getExtractNativeLibs());

        // =========================
        // COMPRESSION
        // =========================

        int compressionLevel = ProcessingManager.getCompressionLevel(mContext);

        ApkWriter.compressionLevel = compressionLevel;

        simpleLog("Compression Level     : " + compressionLevel);

        // =========================
        // WRITE APK
        // =========================

        logMessage("Writing APK ...");

        mergedModule.writeApk(
                options.outputFile
        );

        long outputSize = options.outputFile.length();

        simpleLog("File Size             : " + formatFileSize(outputSize));

        // =========================
        // CLOSE
        // =========================

        mergedModule.close();
        bundle.close();

        if (extracted) {
            Util.deleteDir(dir);
            dir.deleteOnExit();
        }
    }

    private String getAppName(ApkModule apkModule) {
        if (!apkModule.hasAndroidManifest()) return "Unknown";

        AndroidManifestBlock manifest = apkModule.getAndroidManifest();
        ResXmlElement application = manifest.getApplicationElement();
        if (application == null) return "Unknown";

        ResXmlAttribute label = application.searchAttributeByResourceId(AndroidManifest.ID_label);
        if (label == null) return "Unknown";

        // 1. Jika bernilai STRING murni (langsung tertulis di manifest)
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

    private int getDexCount(ApkModule apkModule) {
        if (apkModule == null) return 0;

        // REAndroid menyediakan API bawaan untuk mendaftar seluruh DEX file
        return apkModule.listDexFiles().size();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024L * 1024L) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0)
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

        AndroidManifestHelper.removeAttributeFromManifestById(manifest,
                AndroidManifest.ID_requiredSplitTypes, this);
        AndroidManifestHelper.removeAttributeFromManifestById(manifest,
                AndroidManifest.ID_splitTypes, this);
        AndroidManifestHelper.removeAttributeFromManifestByName(manifest,
                AndroidManifest.NAME_splitTypes, this);

        AndroidManifestHelper.removeAttributeFromManifestByName(manifest,
                AndroidManifest.NAME_requiredSplitTypes, this);
        AndroidManifestHelper.removeAttributeFromManifestByName(manifest,
                AndroidManifest.NAME_splitTypes, this);
        AndroidManifestHelper.removeAttributeFromManifestAndApplication(manifest,
                AndroidManifest.ID_isSplitRequired,
                this, AndroidManifest.NAME_isSplitRequired);
        ResXmlElement application = manifest.getApplicationElement();
        List<ResXmlElement> splitMetaDataElements =
                AndroidManifestHelper.listSplitRequired(application);
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
