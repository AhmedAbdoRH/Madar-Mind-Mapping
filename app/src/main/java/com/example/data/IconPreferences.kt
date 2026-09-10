package com.example.data

import android.content.Context
import android.content.SharedPreferences

class IconPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("orbit_mind_icon_prefs", Context.MODE_PRIVATE)

    fun getRecentIcons(): List<String> {
        val raw = prefs.getString(KEY_RECENT_ICONS, null)
        if (raw.isNullOrBlank()) {
            return DEFAULT_RECENTS
        }
        val list = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return if (list.isEmpty()) DEFAULT_RECENTS else list
    }

    fun recordIconUsed(iconKey: String) {
        if (iconKey.isBlank() || iconKey.equals("none", ignoreCase = true)) return
        val current = getRecentIcons().toMutableList()
        current.remove(iconKey)
        current.add(0, iconKey)
        val trimmed = current.take(20)
        prefs.edit().putString(KEY_RECENT_ICONS, trimmed.joinToString(",")).apply()
    }

    companion object {
        private const val KEY_RECENT_ICONS = "key_recent_icons"
        val DEFAULT_RECENTS = listOf(
            "lightbulb",
            "rocket_launch",
            "star",
            "psychology",
            "favorite",
            "checklist",
            "palette",
            "work",
            "flag",
            "explore"
        )
    }
}
