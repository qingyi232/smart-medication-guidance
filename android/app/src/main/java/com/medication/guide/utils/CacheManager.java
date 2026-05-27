package com.medication.guide.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 离线数据缓存管理器
 * 使用 SharedPreferences 缓存关键数据，在网络不可用时提供离线访问。
 */
public class CacheManager {
    private static final String PREF_NAME = "data_cache";
    private static final String KEY_TODAY_SCHEDULE = "today_schedule";
    private static final String KEY_MEDICATIONS = "medications";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_LATEST_RISK = "latest_risk";
    private static final String KEY_CACHE_TIME_PREFIX = "cache_time_";
    private static final long CACHE_EXPIRY_MS = 30 * 60 * 1000; // 30分钟过期

    private final SharedPreferences prefs;

    public CacheManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ===== 今日服药计划 =====
    public void cacheTodaySchedule(String json) {
        put(KEY_TODAY_SCHEDULE, json);
    }

    public String getTodaySchedule() {
        return get(KEY_TODAY_SCHEDULE);
    }

    // ===== 药品列表 =====
    public void cacheMedications(String json) {
        put(KEY_MEDICATIONS, json);
    }

    public String getMedications() {
        return get(KEY_MEDICATIONS);
    }

    // ===== 用户档案 =====
    public void cacheProfile(String json) {
        put(KEY_PROFILE, json);
    }

    public String getProfile() {
        return get(KEY_PROFILE);
    }

    // ===== 最新风险评估 =====
    public void cacheLatestRisk(String json) {
        put(KEY_LATEST_RISK, json);
    }

    public String getLatestRisk() {
        return get(KEY_LATEST_RISK);
    }

    // ===== 通用方法 =====
    private void put(String key, String json) {
        prefs.edit()
                .putString(key, json)
                .putLong(KEY_CACHE_TIME_PREFIX + key, System.currentTimeMillis())
                .apply();
    }

    private String get(String key) {
        long cacheTime = prefs.getLong(KEY_CACHE_TIME_PREFIX + key, 0);
        if (System.currentTimeMillis() - cacheTime > CACHE_EXPIRY_MS) {
            return null; // 缓存过期
        }
        return prefs.getString(key, null);
    }

    /**
     * 获取缓存（忽略过期时间，用于离线模式）
     */
    public String getForOffline(String key) {
        return prefs.getString(key, null);
    }

    public String getTodayScheduleOffline() {
        return prefs.getString(KEY_TODAY_SCHEDULE, null);
    }

    public String getMedicationsOffline() {
        return prefs.getString(KEY_MEDICATIONS, null);
    }

    /**
     * 检查网络是否可用
     */
    public static boolean isNetworkAvailable(Context context) {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * 清除所有缓存
     */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
