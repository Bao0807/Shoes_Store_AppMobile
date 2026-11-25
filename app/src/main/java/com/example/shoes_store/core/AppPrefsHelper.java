package com.example.shoes_store.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Lưu trữ cài đặt nhẹ trên máy bằng SharedPreferences (email đăng nhập gần nhất, lịch sử tìm kiếm).
 */
public final class AppPrefsHelper {

    private static final String TAG = "AppPrefsHelper";
    private static final String PREF_NAME = "shoes_store_prefs";
    private static final String KEY_LAST_EMAIL = "last_login_email";
    private static final String KEY_RECENT_SEARCHES = "recent_searches_json";
    private static final int MAX_RECENT_SEARCHES = 8;

    private static SharedPreferences prefs;

    private AppPrefsHelper() {
    }

    public static void init(@NonNull Context context) {
        if (prefs == null) {
            prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    @NonNull
    public static String getLastLoginEmail() {
        if (prefs == null) {
            return "";
        }
        return prefs.getString(KEY_LAST_EMAIL, "");
    }

    public static void saveLastLoginEmail(@Nullable String email) {
        if (prefs == null) {
            return;
        }
        String trimmed = email != null ? email.trim() : "";
        prefs.edit().putString(KEY_LAST_EMAIL, trimmed).apply();
    }

    @NonNull
    public static List<String> getRecentSearches() {
        List<String> list = new ArrayList<>();
        if (prefs == null) {
            return list;
        }
        String raw = prefs.getString(KEY_RECENT_SEARCHES, null);
        if (TextUtils.isEmpty(raw)) {
            return list;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                String term = array.optString(i, "").trim();
                if (!term.isEmpty()) {
                    list.add(term);
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Corrupted recent searches JSON: " + e.getMessage());
            // Trả danh sách rỗng nếu JSON cũ bị lỗi
        }
        return list;
    }

    @NonNull
    public static List<String> addRecentSearch(@NonNull String query) {
        List<String> list = getRecentSearches();
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return list;
        }
        list.remove(trimmed);
        list.add(0, trimmed);
        if (list.size() > MAX_RECENT_SEARCHES) {
            list = new ArrayList<>(list.subList(0, MAX_RECENT_SEARCHES));
        }
        replaceRecentSearches(list);
        return list;
    }

    @NonNull
    public static List<String> removeRecentSearch(@NonNull String query) {
        List<String> list = getRecentSearches();
        if (list.remove(query)) {
            replaceRecentSearches(list);
        }
        return list;
    }

    public static void replaceRecentSearches(@Nullable List<String> searches) {
        if (prefs == null) {
            return;
        }
        JSONArray array = new JSONArray();
        if (searches != null) {
            int limit = Math.min(MAX_RECENT_SEARCHES, searches.size());
            for (int i = 0; i < limit; i++) {
                String item = searches.get(i);
                if (!TextUtils.isEmpty(item)) {
                    array.put(item.trim());
                }
            }
        }
        prefs.edit().putString(KEY_RECENT_SEARCHES, array.toString()).apply();
    }
}
