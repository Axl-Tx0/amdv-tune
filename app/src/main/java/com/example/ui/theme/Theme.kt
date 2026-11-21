package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.model.AppThemeMode

@Immutable
data class AmdvTunePalette(
    val mode: AppThemeMode,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val accent: Color,
    val secondaryAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val metallicStart: Color,
    val metallicEnd: Color,
    val isDark: Boolean
)

val LocalAmdvPalette = staticCompositionLocalOf {
    AmdvTunePalette(
        mode = AppThemeMode.NEGRO,
        background = NegroBackground,
        surface = NegroSurface,
        surfaceElevated = NegroSurfaceVariant,
        border = NegroBorder,
        accent = NegroAccent,
        secondaryAccent = NegroSecondaryAccent,
        textPrimary = NegroTextPrimary,
        textSecondary = NegroTextSecondary,
        metallicStart = MetallicLight,
        metallicEnd = MetallicDark,
        isDark = true
    )
}

@Composable
fun AmdvTuneTheme(
    themeMode: AppThemeMode = AppThemeMode.NEGRO,
    content: @Composable () -> Unit
) {
    val palette = when (themeMode) {
        AppThemeMode.NEON -> AmdvTunePalette(
            mode = AppThemeMode.NEON,
            background = NeonBackground,
            surface = NeonSurface,
            surfaceElevated = NeonSurfaceVariant,
            border = NeonBorder,
            accent = NeonAccent,
            secondaryAccent = NeonSecondaryAccent,
            textPrimary = NeonTextPrimary,
            textSecondary = NeonTextSecondary,
            metallicStart = NeonAccent,
            metallicEnd = Color(0xFF6366F1),
            isDark = true
        )
        AppThemeMode.NEGRO -> AmdvTunePalette(
            mode = AppThemeMode.NEGRO,
            background = NegroBackground,
            surface = NegroSurface,
            surfaceElevated = NegroSurfaceVariant,
            border = NegroBorder,
            accent = NegroAccent,
            secondaryAccent = NegroSecondaryAccent,
            textPrimary = NegroTextPrimary,
            textSecondary = NegroTextSecondary,
            metallicStart = MetallicLight,
            metallicEnd = MetallicDark,
            isDark = true
        )
        AppThemeMode.BLANCO -> AmdvTunePalette(
            mode = AppThemeMode.BLANCO,
            background = BlancoBackground,
            surface = BlancoSurface,
            surfaceElevated = BlancoSurfaceVariant,
            border = BlancoBorder,
            accent = BlancoAccent,
            secondaryAccent = BlancoSecondaryAccent,
            textPrimary = BlancoTextPrimary,
            textSecondary = BlancoTextSecondary,
            metallicStart = Color(0xFF334155),
            metallicEnd = Color(0xFF0F172A),
            isDark = false
        )
    }

    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = Color.Black,
            secondary = palette.secondaryAccent,
            onSecondary = Color.White,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            secondary = palette.secondaryAccent,
            onSecondary = Color.White,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border
        )
    }

    CompositionLocalProvider(LocalAmdvPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AmdvTypography,
            content = content
        )
    }
}
