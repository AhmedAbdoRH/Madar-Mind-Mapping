package com.example.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.example.R
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

data class NodeColorItem(
    val name: String,
    val hex: String,
    val color: Color
)

data class ColorCategory(
    val id: String,
    val titleRes: Int,
    val colors: List<NodeColorItem>
)

enum class ColorApplyScope(
    val titleRes: Int,
    val iconName: String
) {
    THIS_NODE_ONLY(R.string.color_scope_this_node, "circle"),
    DIRECT_CHILDREN(R.string.color_scope_direct_children, "subdirectory_arrow_right"),
    ALL_DESCENDANTS(R.string.color_scope_all_descendants, "account_tree"),
    SIBLINGS(R.string.color_scope_siblings, "hub"),
    ENTIRE_BRANCH(R.string.color_scope_entire_branch, "device_hub"),
    NODE_AND_SIBLINGS(R.string.color_scope_node_siblings, "scatter_plot"),
    HARMONIC_GRADIENT(R.string.color_scope_harmonic_gradient, "auto_awesome"),
    SET_AS_PRIMARY_MAP_COLOR(R.string.color_scope_set_as_primary, "star")
}

object OrbitColors {
    val defaultColorHex = "#4F5B92"

    // 1. Classic Sleek & Core
    val coreColors = listOf(
        NodeColorItem("Sleek Indigo", "#4F5B92", Color(0xFF4F5B92)),
        NodeColorItem("Soft Sky Blue", "#D9E2FF", Color(0xFFD9E2FF)),
        NodeColorItem("Lavender Dream", "#EADDFF", Color(0xFFEADDFF)),
        NodeColorItem("Soft Blossom", "#F9DEDC", Color(0xFFF9DEDC)),
        NodeColorItem("Slate Mist", "#DAE2E9", Color(0xFFDAE2E9)),
        NodeColorItem("Cool Pearl", "#E2E2E6", Color(0xFFE2E2E6)),
        NodeColorItem("Mint Glass", "#C4EED0", Color(0xFFC4EED0)),
        NodeColorItem("Warm Lemon", "#FFF0B3", Color(0xFFFFF0B3)),
        NodeColorItem("Deep Violet", "#6750A4", Color(0xFF6750A4)),
        NodeColorItem("Forest Teal", "#006874", Color(0xFF006874)),
        NodeColorItem("Amber Dusk", "#E8710A", Color(0xFFE8710A)),
        NodeColorItem("Rose Coral", "#FF6B6B", Color(0xFFFF6B6B)),
        NodeColorItem("Cobalt Blue", "#1A73E8", Color(0xFF1A73E8)),
        NodeColorItem("Berry Plum", "#75546F", Color(0xFF75546F))
    )

    // 2. Cosmic Neon (ألوان الفضاء والنيون)
    val cosmicNeonColors = listOf(
        NodeColorItem("Electric Violet", "#7C3AED", Color(0xFF7C3AED)),
        NodeColorItem("Cyber Cyan", "#06B6D4", Color(0xFF06B6D4)),
        NodeColorItem("Neon Magenta", "#EC4899", Color(0xFFEC4899)),
        NodeColorItem("Aurora Emerald", "#10B981", Color(0xFF10B981)),
        NodeColorItem("Solar Amber", "#F59E0B", Color(0xFFF59E0B)),
        NodeColorItem("Laser Coral", "#F43F5E", Color(0xFFF43F5E)),
        NodeColorItem("Cosmic Blue", "#3B82F6", Color(0xFF3B82F6)),
        NodeColorItem("Supernova Gold", "#EAB308", Color(0xFFEAB308))
    )

    // 3. Soft Pastel (ألوان الباستيل الهادئة)
    val softPastelColors = listOf(
        NodeColorItem("Pastel Lavender", "#DDD6FE", Color(0xFFDDD6FE)),
        NodeColorItem("Pastel Sky", "#BAE6FD", Color(0xFFBAE6FD)),
        NodeColorItem("Pastel Mint", "#A7F3D0", Color(0xFFA7F3D0)),
        NodeColorItem("Pastel Peach", "#FED7AA", Color(0xFFFED7AA)),
        NodeColorItem("Pastel Rose", "#FECDD3", Color(0xFFFECDD3)),
        NodeColorItem("Pastel Canary", "#FEF08A", Color(0xFFFEF08A)),
        NodeColorItem("Pastel Lilac", "#F5D0FE", Color(0xFFF5D0FE)),
        NodeColorItem("Pastel Slate", "#CBD5E1", Color(0xFFCBD5E1))
    )

    // 4. Modern Corporate (الإنتاجية والأعمال)
    val businessColors = listOf(
        NodeColorItem("Executive Navy", "#1E3A8A", Color(0xFF1E3A8A)),
        NodeColorItem("Teal Leader", "#0F766E", Color(0xFF0F766E)),
        NodeColorItem("Royal Indigo", "#4338CA", Color(0xFF4338CA)),
        NodeColorItem("Crimson Focus", "#BE123C", Color(0xFFBE123C)),
        NodeColorItem("Graphite Slate", "#334155", Color(0xFF334155)),
        NodeColorItem("Forest Pine", "#14532D", Color(0xFF14532D)),
        NodeColorItem("Bronze Ochre", "#B45309", Color(0xFFB45309)),
        NodeColorItem("Titanium Dark", "#18181B", Color(0xFF18181B))
    )

    // 5. Warm Nature (طبيعة وأرضي)
    val warmNatureColors = listOf(
        NodeColorItem("Terracotta", "#C2410C", Color(0xFFC2410C)),
        NodeColorItem("Olive Earth", "#4D7C0F", Color(0xFF4D7C0F)),
        NodeColorItem("Desert Sand", "#D97706", Color(0xFFD97706)),
        NodeColorItem("Clay Brown", "#78350F", Color(0xFF78350F)),
        NodeColorItem("Moss Green", "#15803D", Color(0xFF15803D)),
        NodeColorItem("Eucalyptus", "#0D9488", Color(0xFF0D9488)),
        NodeColorItem("Autumn Spice", "#9A3412", Color(0xFF9A3412)),
        NodeColorItem("Warm Coffee", "#451A03", Color(0xFF451A03))
    )

    // Backward-compatible nodeColors property
    val nodeColors = coreColors

    val categories = listOf(
        ColorCategory("core", R.string.palette_category_core, coreColors),
        ColorCategory("neon", R.string.palette_category_neon, cosmicNeonColors),
        ColorCategory("pastel", R.string.palette_category_pastel, softPastelColors),
        ColorCategory("business", R.string.palette_category_business, businessColors),
        ColorCategory("nature", R.string.palette_category_earth, warmNatureColors)
    )

    private val colorCache = ConcurrentHashMap<String, Color>()
    private val textColorCache = ConcurrentHashMap<Color, Color>()

    fun parseColor(hex: String, defaultColor: Color = Color(0xFF4F5B92)): Color {
        val cached = colorCache[hex]
        if (cached != null) return cached

        val parsed = try {
            val cleanHex = hex.trim().removePrefix("#")
            when (cleanHex.length) {
                6 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
                8 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
                3 -> {
                    // Expand #RGB to #RRGGBB
                    val r = cleanHex[0]
                    val g = cleanHex[1]
                    val b = cleanHex[2]
                    Color(android.graphics.Color.parseColor("#$r$r$g$g$b$b"))
                }
                else -> defaultColor
            }
        } catch (e: Exception) {
            defaultColor
        }
        colorCache[hex] = parsed
        return parsed
    }

    fun colorToHex(color: Color): String {
        val argb = color.toArgb()
        return String.format("#%06X", 0xFFFFFF and argb)
    }

    fun getContrastingTextColor(backgroundColor: Color): Color {
        val cached = textColorCache[backgroundColor]
        if (cached != null) return cached

        val result = if (backgroundColor.luminance() > 0.45f) {
            Color(0xFF1B1B1F)
        } else {
            Color.White
        }
        textColorCache[backgroundColor] = result
        return result
    }

    /**
     * Generates a list of harmonic tints / tones for a given base color.
     * Perfect for children or orbital rings.
     */
    fun generateHarmonicTones(baseHex: String, steps: Int = 4): List<String> {
        val baseColor = parseColor(baseHex)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)

        val result = mutableListOf<String>()
        val count = max(1, steps)

        for (i in 0 until count) {
            val factor = (i + 1).toFloat() / (count + 1)
            // Shift saturation and value to create harmonious analogous/lighter variations
            val newSat = max(0.2f, min(1f, hsv[1] * (1f - factor * 0.4f)))
            val newVal = min(1f, hsv[2] + (1f - hsv[2]) * factor * 0.5f)
            val newHue = (hsv[0] + (i * 12f)) % 360f

            val variantHsv = floatArrayOf(newHue, newSat, newVal)
            val variantColor = Color(android.graphics.Color.HSVToColor(variantHsv))
            result.add(colorToHex(variantColor))
        }

        return result
    }

    /**
     * Generates a specific depth-level harmonic tint for a child node at depth level (1, 2, 3...)
     */
    fun getHarmonicToneForLevel(baseHex: String, level: Int): String {
        if (level <= 0) return baseHex
        val baseColor = parseColor(baseHex)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)

        val satFactor = max(0.35f, 1f - (level * 0.15f))
        val valFactor = min(1f, hsv[2] + (1f - hsv[2]) * (level * 0.12f))
        val hueShift = (hsv[0] + (level * 15f)) % 360f

        val newColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hueShift, hsv[1] * satFactor, valFactor)))
        return colorToHex(newColor)
    }
}
