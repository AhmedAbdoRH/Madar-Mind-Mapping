package com.example.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class OrbitIconItem(
    val key: String,
    val label: String,
    val icon: ImageVector
)

object OrbitIcons {
    val icons = listOf(
        OrbitIconItem("none", "None", Icons.Default.Close),
        OrbitIconItem("lightbulb", "Idea", Icons.Default.Lightbulb),
        OrbitIconItem("rocket_launch", "Launch", Icons.Default.RocketLaunch),
        OrbitIconItem("star", "Star", Icons.Default.Star),
        OrbitIconItem("psychology", "Mind", Icons.Default.Psychology),
        OrbitIconItem("favorite", "Heart", Icons.Default.Favorite),
        OrbitIconItem("palette", "Art", Icons.Default.Palette),
        OrbitIconItem("code", "Tech", Icons.Default.Code),
        OrbitIconItem("menu_book", "Read", Icons.Default.MenuBook),
        OrbitIconItem("checklist", "Tasks", Icons.Default.Checklist),
        OrbitIconItem("check_circle", "Done", Icons.Default.CheckCircle),
        OrbitIconItem("work", "Work", Icons.Default.Work),
        OrbitIconItem("attach_money", "Finance", Icons.Default.AttachMoney),
        OrbitIconItem("flag", "Goal", Icons.Default.Flag),
        OrbitIconItem("bolt", "Power", Icons.Default.Bolt),
        OrbitIconItem("explore", "Explore", Icons.Default.Explore),
        OrbitIconItem("music_note", "Music", Icons.Default.MusicNote),
        OrbitIconItem("coffee", "Coffee", Icons.Default.Coffee),
        OrbitIconItem("fitness_center", "Fitness", Icons.Default.FitnessCenter),
        OrbitIconItem("school", "Learn", Icons.Default.School),
        OrbitIconItem("flight", "Travel", Icons.Default.Flight),
        OrbitIconItem("home", "Home", Icons.Default.Home),
        OrbitIconItem("emoji_events", "Trophy", Icons.Default.EmojiEvents),
        OrbitIconItem("shopping_cart", "Shop", Icons.Default.ShoppingCart),
        OrbitIconItem("brush", "Design", Icons.Default.Brush),
        OrbitIconItem("shield", "Security", Icons.Default.Shield),
        OrbitIconItem("wb_sunny", "Sun", Icons.Default.WbSunny),
        OrbitIconItem("water_drop", "Water", Icons.Default.WaterDrop),
        OrbitIconItem("self_improvement", "Zen", Icons.Default.SelfImprovement),
        OrbitIconItem("timer", "Timer", Icons.Default.Timer),
        OrbitIconItem("group", "Team", Icons.Default.Group),
        OrbitIconItem("cloud", "Cloud", Icons.Default.Cloud),
        OrbitIconItem("smart_display", "Video", Icons.Default.SmartDisplay),
        OrbitIconItem("mic", "Audio", Icons.Default.Mic),
        OrbitIconItem("touch_app", "Touch", Icons.Default.TouchApp),
        OrbitIconItem("directions_walk", "Walk", Icons.Default.DirectionsWalk),
        OrbitIconItem("dark_mode", "Night", Icons.Default.DarkMode),
        OrbitIconItem("auto_awesome", "AI Spark", Icons.Default.AutoAwesome)
    )

    private val iconMap: Map<String, ImageVector> = icons.associate { it.key.lowercase() to it.icon }

    fun getIcon(key: String): ImageVector? {
        if (key.isBlank() || key.equals("none", ignoreCase = true)) {
            return null
        }
        return iconMap[key.lowercase()] ?: Icons.Default.Lightbulb
    }
}
