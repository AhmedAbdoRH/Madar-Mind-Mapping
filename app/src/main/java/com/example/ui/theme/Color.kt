package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Palette
val LightBackground = Color(0xFFFEFBFF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF3F4F9)
val LightSurfaceElevated = Color(0xFFE2E1EC)
val LightBorder = Color(0xFFC5C6D0)
val LightBorderSubtle = Color(0xFFE2E1EC)

val LightPrimary = Color(0xFF4F5B92)
val LightPrimaryLight = Color(0xFFD9E2FF)
val LightPrimaryContainer = Color(0xFFD3E3FD)
val LightOnPrimaryContainer = Color(0xFF041E49)

val LightTextPrimary = Color(0xFF1B1B1F)
val LightTextSecondary = Color(0xFF44474E)
val LightTextTertiary = Color(0xFF74777F)

// Dark Palette
val DarkBackground = Color(0xFF111318)
val DarkSurface = Color(0xFF1B1D24)
val DarkSurfaceVariant = Color(0xFF252833)
val DarkSurfaceElevated = Color(0xFF2E3240)
val DarkBorder = Color(0xFF3E4354)
val DarkBorderSubtle = Color(0xFF2B2E3C)

val DarkPrimary = Color(0xFFB8C4FF)
val DarkPrimaryLight = Color(0xFF374478)
val DarkPrimaryContainer = Color(0xFF374478)
val DarkOnPrimaryContainer = Color(0xFFD9E2FF)

val DarkTextPrimary = Color(0xFFE4E2EA)
val DarkTextSecondary = Color(0xFFA6A8B6)
val DarkTextTertiary = Color(0xFF787B8A)

// Theme Colors Data Structure
data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderSubtle: Color,
    val primary: Color,
    val primaryLight: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val canvasDot: Color,
    val canvasTrack: Color
)

val LightAppColors = AppColors(
    isDark = false,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    surfaceElevated = LightSurfaceElevated,
    border = LightBorder,
    borderSubtle = LightBorderSubtle,
    primary = LightPrimary,
    primaryLight = LightPrimaryLight,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    canvasDot = LightPrimary.copy(alpha = 0.08f),
    canvasTrack = LightBorder.copy(alpha = 0.8f)
)

val DarkAppColors = AppColors(
    isDark = true,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    surfaceElevated = DarkSurfaceElevated,
    border = DarkBorder,
    borderSubtle = DarkBorderSubtle,
    primary = DarkPrimary,
    primaryLight = DarkPrimaryLight,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    canvasDot = DarkPrimary.copy(alpha = 0.15f),
    canvasTrack = DarkBorder.copy(alpha = 0.9f)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

// Backward-compatible color references
val SleekBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.background

val SleekSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.surface

val SleekSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.surfaceVariant

val SleekSurfaceElevated: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.surfaceElevated

val SleekBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.border

val SleekBorderSubtle: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.borderSubtle

val SleekPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.primary

val SleekPrimaryLight: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.primaryLight

val SleekPrimaryContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.primaryContainer

val SleekOnPrimaryContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.onPrimaryContainer

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.textSecondary

val TextTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.textTertiary

val CosmicBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.background

val CosmicSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.surface

val CosmicSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.surfaceVariant

val CosmicBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.border
