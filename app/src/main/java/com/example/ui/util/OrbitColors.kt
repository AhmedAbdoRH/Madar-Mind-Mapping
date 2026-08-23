package com.example.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

data class NodeColorItem(
    val name: String,
    val hex: String,
    val color: Color
)

object OrbitColors {
    val nodeColors = listOf(
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

    val defaultColorHex = "#4F5B92"

    fun parseColor(hex: String, defaultColor: Color = Color(0xFF4F5B92)): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            when (cleanHex.length) {
                6 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
                8 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
                else -> defaultColor
            }
        } catch (e: Exception) {
            defaultColor
        }
    }

    fun getContrastingTextColor(backgroundColor: Color): Color {
        // High luminance (light pastel) -> Dark text, Low luminance (deep tone) -> White text
        return if (backgroundColor.luminance() > 0.45f) {
            Color(0xFF1B1B1F)
        } else {
            Color.White
        }
    }
}

