package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.data.model.MindMapEntity
import com.example.data.model.MindNodeEntity

import org.json.JSONArray
import org.json.JSONObject

data class PinnedNodeShortcut(
    val mapId: Long,
    val nodeId: String,
    val nodeTitle: String,
    val mapTitle: String,
    val colorHex: String = "#6366F1",
    val iconName: String = "lightbulb",
    val addedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("mapId", mapId)
            put("nodeId", nodeId)
            put("nodeTitle", nodeTitle)
            put("mapTitle", mapTitle)
            put("colorHex", colorHex)
            put("iconName", iconName)
            put("addedAt", addedAt)
            put("isPinned", isPinned)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): PinnedNodeShortcut {
            return PinnedNodeShortcut(
                mapId = json.optLong("mapId", 0L),
                nodeId = json.optString("nodeId", ""),
                nodeTitle = json.optString("nodeTitle", "عقدة"),
                mapTitle = json.optString("mapTitle", "مدار"),
                colorHex = json.optString("colorHex", "#6366F1"),
                iconName = json.optString("iconName", "lightbulb"),
                addedAt = json.optLong("addedAt", System.currentTimeMillis()),
                isPinned = json.optBoolean("isPinned", false)
            )
        }
    }
}

object ShortcutHelper {
    const val EXTRA_MAP_ID = "EXTRA_MAP_ID"
    const val EXTRA_NODE_ID = "EXTRA_NODE_ID"
    private const val PREFS_NAME = "madar_node_shortcuts"
    private const val KEY_SHORTCUTS = "pinned_shortcuts_list"
    private const val KEY_PINNED_MAPS = "pinned_universe_maps_set"

    fun getPinnedShortcuts(context: Context): List<PinnedNodeShortcut> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_SHORTCUTS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<PinnedNodeShortcut>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(PinnedNodeShortcut.fromJson(obj))
            }
            list.sortedWith(compareByDescending<PinnedNodeShortcut> { it.isPinned }.thenByDescending { it.addedAt })
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isNodePinned(context: Context, mapId: Long, nodeId: String): Boolean {
        return getPinnedShortcuts(context).any { it.mapId == mapId && it.nodeId == nodeId }
    }

    fun togglePinShortcut(context: Context, mapId: Long, nodeId: String): List<PinnedNodeShortcut> {
        val current = getPinnedShortcuts(context).map {
            if (it.mapId == mapId && it.nodeId == nodeId) {
                it.copy(isPinned = !it.isPinned)
            } else it
        }.toMutableList()
        persistShortcuts(context, current)
        return getPinnedShortcuts(context)
    }

    fun getPinnedMapIds(context: Context): Set<Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stringSet = prefs.getStringSet(KEY_PINNED_MAPS, emptySet()) ?: emptySet()
        return stringSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun isMapPinned(context: Context, mapId: Long): Boolean {
        return getPinnedMapIds(context).contains(mapId)
    }

    fun togglePinMap(context: Context, mapId: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(KEY_PINNED_MAPS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val idStr = mapId.toString()
        val isNowPinned: Boolean
        if (currentSet.contains(idStr)) {
            currentSet.remove(idStr)
            isNowPinned = false
        } else {
            currentSet.add(idStr)
            isNowPinned = true
        }
        prefs.edit().putStringSet(KEY_PINNED_MAPS, currentSet).apply()
        return isNowPinned
    }

    fun savePinnedShortcut(
        context: Context,
        map: MindMapEntity?,
        node: MindNodeEntity
    ): List<PinnedNodeShortcut> {
        val current = getPinnedShortcuts(context).toMutableList()
        current.removeAll { it.mapId == node.mapId && it.nodeId == node.id }
        current.add(
            0,
            PinnedNodeShortcut(
                mapId = node.mapId,
                nodeId = node.id,
                nodeTitle = node.title.ifBlank { "عقدة مدارية" },
                mapTitle = map?.title ?: "مدار",
                colorHex = node.colorHex,
                iconName = node.iconName,
                addedAt = System.currentTimeMillis()
            )
        )
        persistShortcuts(context, current)
        return current
    }

    fun removePinnedShortcut(
        context: Context,
        mapId: Long,
        nodeId: String
    ): List<PinnedNodeShortcut> {
        val current = getPinnedShortcuts(context).toMutableList()
        current.removeAll { it.mapId == mapId && it.nodeId == nodeId }
        persistShortcuts(context, current)
        try {
            val shortcutId = "node_shortcut_${mapId}_$nodeId"
            ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(shortcutId))
        } catch (_: Exception) {}
        return current
    }

    private fun persistShortcuts(context: Context, list: List<PinnedNodeShortcut>) {
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it.toJson()) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SHORTCUTS, jsonArray.toString())
            .apply()
    }

    fun isShortcutSupported(context: Context): Boolean {
        return ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    }

    fun createShortcutInfo(
        context: Context,
        mapId: Long,
        nodeId: String,
        nodeTitle: String,
        mapTitle: String,
        colorHex: String,
        iconName: String
    ): ShortcutInfoCompat {
        val shortcutId = "node_shortcut_${mapId}_$nodeId"
        val cleanTitle = nodeTitle.ifBlank { "عقدة مدارية" }
        val cleanMapTitle = mapTitle.ifBlank { "مدار" }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            `package` = context.packageName
            component = android.content.ComponentName(context, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_MAP_ID, mapId)
            putExtra(EXTRA_NODE_ID, nodeId)
        }

        val iconBitmap = generateShortcutBitmap(cleanTitle, colorHex)
        val iconCompat = IconCompat.createWithBitmap(iconBitmap)

        return ShortcutInfoCompat.Builder(context, shortcutId)
            .setActivity(android.content.ComponentName(context, MainActivity::class.java))
            .setShortLabel(cleanTitle.take(25))
            .setLongLabel("$cleanMapTitle: $cleanTitle")
            .setIcon(iconCompat)
            .setIntent(launchIntent)
            .setAlwaysBadged()
            .build()
    }

    /**
     * Re-syncs all pinned shortcuts with Android OS ShortcutManager.
     * This is called on device boot, app launch, and updates to guarantee
     * shortcuts are never lost or orphaned after system restarts.
     */
    fun syncAllShortcuts(context: Context) {
        try {
            val shortcuts = getPinnedShortcuts(context)
            if (shortcuts.isEmpty()) return

            val shortcutInfos = shortcuts.take(15).map { item ->
                createShortcutInfo(
                    context = context,
                    mapId = item.mapId,
                    nodeId = item.nodeId,
                    nodeTitle = item.nodeTitle,
                    mapTitle = item.mapTitle,
                    colorHex = item.colorHex,
                    iconName = item.iconName
                )
            }

            // Set/refresh dynamic shortcuts with the OS
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcutInfos)
            // Update any existing pinned shortcuts so their intents and icons stay fresh
            ShortcutManagerCompat.updateShortcuts(context, shortcutInfos)
        } catch (e: Exception) {
            // Failsafe catch for OEM launcher quirks
        }
    }

    fun createLauncherOnlyShortcut(
        context: Context,
        map: MindMapEntity?,
        node: MindNodeEntity
    ): Boolean {
        try {
            val shortcutInfo = createShortcutInfo(
                context = context,
                mapId = node.mapId,
                nodeId = node.id,
                nodeTitle = node.title,
                mapTitle = map?.title ?: "مدار",
                colorHex = node.colorHex,
                iconName = node.iconName
            )

            ShortcutManagerCompat.pushDynamicShortcut(context, shortcutInfo)

            var pinSuccess = true
            if (isShortcutSupported(context)) {
                pinSuccess = ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
            }

            Toast.makeText(
                context,
                "تمت إضافة الاختصار لشاشة الموبايل الرئيسية 📱",
                Toast.LENGTH_SHORT
            ).show()
            return pinSuccess
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر إضافة الاختصار للشاشة الرئيسية", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    fun saveInAppShortcutOnly(
        context: Context,
        map: MindMapEntity?,
        node: MindNodeEntity
    ): List<PinnedNodeShortcut> {
        val updated = savePinnedShortcut(context, map, node)
        Toast.makeText(
            context,
            "تم تثبيت الاختصار في الصفحة الرئيسية للتطبيق 📌",
            Toast.LENGTH_SHORT
        ).show()
        return updated
    }

    fun createNodeShortcut(
        context: Context,
        map: MindMapEntity?,
        node: MindNodeEntity
    ): Boolean {
        // Save in-app shortcut
        savePinnedShortcut(context, map, node)

        try {
            val shortcutInfo = createShortcutInfo(
                context = context,
                mapId = node.mapId,
                nodeId = node.id,
                nodeTitle = node.title,
                mapTitle = map?.title ?: "مدار",
                colorHex = node.colorHex,
                iconName = node.iconName
            )

            // 1. Push as dynamic shortcut first so OS ShortcutManager tracks it persistently
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcutInfo)

            // 2. Request launcher pin to home screen if supported
            var pinSuccess = true
            if (isShortcutSupported(context)) {
                pinSuccess = ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
            }

            Toast.makeText(
                context,
                "تم حفظ الاختصار في التطبيق وتثبيته على شاشة الهاتف 🌟",
                Toast.LENGTH_SHORT
            ).show()
            return pinSuccess
        } catch (e: Exception) {
            Toast.makeText(context, "تم حفظ الاختصار في التطبيق 📌", Toast.LENGTH_SHORT).show()
            return true
        }
    }

    private fun generateShortcutBitmap(title: String, colorHex: String): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val colorInt = try {
            AndroidColor.parseColor(colorHex)
        } catch (e: Exception) {
            AndroidColor.parseColor("#6366F1")
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            style = Paint.Style.FILL
        }

        // Draw rounded rectangle background
        val rect = RectF(10f, 10f, (size - 10).toFloat(), (size - 10).toFloat())
        canvas.drawRoundRect(rect, 48f, 48f, bgPaint)

        // Draw glowing inner border ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            alpha = 75
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        val innerRect = RectF(18f, 18f, (size - 18).toFloat(), (size - 18).toFloat())
        canvas.drawRoundRect(innerRect, 40f, 40f, ringPaint)

        // Determine text color (white or dark)
        val r = (colorInt shr 16) and 0xFF
        val g = (colorInt shr 8) and 0xFF
        val b = colorInt and 0xFF
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        val textPaintColor = if (luminance > 0.65) AndroidColor.BLACK else AndroidColor.WHITE

        // Draw initial characters or symbol in center
        val initials = title.trim().take(2).uppercase().ifBlank { "✦" }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPaintColor
            textSize = 62f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val yPos = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initials, canvas.width / 2f, yPos, textPaint)

        return bitmap
    }
}
