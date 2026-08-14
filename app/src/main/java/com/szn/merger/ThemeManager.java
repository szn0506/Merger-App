package com.szn.merger;

import android.app.Activity;
import android.content.Intent;
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

    private static final int[] MODE_MAP = {AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES};
    private static final List<Activity> activities = new ArrayList<>();

    public static void register(Activity activity) {
        try {
            if (!activities.contains(activity)) activities.add(activity);
        } catch (Throwable e) {
        }
    }

    private static void recreateAll() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            List<Activity> snapshot =
                    new ArrayList<>(activities);

            for (Activity activity : snapshot) {

                if (activity == null ||
                        activity.isFinishing() ||
                        activity.isDestroyed()) {

                    activities.remove(activity);
                    continue;
                }

                Intent intent =
                        new Intent(
                                activity,
                                activity.getClass()
                        );

                intent.putExtras(activity.getIntent());

                activity.startActivity(intent);

                activity.overridePendingTransition(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                );

                activity.finish();

                activity.overridePendingTransition(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                );
            }

        }, 400);
    }

    public static void applyTheme(Activity activity) {
        try {
            PrefsManager prefs = PrefsManager.getInstance(activity);
            int mode = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            boolean materialYou = prefs.getBoolean(KEY_MATERIAL_YOU, false);
            boolean pureBlack = prefs.getBoolean(KEY_PURE_BLACK, false);
            boolean isDark = !isCurrentThemeLight(activity, prefs);

            if (materialYou && pureBlack && isDark) {
                activity.setTheme(R.style.AppTheme_Dynamic_PureBlack);
            } else if (materialYou) {
                activity.setTheme(R.style.AppTheme_Dynamic);
            } else if (pureBlack && isDark) {
                activity.setTheme(R.style.AppTheme_PureBlack);
            } else {
                activity.setTheme(R.style.AppTheme);
            }

            AppCompatDelegate.setDefaultNightMode(mode);
        } catch (Throwable e) {
        }
    }

    public static void setupMaterialYouSwitch(Activity activity, CustomSwitchItem item) {
        try {
            if (item == null) return;
            PrefsManager prefs = PrefsManager.getInstance(activity);
            boolean checked = prefs.getBoolean(KEY_MATERIAL_YOU, false);
            item.setChecked(checked);
            item.setOnCheckedChangeListener((v, isChecked) -> {
                try {
                    prefs.saveBoolean(KEY_MATERIAL_YOU, isChecked);
                    recreateAll();
                } catch (Throwable e) {
                }
            });
        } catch (Throwable e) {
        }
    }

    public static void setupPureBlackSwitch(Activity activity, CustomSwitchItem item) {
        try {
            if (item == null) return;
            PrefsManager prefs = PrefsManager.getInstance(activity);
            boolean isPureBlackChecked = prefs.getBoolean(KEY_PURE_BLACK, false);
            boolean isLight = isCurrentThemeLight(activity, prefs);

            if (isLight) {
                item.setEnabled(false);
                item.setAlpha(0.4f);
                item.setChecked(false);
                item.setFocusable(false);
            } else {
                item.setEnabled(true);
                item.setAlpha(1.0f);
                item.setFocusable(true);
                item.setChecked(isPureBlackChecked);
            }

            item.setOnCheckedChangeListener((v, isChecked) -> {
                try {
                    prefs.saveBoolean(KEY_PURE_BLACK, isChecked);
                    recreateAll();
                } catch (Throwable e) {
                }
            });
        } catch (Throwable e) {
        }
    }

    private static boolean isCurrentThemeLight(Activity activity, PrefsManager prefs) {
        try {
            int mode = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            int uiMode = activity.getResources().getConfiguration().uiMode;
            int nightMask = uiMode & Configuration.UI_MODE_NIGHT_MASK;

            if (mode == AppCompatDelegate.MODE_NIGHT_NO) return true;
            else if (mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                return nightMask == Configuration.UI_MODE_NIGHT_NO;
            return false;
        } catch (Throwable e) {
            return false;
        }
    }

    public static void setupThemePopup(Activity activity, View themeCard, TextView currentThemeText) {
        try {
            if (themeCard == null || currentThemeText == null) return;
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
                try {
                    View view = activity.getLayoutInflater().inflate(R.layout.theme_sheet, null);
                    View checkSystem = view.findViewById(R.id.check_system);
                    View checkLight = view.findViewById(R.id.check_light);
                    View checkDark = view.findViewById(R.id.check_dark);
                    View system = view.findViewById(R.id.system);
                    View light = view.findViewById(R.id.light);
                    View dark = view.findViewById(R.id.dark);
                    TextView systemTv = view.findViewById(R.id.system_text);
                    TextView lightTv = view.findViewById(R.id.light_text);
                    TextView darkTv = view.findViewById(R.id.dark_text);

                    PopupWindow popup = new PopupWindow(view, (int) (220 * activity.getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT, true);

                    checkSystem.setVisibility(currentMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ? View.VISIBLE : View.GONE);
                    checkLight.setVisibility(currentMode == AppCompatDelegate.MODE_NIGHT_NO ? View.VISIBLE : View.GONE);
                    checkDark.setVisibility(currentMode == AppCompatDelegate.MODE_NIGHT_YES ? View.VISIBLE : View.GONE);

                    popup.showAsDropDown(anchor, 0, 4, Gravity.END);

                    system.setOnClickListener(v -> {
                        currentThemeText.setText(systemTv.getText().toString());
                        updateTheme(activity, popup, MODE_MAP[0], prefs);
                    });

                    light.setOnClickListener(v -> {
                        currentThemeText.setText(lightTv.getText().toString());
                        updateTheme(activity, popup, MODE_MAP[1], prefs);
                    });

                    dark.setOnClickListener(v -> {
                        currentThemeText.setText(darkTv.getText().toString());
                        updateTheme(activity, popup, MODE_MAP[2], prefs);
                    });
                } catch (Throwable e) {
                }
            });
        } catch (Throwable e) {
        }
    }

    private static void updateTheme(Activity activity, PopupWindow popup, int mode, PrefsManager prefs) {
        try {
            prefs.saveInt(KEY_THEME, mode);
            boolean light = isCurrentThemeLight(activity, prefs);
            if (mode == AppCompatDelegate.MODE_NIGHT_NO || light)
                prefs.saveBoolean(KEY_PURE_BLACK, false);

            AppCompatDelegate.setDefaultNightMode(mode);
            popup.dismiss();
            recreateAll();
        } catch (Throwable e) {
        }
    }

    public static int getLanguage(Activity activity) {
        try {
            return PrefsManager.getInstance(activity).getInt(KEY_LANGUAGE, 0);
        } catch (Throwable e) {
            return 0;
        }
    }

    public static void applyLanguage(Activity activity) {
        try {
            int index = getLanguage(activity);
            String[] codes = activity.getResources().getStringArray(R.array.language_codes);

            if (index < 0 || index >= codes.length) return;

            if (index == 0) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(codes[index]));
            }
        } catch (Throwable e) {
        }
    }

    public static void setLanguage(Activity activity, int index) {
        try {
            PrefsManager prefs = PrefsManager.getInstance(activity);
            prefs.saveInt(KEY_LANGUAGE, index);
            applyLanguage(activity);
            recreateAll();
        } catch (Throwable e) {
        }
    }
}