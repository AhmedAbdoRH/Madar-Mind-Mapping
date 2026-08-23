package com.example.data

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    val displayName: String
        get() = when (this) {
            LIGHT -> "Light"
            DARK -> "Dark"
            SYSTEM -> "System"
        }

    val arabicName: String
        get() = when (this) {
            LIGHT -> "الوضع الفاتح"
            DARK -> "الوضع الليلي"
            SYSTEM -> "تلقائي (حسب النظام)"
        }
}

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("orbit_mind_theme_prefs", Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(saved ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
    }
}
