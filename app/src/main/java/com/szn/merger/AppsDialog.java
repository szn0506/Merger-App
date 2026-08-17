package com.szn.merger;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.szn.merger.Utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AppsDialog {

    public interface OnAppExtractedListener {
        void onAppExtractionStart(String message);
        void onAppExtractionSuccess(File file, String fileName, int splitCount);
        void onAppExtractionFailed(String errorMsg);
    }

    private final Activity activity;
    private final OnAppExtractedListener listener;
    private final List<ApplicationInfo> cachedValidApps = new ArrayList<>();
    private final HashMap<String, String> cachedLabels = new HashMap<>();
    private final HashMap<String, String> cachedVersions = new HashMap<>();
    private final HashMap<String, String> cachedSizes = new HashMap<>();
    private final HashMap<String, Drawable> iconCache = new HashMap<>();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private boolean isDataPreloaded = false;
    private int expandedPosition = -1;

    private EditText searchApp;
    private ListView appsListView;
    private Button btnCancel;
    private MaterialButton btnExtractConfirm;
    private ArrayAdapter<ApplicationInfo> adapter;

    public AppsDialog(Activity activity, OnAppExtractedListener listener) {
        this.activity = activity;
        this.listener = listener;
        preloadInstalledApps();
    }

    private void preloadInstalledApps() {
        new Thread(() -> {
            try {
                PackageManager pm = activity.getPackageManager();
                List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

                synchronized (cachedValidApps) {
                    cachedValidApps.clear();
                    cachedLabels.clear();
                    cachedVersions.clear();
                    cachedSizes.clear();

                    for (ApplicationInfo app : apps) {
                        if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                                && app.splitSourceDirs != null
                                && app.splitSourceDirs.length > 0) {

                            cachedValidApps.add(app);
                            cachedLabels.put(app.packageName, app.loadLabel(pm).toString());

                            try {
                                PackageInfo pi = pm.getPackageInfo(app.packageName, 0);
                                cachedVersions.put(app.packageName, pi.versionName != null ? pi.versionName : "1.0");
                            } catch (Exception e) {
                                cachedVersions.put(app.packageName, "N/A");
                            }

                            long totalSize = new File(app.sourceDir).length();
                            if (app.splitSourceDirs != null) {
                                for (String splitPath : app.splitSourceDirs) {
                                    totalSize += new File(splitPath).length();
                                }
                            }
                            cachedSizes.put(app.packageName, formatSize(totalSize));
                        }
                    }
                }
                isDataPreloaded = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String formatSize(long sizeInBytes) {
        if (sizeInBytes <= 0) return "0 MB";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(sizeInBytes) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(sizeInBytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public void show() {
        if (!isDataPreloaded) {
            Utils.toast(activity, "Loading application data, please wait...");
            return;
        }

        List<ApplicationInfo> validApps;
        synchronized (cachedValidApps) {
            validApps = new ArrayList<>(cachedValidApps);
        }

        if (validApps.isEmpty()) {
            Utils.toast(activity, "No installed Split APK applications found.");
            return;
        }

        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.installed_apps_list, null);

        initViews(dialogView);

        expandedPosition = -1;
        if (btnExtractConfirm != null) {
            btnExtractConfirm.setEnabled(false);
        }

        List<ApplicationInfo> filteredApps = new ArrayList<>(validApps);

        setupAdapter(inflater, filteredApps);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        setupDialogActions(dialog, filteredApps);
        setupSearch(validApps, filteredApps);

        dialog.show();
    }

    private void initViews(View dialogView) {
        searchApp = dialogView.findViewById(R.id.search_app);
        appsListView = dialogView.findViewById(R.id.appsListView);
        btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        btnExtractConfirm = dialogView.findViewById(R.id.btnExtractConfirm);
    }

    private void setupAdapter(LayoutInflater inflater, List<ApplicationInfo> filteredApps) {
        PackageManager pm = activity.getPackageManager();

        TypedValue surfaceColor = new TypedValue();
        TypedValue variantColor = new TypedValue();
        activity.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, surfaceColor, true);
        activity.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, variantColor, true);

        adapter = new ArrayAdapter<ApplicationInfo>(activity, 0, filteredApps) {
            class ViewHolder {
                View cardItem;
                ShapeableImageView appIcon;
                TextView appLabel;
                //TextView pkgName;
                TextView appVersion;
                TextView pkgSize;
                MaterialButton btnSplits;
                String currentPkg;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;

                if (convertView == null) {
                    View templateView = inflater.inflate(R.layout.installed_apps_list, parent, false);
                    View innerItem = templateView.findViewById(R.id.itemAppTemplate);

                    if (innerItem != null) {
                        ViewGroup innerParent = (ViewGroup) innerItem.getParent();
                        if (innerParent != null) {
                            innerParent.removeView(innerItem);
                        }
                        innerItem.setVisibility(View.VISIBLE);
                        convertView = innerItem;
                    } else {
                        convertView = new View(activity);
                    }

                    holder = new ViewHolder();
                    holder.cardItem = convertView;
                    holder.appIcon = convertView.findViewById(R.id.appIcon);
                    holder.appLabel = convertView.findViewById(R.id.appName);
                    //holder.pkgName = convertView.findViewById(R.id.pkgName);
                    holder.appVersion = convertView.findViewById(R.id.pkgVersion);
                    holder.pkgSize = convertView.findViewById(R.id.pkgSize);
                    holder.btnSplits = convertView.findViewById(R.id.btnSplits);

                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                ApplicationInfo app = getItem(position);
                if (app != null) {
                    holder.currentPkg = app.packageName;
                    holder.appLabel.setText(cachedLabels.get(app.packageName));
                    holder.appVersion.setText(cachedVersions.get(app.packageName));
                    holder.pkgSize.setText(cachedSizes.get(app.packageName));

                    int totalSplits = 1;
                    if (app.splitSourceDirs != null) {
                        totalSplits += app.splitSourceDirs.length;
                    }
                    if (holder.btnSplits != null) {
                        holder.btnSplits.setText(totalSplits + " Splits");
                    }

                    if (holder.appIcon != null) {
                        holder.appIcon.setImageResource(android.R.drawable.sym_def_app_icon);

                        if (iconCache.containsKey(app.packageName)) {
                            holder.appIcon.setImageDrawable(iconCache.get(app.packageName));
                        } else {
                            new Thread(() -> {
                                try {
                                    Drawable icon = app.loadIcon(pm);
                                    iconCache.put(app.packageName, icon);
                                    uiHandler.post(() -> {
                                        if (holder.currentPkg.equals(app.packageName)) {
                                            holder.appIcon.setImageDrawable(icon);
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        }
                    }

                    if (holder.cardItem instanceof MaterialCardView) {
                        MaterialCardView card = (MaterialCardView) holder.cardItem;

                        if (position == expandedPosition) {
                            card.setCardBackgroundColor(variantColor.data);
                            holder.appLabel.setEllipsize(null);
                            holder.appLabel.setMaxLines(Integer.MAX_VALUE);
                            //holder.pkgName.setText(app.packageName);
                            //holder.pkgName.setEllipsize(null);
                            //holder.pkgName.setMaxLines(Integer.MAX_VALUE);
                            holder.appVersion.setEllipsize(null);
                            holder.appVersion.setMaxLines(Integer.MAX_VALUE);
                            holder.pkgSize.setEllipsize(null);
                            holder.pkgSize.setMaxLines(Integer.MAX_VALUE);
                        } else {
                            card.setCardBackgroundColor(surfaceColor.data);
                            holder.appLabel.setEllipsize(TextUtils.TruncateAt.END);
                            holder.appLabel.setMaxLines(1);
                            //holder.pkgName.setText(app.packageName);
                            //holder.pkgName.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                            //holder.pkgName.setMaxLines(1);
                            holder.appVersion.setEllipsize(TextUtils.TruncateAt.END);
                            holder.appVersion.setMaxLines(1);
                            holder.pkgSize.setEllipsize(TextUtils.TruncateAt.END);
                            holder.pkgSize.setMaxLines(1);
                        }
                    }
                }
                return convertView;
            }
        };

        appsListView.setAdapter(adapter);
    }

    private void setupDialogActions(AlertDialog dialog, List<ApplicationInfo> filteredApps) {
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Local wrapper to lock click status & animation (300ms)
        final boolean[] isAnimating = {false};

        appsListView.setOnItemClickListener((parent, view, position, id) -> {
            // 1. If currently in the middle of an animation/click delay, ignore input
            if (isAnimating[0]) return;

            // 2. Lock both clicks and scrolling simultaneously
            isAnimating[0] = true;
            appsListView.setOnTouchListener((v, event) -> true);

            // 3. Run layout transition animation
            Utils.applyLayoutTransition(appsListView);

            if (expandedPosition == position) {
                expandedPosition = -1;
                if (btnExtractConfirm != null) {
                    Utils.animateButtonState(btnExtractConfirm, false);
                }
            } else {
                expandedPosition = position;
                if (btnExtractConfirm != null) {
                    Utils.animateButtonState(btnExtractConfirm, true);
                }
            }

            // 4. Refresh adapter
            adapter.notifyDataSetChanged();

            // 5. Release all locks after the animation finishes (300ms)
            appsListView.postDelayed(() -> {
                appsListView.setOnTouchListener(null); // Unlock scrolling
                appsListView.invalidateViews();
                appsListView.requestLayout();
                isAnimating[0] = false;                // Unlock clicks
            }, 300);
        });

        if (btnExtractConfirm != null) {
            btnExtractConfirm.setOnClickListener(v -> {
                if (expandedPosition != -1 && expandedPosition < adapter.getCount()) {
                    ApplicationInfo selectedApp = adapter.getItem(expandedPosition);
                    if (selectedApp != null) {
                        extractApk(selectedApp);
                        dialog.dismiss();
                    }
                }
            });
        }
    }

    private void setupSearch(List<ApplicationInfo> validApps, List<ApplicationInfo> filteredApps) {
        // Call the new Utils method. It automatically handles the 150ms debounce,
        // appsListView layout transitions, and triggers premium text animations while typing.
        Utils.animateTextChange(searchApp, appsListView, 150, query -> {

            // 1. Clear the old filter list
            filteredApps.clear();

            // 2. Perform a single-pass search on the valid apps list
            for (ApplicationInfo app : validApps) {
                String label = cachedLabels.get(app.packageName);
                if (label != null) {
                    label = label.toLowerCase();
                    String pkg = app.packageName.toLowerCase();

                    if (label.contains(query) || pkg.contains(query)) {
                        filteredApps.add(app);
                    }
                }
            }

            // 3. Reset the position of the item currently being expanded
            expandedPosition = -1;
            if (btnExtractConfirm != null) {
                Utils.animateButtonState(btnExtractConfirm, false);
            }

            // 4. Refresh the adapter view
            adapter.notifyDataSetChanged();
        });
    }

    private void extractApk(ApplicationInfo appInfo) {
        if (listener != null) {
            listener.onAppExtractionStart("Extracting " + appInfo.loadLabel(activity.getPackageManager()) + "...");
        }
        final int splitCount = appInfo.splitSourceDirs != null ? appInfo.splitSourceDirs.length : 0;

        new Thread(() -> {
            try {
                PackageManager pm = activity.getPackageManager();
                String appLabel = appInfo.loadLabel(pm).toString();
                String cleanName = appLabel.replaceAll("[\\\\/:*?\"<>|]", "").trim();
                String outputFileName = cleanName + ".apks";

                File tempFile = new File(activity.getCacheDir(), outputFileName);

                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile))) {
                    byte[] buffer = new byte[8192];
                    int read;

                    File baseFile = new File(appInfo.sourceDir);
                    try (InputStream is = new FileInputStream(baseFile)) {
                        zos.putNextEntry(new ZipEntry("base.apk"));
                        while ((read = is.read(buffer)) != -1) {
                            zos.write(buffer, 0, read);
                        }
                        zos.closeEntry();
                    }

                    if (appInfo.splitSourceDirs != null) {
                        for (String splitPath : appInfo.splitSourceDirs) {
                            File splitFile = new File(splitPath);
                            try (InputStream is = new FileInputStream(splitFile)) {
                                zos.putNextEntry(new ZipEntry(splitFile.getName()));
                                while ((read = is.read(buffer)) != -1) {
                                    zos.write(buffer, 0, read);
                                }
                                zos.closeEntry();
                            }
                        }
                    }
                }

                if (listener != null) {
                    uiHandler.post(() -> listener.onAppExtractionSuccess(tempFile, outputFileName, splitCount));
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (listener != null) {
                    uiHandler.post(() -> listener.onAppExtractionFailed(e.getMessage()));
                }
            }
        }).start();
    }
}