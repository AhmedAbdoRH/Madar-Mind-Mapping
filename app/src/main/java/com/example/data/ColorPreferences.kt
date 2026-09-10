package com.example.data

import android.content.Context
import android.content.SharedPreferences

class ColorPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("orbit_mind_color_prefs", Context.MODE_PRIVATE)

    fun getRecentColors(): List<String> {
        val raw = prefs.getString(KEY_RECENT_COLORS, null)
        if (raw.isNullOrBlank()) {
            return DEFAULT_RECENTS
        }
        val list = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return if (list.isEmpty()) DEFAULT_RECENTS else list
    }

    fun recordColorUsed(colorHex: String) {
        if (colorHex.isBlank()) return
        val current = getRecentColors().toMutableList()
        current.removeAll { it.equals(colorHex, ignoreCase = true) }
        current.add(0, colorHex)
        val trimmed = current.take(16)
        prefs.edit().putString(KEY_RECENT_COLORS, trimmed.joinToString(",")).apply()
    }

    fun getDefaultPrimaryColor(): String {
        return prefs.getString(KEY_DEFAULT_PRIMARY_COLOR, "#4F5B92") ?: "#4F5B92"
    }

    fun setDefaultPrimaryColor(colorHex: String) {
        if (colorHex.isNotBlank()) {
            prefs.edit().putString(KEY_DEFAULT_PRIMARY_COLOR, colorHex).apply()
            recordColorUsed(colorHex)
        }
    }

    fun getDefaultNodeColor(mapId: Long? = null): String {
        if (mapId != null) {
            val mapSpecific = prefs.getString("key_map_default_node_color_$mapId", null)
            if (!mapSpecific.isNullOrBlank()) return mapSpecific
        }
        return prefs.getString(KEY_DEFAULT_NODE_COLOR, "#4F5B92") ?: "#4F5B92"
    }

    fun setDefaultNodeColor(colorHex: String, mapId: Long? = null) {
        if (colorHex.isNotBlank()) {
            val editor = prefs.edit()
            editor.putString(KEY_DEFAULT_NODE_COLOR, colorHex)
            if (mapId != null) {
                editor.putString("key_map_default_node_color_$mapId", colorHex)
            }
            editor.apply()
            recordColorUsed(colorHex)
        }
    }

    fun isDefaultNodeColor(colorHex: String, mapId: Long? = null): Boolean {
        val def = getDefaultNodeColor(mapId)
        return colorHex.equals(def, ignoreCase = true)
    }

    companion object {
        private const val KEY_RECENT_COLORS = "key_recent_colors"
        private const val KEY_DEFAULT_PRIMARY_COLOR = "key_default_primary_color"
        private const val KEY_DEFAULT_NODE_COLOR = "key_default_node_color"
        val DEFAULT_RECENTS = listOf(
            "#4F5B92",
            "#6750A4",
            "#006874",
            "#E8710A",
            "#FF6B6B",
            "#1A73E8",
            "#D9E2FF",
            "#C4EED0"
        )
    }
}
