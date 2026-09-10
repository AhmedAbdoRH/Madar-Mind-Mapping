package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.ThemeMode

private val SleekLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = Color(0xFF5B5D72),
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceElevated,
    onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF75546F),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorderSubtle
)

private val SleekDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF1F2D60),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = Color(0xFFC3C5DD),
    onSecondary = Color(0xFF2D2F42),
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = Color(0xFFDFE1F9),
    tertiary = Color(0xFFE4BAD9),
    onTertiary = Color(0xFF43273F),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorderSubtle
)

private val SleekZenBlackColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF1F2D60),
    primaryContainer = Color(0xFF1A1A24),
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = Color(0xFFC3C5DD),
    onSecondary = Color(0xFF16161D),
    secondaryContainer = Color(0xFF1E1E26),
    onSecondaryContainer = Color(0xFFDFE1F9),
    tertiary = Color(0xFFE4BAD9),
    onTertiary = Color(0xFF43273F),
    background = Color.Black,
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF0D0D11),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF16161D),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF2A2B36),
    outlineVariant = Color(0xFF1F202B)
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val (appColors, colorScheme) = when (themeMode) {
        ThemeMode.ZEN_BLACK -> Pair(ZenBlackAppColors, SleekZenBlackColorScheme)
        ThemeMode.DARK -> Pair(DarkAppColors, SleekDarkColorScheme)
        ThemeMode.LIGHT -> Pair(LightAppColors, SleekLightColorScheme)
        ThemeMode.SYSTEM -> if (darkTheme) Pair(DarkAppColors, SleekDarkColorScheme) else Pair(LightAppColors, SleekLightColorScheme)
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
