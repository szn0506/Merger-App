package com.szn.merger;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.ArrayList;
import java.util.List;

public class ThemeManager {

    private static final String KEY_THEME = "selected_mode";
    private static final String KEY_MATERIAL_YOU = "material_you_enabled";
    private static final String KEY_PURE_BLACK = "pure_black_enabled";
    private static final String KEY_LANGUAGE = "selected_language";
    private static final int[] MODE_MAP = {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
    };

    private static final List<Activity> activities = new ArrayList<>();

    public static void register(Activity activity) {
        if (!activities.contains(activity)) {
            activities.add(activity);
        }
    }

    private static void recreateAll() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            for (Activity activity : activities) {
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    activity.recreate();
                }
            }
        }, 200);
    }

    public static void applyTheme(Activity activity) {
        PrefsManager prefs = PrefsManager.getInstance(activity);

        int mode = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        boolean materialYou = prefs.getBoolean(KEY_MATERIAL_YOU, false);
        boolean pureBlack = prefs.getBoolean(KEY_PURE_BLACK, false);

        // Check if the current screen state is actually in dark/night mode
        boolean isDark = !isCurrentThemeLight(activity, prefs);

        if (materialYou && pureBlack && isDark) {
            // IF BOTH ARE ACTIVE (At Night) -> Use AMOLED-ified Dynamic Theme
            activity.setTheme(R.style.AppTheme_Dynamic_PureBlack);
        } else if (materialYou) {
            // Only Material You is active
            activity.setTheme(R.style.AppTheme_Dynamic);
        } else if (pureBlack && isDark) {
            // Only standard Pure Black is active
            activity.setTheme(R.style.AppTheme_PureBlack);
        } else {
            // Default standard mode
            activity.setTheme(R.style.AppTheme);
        }

        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static void setupMaterialYouSwitch(
            Activity activity,
            CustomSwitchItem item
    ) {
        if (item == null) return;

        PrefsManager prefs =
                PrefsManager.getInstance(activity);

        item.setChecked(
                prefs.getBoolean(KEY_MATERIAL_YOU, false)
        );

        item.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.saveBoolean(KEY_MATERIAL_YOU, isChecked);
            recreateAll();
        });
    }

    public static void setupPureBlackSwitch(
            Activity activity,
            CustomSwitchItem item
    ) {
        if (item == null) return;

        PrefsManager prefs = PrefsManager.getInstance(activity);

        // Retrieve original saved state
        boolean isPureBlackChecked = prefs.getBoolean(KEY_PURE_BLACK, false);

        if (isCurrentThemeLight(activity, prefs)) {
            item.setEnabled(false);
            item.setAlpha(0.4f);
            item.setChecked(false);
            item.setFocusable(false);
        } else {
            item.setEnabled(true);
            item.setAlpha(1.0f);
            item.setFocusable(true);
            item.setChecked(isPureBlackChecked); // FIX: Only set original checked state if currently in Dark Mode
        }

        item.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.saveBoolean(KEY_PURE_BLACK, isChecked);
            recreateAll();
        });
    }

    private static boolean isCurrentThemeLight(Activity activity, PrefsManager prefs) {
        int mode = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            return true;
        } else if (mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            int currentFollowSystemMode = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return currentFollowSystemMode == Configuration.UI_MODE_NIGHT_NO;
        }
        return false;
    }

    public static void setupThemePopup(
            Activity activity,
            View themeCard,
            TextView currentThemeText
    ) {
        if (themeCard == null || currentThemeText == null)
            return;

        PrefsManager prefs = PrefsManager.getInstance(activity);

        int currentMode = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        if (currentMode == AppCompatDelegate.MODE_NIGHT_NO) {
            currentThemeText.setText(activity.getString(R.string.theme_light));
        } else if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            currentThemeText.setText(activity.getString(R.string.theme_dark));
        } else {
            currentThemeText.setText(activity.getString(R.string.theme_system));
        }

        themeCard.setOnClickListener(anchor -> {
            View view = activity.getLayoutInflater().inflate(R.layout.theme_sheet, null);

            PopupWindow popup = new PopupWindow(
                    view,
                    (int) (220 * activity.getResources().getDisplayMetrics().density),
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );

            view.findViewById(R.id.check_system).setVisibility(currentMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.check_light).setVisibility(currentMode == AppCompatDelegate.MODE_NIGHT_NO ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.check_dark).setVisibility(currentMode == AppCompatDelegate.MODE_NIGHT_YES ? View.VISIBLE : View.GONE);

            popup.showAsDropDown(anchor, 0, 4, Gravity.END);

            TextView systemTv = view.findViewById(R.id.system_text);
            TextView lightTv = view.findViewById(R.id.light_text);
            TextView darkTv = view.findViewById(R.id.dark_text);

            view.findViewById(R.id.system).setOnClickListener(v -> {
                currentThemeText.setText(systemTv.getText().toString());
                updateTheme(activity, popup, MODE_MAP[0], prefs); // FIX: Pass activity parameter
            });

            view.findViewById(R.id.light).setOnClickListener(v -> {
                currentThemeText.setText(lightTv.getText().toString());
                updateTheme(activity, popup, MODE_MAP[1], prefs); // FIX: Pass activity parameter
            });

            view.findViewById(R.id.dark).setOnClickListener(v -> {
                currentThemeText.setText(darkTv.getText().toString());
                updateTheme(activity, popup, MODE_MAP[2], prefs); // FIX: Pass activity parameter
            });
        });
    }

    private static void updateTheme(
            Activity activity,
            PopupWindow popup,
            int mode,
            PrefsManager prefs
    ) {
        prefs.saveInt(KEY_THEME, mode);

        // FIX: If switching modes results in a Light Theme (either forced Light or Follow System during daytime), force clear the Pure Black state
        if (mode == AppCompatDelegate.MODE_NIGHT_NO || isCurrentThemeLight(activity, prefs)) {
            prefs.saveBoolean(KEY_PURE_BLACK, false);
        }

        AppCompatDelegate.setDefaultNightMode(mode);
        popup.dismiss();
        recreateAll();
    }

    public static int getLanguage(Activity activity) {
        return PrefsManager.getInstance(activity).getInt(KEY_LANGUAGE, 0);
    }

    public static void applyLanguage(Activity activity) {

        int index = getLanguage(activity);

        String[] codes = activity.getResources()
                .getStringArray(R.array.language_codes);

        if (index == 0) {
            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.getEmptyLocaleList()
            );
        } else {
            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(codes[index])
            );
        }
    }

    public static void setLanguage(Activity activity, int index) {
        PrefsManager prefs = PrefsManager.getInstance(activity);

        prefs.saveInt(KEY_LANGUAGE, index);
        applyLanguage(activity);

        recreateAll();
    }
}