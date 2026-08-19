package com.szn.merger.Utils.Adapter;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.szn.merger.R;
import com.szn.merger.Utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class InstalledAppsManager {

    public interface OnAppExtractionListener {
        void onAppExtractionStart(String message);

        void onAppExtractionSuccess(File file, String fileName, int splitCount);

        void onAppExtractionFailed(String errorMsg);
    }

    private final Context context;
    private final PackageManager packageManager;
    private final OnAppExtractionListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<String, String> labelCache = new HashMap<>();
    private final Map<String, Drawable> iconCache = new HashMap<>();
    private final Map<String, Long> totalSizeCache = new HashMap<>();
    private final Map<String, String> versionNameCache = new HashMap<>();
    private final Map<String, Long> versionCodeCache = new HashMap<>();

    public InstalledAppsManager(Context context, OnAppExtractionListener listener) {
        this.context = context.getApplicationContext();
        this.packageManager = context.getPackageManager();
        this.listener = listener;
    }

    public static List<ApplicationInfo> getInstalledApps(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> result = new ArrayList<>();
        List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo app : installedApps) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue;
            }

            if (app.splitSourceDirs == null || app.splitSourceDirs.length == 0) {
                continue;
            }

            result.add(app);
        }

        return result;
    }

    public void preloadApps(List<ApplicationInfo> apps, Runnable onFinished) {
        new Thread(() -> {
            try {
                for (ApplicationInfo app : apps) {
                    preloadApp(app);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (onFinished != null) {
                mainHandler.post(onFinished);
            }
        }).start();
    }

    private void preloadApp(ApplicationInfo app) {
        String packageName = app.packageName;

        if (!labelCache.containsKey(packageName)) {
            try {
                labelCache.put(packageName, app.loadLabel(packageManager).toString());
            } catch (Exception e) {
                labelCache.put(packageName, packageName);
            }
        }

        if (!iconCache.containsKey(packageName)) {
            try {
                iconCache.put(packageName, app.loadIcon(packageManager));
            } catch (Exception e) {
                iconCache.put(packageName, context.getDrawable(android.R.drawable.sym_def_app_icon));
            }
        }

        if (!versionNameCache.containsKey(packageName) || !versionCodeCache.containsKey(packageName)) {
            try {
                PackageInfo info = packageManager.getPackageInfo(packageName, 0);

                versionNameCache.put(packageName, info.versionName != null ? info.versionName : "N/A");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    versionCodeCache.put(packageName, info.getLongVersionCode());
                } else {
                    versionCodeCache.put(packageName, (long) info.versionCode);
                }
            } catch (Exception e) {
                versionNameCache.put(packageName, "N/A");
                versionCodeCache.put(packageName, 0L);
            }
        }

        getTotalSize(app);
    }

    public String getAppLabel(ApplicationInfo app) {
        String packageName = app.packageName;
        String cached = labelCache.get(packageName);

        if (cached != null) {
            return cached;
        }

        try {
            String label = app.loadLabel(packageManager).toString();
            labelCache.put(packageName, label);
            return label;
        } catch (Exception e) {
            labelCache.put(packageName, packageName);
            return packageName;
        }
    }

    public Drawable getAppIcon(ApplicationInfo app) {
        String packageName = app.packageName;
        Drawable cached = iconCache.get(packageName);

        if (cached != null) {
            return cached;
        }

        return context.getDrawable(android.R.drawable.sym_def_app_icon);
    }

    public String getPackageName(ApplicationInfo app) {
        return app.packageName;
    }

    public String getVersionName(ApplicationInfo app) {
        String packageName = app.packageName;
        String cached = versionNameCache.get(packageName);

        if (cached != null) {
            return cached;
        }

        try {
            PackageInfo info = packageManager.getPackageInfo(packageName, 0);
            String version = info.versionName != null ? info.versionName : "N/A";

            versionNameCache.put(packageName, version);

            if (!versionCodeCache.containsKey(packageName)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    versionCodeCache.put(packageName, info.getLongVersionCode());
                } else {
                    versionCodeCache.put(packageName, (long) info.versionCode);
                }
            }

            return version;
        } catch (Exception e) {
            versionNameCache.put(packageName, "N/A");
            return "N/A";
        }
    }

    public long getVersionCode(ApplicationInfo app) {
        String packageName = app.packageName;
        Long cached = versionCodeCache.get(packageName);

        if (cached != null) {
            return cached;
        }

        try {
            PackageInfo info = packageManager.getPackageInfo(packageName, 0);
            long versionCode;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = info.getLongVersionCode();
            } else {
                versionCode = info.versionCode;
            }

            versionCodeCache.put(packageName, versionCode);

            if (!versionNameCache.containsKey(packageName)) {
                versionNameCache.put(packageName, info.versionName != null ? info.versionName : "N/A");
            }

            return versionCode;
        } catch (Exception e) {
            versionCodeCache.put(packageName, 0L);
            return 0;
        }
    }

    public int getMinSdk(ApplicationInfo app) {
        return app.minSdkVersion;
    }

    public int getTargetSdk(ApplicationInfo app) {
        return app.targetSdkVersion;
    }

    public long getBaseSize(ApplicationInfo app) {
        if (app.sourceDir == null) {
            return 0;
        }

        return new File(app.sourceDir).length();
    }

    public String getFormattedBaseSize(ApplicationInfo app) {
        return formatSize(getBaseSize(app));
    }

    public int getSplitCount(ApplicationInfo app) {
        return app.splitSourceDirs != null ? app.splitSourceDirs.length : 0;
    }

    public long getSplitSize(ApplicationInfo app) {
        long size = 0;

        if (app.splitSourceDirs != null) {
            for (String splitPath : app.splitSourceDirs) {
                size += new File(splitPath).length();
            }
        }

        return size;
    }

    public String getFormattedSplitSize(ApplicationInfo app) {
        return formatSize(getSplitSize(app));
    }

    public long getTotalSize(ApplicationInfo app) {
        String packageName = app.packageName;
        Long cachedSize = totalSizeCache.get(packageName);

        if (cachedSize != null) {
            return cachedSize;
        }

        long size = 0;

        if (app.sourceDir != null) {
            size += new File(app.sourceDir).length();
        }

        if (app.splitSourceDirs != null) {
            for (String splitPath : app.splitSourceDirs) {
                size += new File(splitPath).length();
            }
        }

        totalSizeCache.put(packageName, size);

        return size;
    }

    public String getFormattedSize(ApplicationInfo app) {
        return formatSize(getTotalSize(app));
    }

    public static String formatSize(long bytes) {
        if (bytes <= 0) {
            return "0 MB";
        }

        if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        }

        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
        }

        return String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public void extract(ApplicationInfo app) {
        final String appLabel = getAppLabel(app);

        if (listener != null) {
            mainHandler.post(() -> listener.onAppExtractionStart(context.getString(R.string.extracting_app, appLabel)));
        }

        new Thread(() -> {
            try {
                String cleanName = appLabel.replaceAll("[\\\\/:*?\"<>|]", "").trim();

                if (cleanName.isEmpty()) {
                    cleanName = app.packageName;
                }

                String outputFileName = cleanName + ".apks";
                File outputFile = new File(context.getCacheDir(), outputFileName);
                int splitCount = getSplitCount(app);

                try (FileOutputStream fos = new FileOutputStream(outputFile);
                     ZipOutputStream zos = new ZipOutputStream(fos)) {

                    byte[] buffer = new byte[8192];

                    addApk(zos, new File(app.sourceDir), "base.apk", buffer);

                    if (app.splitSourceDirs != null) {
                        for (String splitPath : app.splitSourceDirs) {
                            File splitFile = new File(splitPath);
                            addApk(zos, splitFile, splitFile.getName(), buffer);
                        }
                    }
                }

                if (listener != null) {
                    mainHandler.post(() -> listener.onAppExtractionSuccess(outputFile, outputFileName, splitCount));
                }

            } catch (Exception e) {
                e.printStackTrace();

                String error = e.getMessage() != null
                        ? e.getMessage()
                        : context.getString(R.string.unknown_extraction_error);

                if (listener != null) {
                    mainHandler.post(() -> listener.onAppExtractionFailed(error));
                }
            }
        }).start();
    }

    private void addApk(ZipOutputStream zos, File apkFile, String entryName, byte[] buffer) throws Exception {
        if (!apkFile.exists()) {
            throw new Exception(context.getString(R.string.apk_file_not_found, apkFile.getAbsolutePath()));
        }

        try (InputStream input = new FileInputStream(apkFile)) {
            zos.putNextEntry(new ZipEntry(entryName));

            int read;

            while ((read = input.read(buffer)) != -1) {
                zos.write(buffer, 0, read);
            }

            zos.closeEntry();
        }
    }

    public interface OnAppsLoadedListener {
        void onAppsLoaded(InstalledAppsAdapter adapter);
    }

    public void showInstalledApps(
            RecyclerView skeletonRecyclerView,
            RecyclerView recyclerView,
            List<ApplicationInfo> apps,
            InstalledAppsAdapter.OnAppClickListener clickListener,
            OnAppsLoadedListener loadedListener
    ) {
        skeletonRecyclerView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        skeletonRecyclerView.setAdapter(
                new InstalledAppsSkeletonAdapter(
                        Math.max(5, Math.min(apps.size(), 8))
                )
        );

        preloadApps(apps, () -> {
            InstalledAppsAdapter adapter =
                    new InstalledAppsAdapter(
                            context,
                            apps,
                            this,
                            clickListener
                    );

            recyclerView.setAdapter(adapter);

            skeletonRecyclerView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            if (loadedListener != null) {
                loadedListener.onAppsLoaded(adapter);
            }
        });
    }
    public static class InstalledAppsAdapter extends RecyclerView.Adapter<InstalledAppsAdapter.ViewHolder> {

        public interface OnAppClickListener {
            void onAppClick(ApplicationInfo app);
        }

        private final Context context;
        private final List<ApplicationInfo> apps;
        private final InstalledAppsManager manager;
        private final OnAppClickListener listener;
        private final List<ApplicationInfo> originalApps;
        private final int surfaceColor;
        private final int surfaceVariantColor;
        private RecyclerView recyclerView;

        private int expandedPosition = RecyclerView.NO_POSITION;
        private int selectedPosition = RecyclerView.NO_POSITION;

        public InstalledAppsAdapter(
                Context context,
                List<ApplicationInfo> apps,
                InstalledAppsManager manager,
                OnAppClickListener listener
        ) {
            this.context = context;
            this.apps = apps;
            this.originalApps = new ArrayList<>(apps);
            this.manager = manager;
            this.listener = listener;

            TypedValue surface = new TypedValue();
            TypedValue surfaceVariant = new TypedValue();

            context.getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorSurface,
                    surface,
                    true
            );

            context.getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorSurfaceVariant,
                    surfaceVariant,
                    true
            );

            this.surfaceColor = surface.data;
            this.surfaceVariantColor = surfaceVariant.data;
        }

        public ApplicationInfo getSelectedApp() {
            if (selectedPosition == RecyclerView.NO_POSITION) {
                return null;
            }

            if (selectedPosition < 0 || selectedPosition >= apps.size()) {
                return null;
            }

            return apps.get(selectedPosition);
        }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (recyclerView == null) recyclerView = (RecyclerView) parent;
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_installed_app, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
            ApplicationInfo app = apps.get(position);

            holder.appName.setText(manager.getAppLabel(app));
            holder.appIcon.setImageDrawable(manager.getAppIcon(app));
            holder.appSize.setText(manager.getFormattedSize(app));
            holder.appSplits.setText(manager.getSplitCount(app) + " " + context.getString(R.string.splits));

            boolean expanded = position == expandedPosition;
            boolean selected = position == selectedPosition;

            if (holder.cardItem != null) {
                holder.cardItem.setCardBackgroundColor(selected ? surfaceVariantColor : surfaceColor);
            }

            Utils.applyLayoutTransition((ViewGroup) holder.itemView);
            holder.expandContent.setVisibility(expanded ? View.VISIBLE : View.GONE);

            if (expanded) {
                holder.infoPackage.setText(context.getString(R.string.info_package, manager.getPackageName(app)));
                holder.infoVersion.setText(context.getString(R.string.info_version, manager.getVersionName(app)));
                holder.infoVersionCode.setText(context.getString(R.string.info_version_code, manager.getVersionCode(app)));
                holder.infoMinSdk.setText(context.getString(R.string.info_min_sdk, manager.getMinSdk(app)));
                holder.infoTargetSdk.setText(context.getString(R.string.info_target_sdk, manager.getTargetSdk(app)));
                holder.infoBaseSize.setText(context.getString(R.string.info_base_apk, manager.getFormattedBaseSize(app)));
                holder.infoTotalSize.setText(context.getString(R.string.info_total_size, manager.getFormattedSize(app)));

                holder.splitList.removeAllViews();

                if (app.splitSourceDirs != null && app.splitSourceDirs.length > 0) {
                    for (String splitPath : app.splitSourceDirs) {
                        File splitFile = new File(splitPath);
                        TextView splitView = new TextView(context);

                        splitView.setText(context.getString(R.string.info_split, splitFile.getName(), InstalledAppsManager.formatSize(splitFile.length())));
                        splitView.setTextColor(context.getColor(com.google.android.material.R.color.material_on_surface_emphasis_medium));
                        splitView.setTextSize(12);

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        params.topMargin = 5;
                        splitView.setLayoutParams(params);

                        holder.splitList.addView(splitView);
                    }
                }
            }

            holder.itemView.setOnClickListener(v -> {
                int oldExpandedPosition = expandedPosition;
                int oldSelectedPosition = selectedPosition;

                if (oldExpandedPosition == position) {
                    expandedPosition = RecyclerView.NO_POSITION;
                    selectedPosition = RecyclerView.NO_POSITION;

                    notifyItemChanged(position);

                    if (listener != null) {
                        listener.onAppClick(null);
                    }

                    return;
                }

                expandedPosition = position;
                selectedPosition = position;

                if (oldExpandedPosition != RecyclerView.NO_POSITION
                        && oldExpandedPosition != position) {
                    notifyItemChanged(oldExpandedPosition);
                }

                if (oldSelectedPosition != RecyclerView.NO_POSITION
                        && oldSelectedPosition != position
                        && oldSelectedPosition != oldExpandedPosition) {
                    notifyItemChanged(oldSelectedPosition);
                }

                notifyItemChanged(position);

                if (listener != null) {
                    listener.onAppClick(app);
                }

                recyclerView.postDelayed(() -> {
                    if (expandedPosition != position) {
                        return;
                    }

                    int[] appNameLocation = new int[2];
                    int[] recyclerLocation = new int[2];

                    holder.appName.getLocationOnScreen(appNameLocation);
                    recyclerView.getLocationOnScreen(recyclerLocation);

                    int recyclerTop = recyclerLocation[1];
                    int appNameTop = appNameLocation[1];

                    int distance = (appNameTop - recyclerTop) - 20;

                    if (distance != 0) {
                        recyclerView.smoothScrollBy(0, distance);
                    }
                }, 300);
            });;
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }

        public void filter(String query) {
            apps.clear();

            if (query == null || query.trim().isEmpty()) {
                apps.addAll(originalApps);
            } else {
                String lowerQuery = query.toLowerCase(Locale.ROOT);

                for (ApplicationInfo app : originalApps) {
                    String appName = manager.getAppLabel(app).toLowerCase(Locale.ROOT);
                    String packageName = app.packageName.toLowerCase(Locale.ROOT);

                    if (appName.contains(lowerQuery) || packageName.contains(lowerQuery)) {
                        apps.add(app);
                    }
                }
            }

            expandedPosition = RecyclerView.NO_POSITION;
            selectedPosition = RecyclerView.NO_POSITION;

            notifyDataSetChanged();

            if (listener != null) {
                listener.onAppClick(null);
            }
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            MaterialCardView cardItem;
            ImageView appIcon;
            TextView appName;
            TextView appSize;
            MaterialButton appSplits;
            View expandContent;
            TextView infoPackage;
            TextView infoVersion;
            TextView infoVersionCode;
            TextView infoMinSdk;
            TextView infoTargetSdk;
            TextView infoBaseSize;
            TextView infoTotalSize;
            LinearLayout splitList;

            ViewHolder(@NonNull View itemView) {
                super(itemView);

                cardItem = itemView instanceof MaterialCardView ? (MaterialCardView) itemView : itemView.findViewById(R.id.cardItem);
                appIcon = itemView.findViewById(R.id.appIcon);
                appName = itemView.findViewById(R.id.appName);
                appSize = itemView.findViewById(R.id.pkgSize);
                appSplits = itemView.findViewById(R.id.btnSplits);
                expandContent = itemView.findViewById(R.id.expandContent);
                infoPackage = itemView.findViewById(R.id.infoPackage);
                infoVersion = itemView.findViewById(R.id.infoVersion);
                infoVersionCode = itemView.findViewById(R.id.infoVersionCode);
                infoMinSdk = itemView.findViewById(R.id.infoMinSdk);
                infoTargetSdk = itemView.findViewById(R.id.infoTargetSdk);
                infoBaseSize = itemView.findViewById(R.id.infoBaseSize);
                infoTotalSize = itemView.findViewById(R.id.infoTotalSize);
                splitList = itemView.findViewById(R.id.splitList);
            }

        }
    }

    public static class InstalledAppsSkeletonAdapter
            extends RecyclerView.Adapter<InstalledAppsSkeletonAdapter.ViewHolder> {

        private final int itemCount;

        public InstalledAppsSkeletonAdapter(int itemCount) {
            this.itemCount = itemCount;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType
        ) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(
                            R.layout.item_installed_skeleton,
                            parent,
                            false
                    );

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull ViewHolder holder,
                int position
        ) {
            holder.startShimmer();
        }

        @Override
        public void onViewAttachedToWindow(
                @NonNull ViewHolder holder
        ) {
            super.onViewAttachedToWindow(holder);
            holder.startShimmer();
        }

        @Override
        public void onViewDetachedFromWindow(
                @NonNull ViewHolder holder
        ) {
            holder.stopShimmer();
            super.onViewDetachedFromWindow(holder);
        }

        @Override
        public void onViewRecycled(
                @NonNull ViewHolder holder
        ) {
            holder.stopShimmer();
            super.onViewRecycled(holder);
        }

        @Override
        public int getItemCount() {
            return itemCount;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            private final MaterialCardView skeletonIcon;
            private final MaterialCardView skeletonName;
            private final MaterialCardView skeletonSize;
            private final MaterialCardView skeletonSplits;

            private ValueAnimator animator;

            ViewHolder(@NonNull View itemView) {
                super(itemView);

                skeletonIcon = itemView.findViewById(R.id.skeletonIcon);
                skeletonName = itemView.findViewById(R.id.skeletonName);
                skeletonSize = itemView.findViewById(R.id.skeletonSize);
                skeletonSplits = itemView.findViewById(R.id.skeletonSplits);
            }

            void startShimmer() {
                stopShimmer();

                Context context = itemView.getContext();

                TypedValue surfaceVariantValue = new TypedValue();
                TypedValue surfaceValue = new TypedValue();

                context.getTheme().resolveAttribute(
                        com.google.android.material.R.attr.colorSurfaceVariant,
                        surfaceVariantValue,
                        true
                );

                context.getTheme().resolveAttribute(
                        com.google.android.material.R.attr.colorSurface,
                        surfaceValue,
                        true
                );

                int surfaceVariant = surfaceVariantValue.data;
                int surface = surfaceValue.data;

                ShimmerDrawable iconDrawable =
                        new ShimmerDrawable(
                                24f,
                                surfaceVariant,
                                surface
                        );

                ShimmerDrawable nameDrawable =
                        new ShimmerDrawable(
                                7f,
                                surfaceVariant,
                                surface
                        );

                ShimmerDrawable sizeDrawable =
                        new ShimmerDrawable(
                                6f,
                                surfaceVariant,
                                surface
                        );

                ShimmerDrawable splitsDrawable =
                        new ShimmerDrawable(
                                12f,
                                surfaceVariant,
                                surface
                        );

                skeletonIcon.setBackground(iconDrawable);
                skeletonName.setBackground(nameDrawable);
                skeletonSize.setBackground(sizeDrawable);
                skeletonSplits.setBackground(splitsDrawable);

                ShimmerDrawable[] drawables = {
                        iconDrawable,
                        nameDrawable,
                        sizeDrawable,
                        splitsDrawable
                };

                animator = ValueAnimator.ofFloat(-1f, 1f);
                animator.setDuration(1200);
                animator.setInterpolator(new LinearInterpolator());
                animator.setRepeatCount(ValueAnimator.INFINITE);

                animator.addUpdateListener(animation -> {

                    float progress =
                            (float) animation.getAnimatedValue();

                    for (ShimmerDrawable drawable : drawables) {
                        drawable.setProgress(progress);
                    }
                });

                animator.start();
            }

            void stopShimmer() {
                if (animator != null) {
                    animator.cancel();
                    animator.removeAllUpdateListeners();
                    animator = null;
                }
            }
        }

        private static class ShimmerDrawable extends Drawable {

            private final Paint paint =
                    new Paint(Paint.ANTI_ALIAS_FLAG);

            private final float cornerRadius;
            private final int surfaceVariant;
            private final int surface;

            private float progress;

            ShimmerDrawable(
                    float cornerRadius,
                    int surfaceVariant,
                    int surface
            ) {
                this.cornerRadius = cornerRadius;
                this.surfaceVariant = surfaceVariant;
                this.surface = surface;
            }

            void setProgress(float progress) {
                this.progress = progress;
                invalidateSelf();
            }

            @Override
            public void draw(@NonNull Canvas canvas) {

                float width = getBounds().width();
                float height = getBounds().height();

                if (width <= 0 || height <= 0) {
                    return;
                }

                float diagonal =
                        (float) Math.sqrt(
                                width * width +
                                        height * height
                        );

                float offset =
                        progress * diagonal * 1.5f;

                float centerX =
                        width / 2f + offset;

                float centerY =
                        height / 2f + offset;

                float half =
                        diagonal * 0.45f;

                LinearGradient gradient =
                        new LinearGradient(
                                centerX - half,
                                centerY - half,
                                centerX + half,
                                centerY + half,
                                new int[]{
                                        surfaceVariant,
                                        surface,
                                        surfaceVariant
                                },
                                new float[]{
                                        0f,
                                        0.5f,
                                        1f
                                },
                                Shader.TileMode.CLAMP
                        );

                paint.setShader(gradient);

                canvas.drawRoundRect(
                        0,
                        0,
                        width,
                        height,
                        cornerRadius,
                        cornerRadius,
                        paint
                );

                paint.setShader(null);
            }

            @Override
            protected void onBoundsChange(
                    android.graphics.Rect bounds
            ) {
                super.onBoundsChange(bounds);
                invalidateSelf();
            }

            @Override
            public void setAlpha(int alpha) {
                paint.setAlpha(alpha);
                invalidateSelf();
            }

            @Override
            public void setColorFilter(
                    android.graphics.ColorFilter colorFilter
            ) {
                paint.setColorFilter(colorFilter);
                invalidateSelf();
            }

            @Override
            public int getOpacity() {
                return android.graphics.PixelFormat.TRANSLUCENT;
            }
        }
    }
}