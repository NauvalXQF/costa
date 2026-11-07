package com.opankcs.costa.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("costa", Context.MODE_PRIVATE)

    var token: String
        get() = sp.getString("token", "") ?: ""
        set(value) {
            sp.edit().putString("token", value.trim()).apply()
        }

    var baseUrl: String
        get() = sp.getString("base_url", "https://costa.opankcs.com/") ?: "https://costa.opankcs.com/"
        set(value) {
            val normalized = value.trim().let { if (it.endsWith("/")) it else "$it/" }
            sp.edit().putString("base_url", normalized).apply()
        }

    var userName: String
        get() = sp.getString("user_name", "") ?: ""
        set(value) {
            sp.edit().putString("user_name", value.trim()).apply()
        }

    var userEmail: String
        get() = sp.getString("user_email", "") ?: ""
        set(value) {
            sp.edit().putString("user_email", value.trim()).apply()
        }

    var notifyDailyReminder: Boolean
        get() = sp.getBoolean("notify_daily_reminder", true)
        set(value) {
            sp.edit().putBoolean("notify_daily_reminder", value).apply()
        }

    var notifyBudget: Boolean
        get() = sp.getBoolean("notify_budget", true)
        set(value) {
            sp.edit().putBoolean("notify_budget", value).apply()
        }

    var notifyWeeklyRecap: Boolean
        get() = sp.getBoolean("notify_weekly_recap", true)
        set(value) {
            sp.edit().putBoolean("notify_weekly_recap", value).apply()
        }

    var notifyMonthlyReset: Boolean
        get() = sp.getBoolean("notify_monthly_reset", true)
        set(value) {
            sp.edit().putBoolean("notify_monthly_reset", value).apply()
        }


    var appLanguage: String
        get() = sp.getString("app_language", "system") ?: "system"
        set(value) {
            sp.edit().putString("app_language", value).apply()
        }
    var hapticsEnabled: Boolean
        get() = sp.getBoolean("haptics_enabled", true)
        set(value) {
            sp.edit().putBoolean("haptics_enabled", value).apply()
        }

    var dailyWarningState: String
        get() = sp.getString("daily_warning_state", "") ?: ""
        set(value) {
            sp.edit().putString("daily_warning_state", value).apply()
        }

    var monthlyWarningState: String
        get() = sp.getString("monthly_warning_state", "") ?: ""
        set(value) {
            sp.edit().putString("monthly_warning_state", value).apply()
        }

    var foodWarningState: String
        get() = sp.getString("food_warning_state", "") ?: ""
        set(value) {
            sp.edit().putString("food_warning_state", value).apply()
        }

    fun clearSession() {
        sp.edit().remove("token").remove("user_name").remove("user_email").apply()
    }
}
