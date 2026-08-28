package com.szn.merger;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class PrefsManager {

    private static PrefsManager instance;
    private final SharedPreferences prefs;

    // Private constructor to ensure Singleton pattern security
    private PrefsManager(Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    // Call PrefsManager.getInstance(context) wherever it is needed
    public static synchronized PrefsManager getInstance(Context context) {
        if (instance == null) {
            instance = new PrefsManager(context);
        }
        return instance;
    }

    // ==================== STORAGE OPERATIONS (WRITE) ====================

    public void saveString(String key, String value) {
        prefs.edit().putString(key, value).commit();
    }

    public void saveInt(String key, int value) {
        prefs.edit().putInt(key, value).commit();
    }

    public void saveBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).commit();
    }

    // ==================== RETRIEVAL OPERATIONS (READ) ====================

    public String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    public void saveStringList(String key, List<String> value) {
        JSONArray array = new JSONArray();

        for (String item : value) {
            array.put(item);
        }

        prefs.edit().putString(key, array.toString()).apply();
    }

    public List<String> getStringList(String key, List<String> defaultValue) {
        String saved = prefs.getString(key, null);

        if (saved == null) {
            return new ArrayList<>(defaultValue);
        }

        try {
            JSONArray array = new JSONArray(saved);
            List<String> result = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                result.add(array.getString(i));
            }

            return result;
        } catch (Exception e) {
            return new ArrayList<>(defaultValue);
        }
    }

    // Removes a specific key
    public void remove(String key) {
        prefs.edit().remove(key).apply();
    }

    // Clears all preference data (e.g., for a settings reset feature)
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}