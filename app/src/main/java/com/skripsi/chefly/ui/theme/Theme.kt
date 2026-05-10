package com.skripsi.chefly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Bauhaus Light Color Scheme
 * Bold, geometric, neo-brutalist kitchen aesthetic
 */
private val LightColorScheme = lightColorScheme(
    // Primary: Black (#1a1a1a)
    primary = BauhausPrimary,
    onPrimary = BauhausOnPrimary,
    primaryContainer = BauhausPrimaryContainer,
    onPrimaryContainer = Color(0xFF1a1a1a),

    // Secondary: Red (#e63b2e)
    secondary = BauhausSecondary,
    onSecondary = BauhausOnSecondary,
    secondaryContainer = BauhausSecondaryContainer,
    onSecondaryContainer = Color(0xFF1a1a1a),

    // Tertiary: Blue (#0055ff)
    tertiary = BauhausTertiary,
    onTertiary = BauhausOnTertiary,
    tertiaryContainer = BauhausTertiaryContainer,
    onTertiaryContainer = Color(0xFF1a1a1a),

    // Surfaces
    background = BauhausBackground,
    onBackground = Color(0xFF1a1a1a),
    surface = BauhausSurface,
    onSurface = BauhausOnSurface,
    surfaceVariant = BauhausSurfaceVariant,
    onSurfaceVariant = Color(0xFF4a4a4a),

    // Outlines
    outline = BauhausOutline,
    outlineVariant = BauhausOutlineVariant,

    // Error
    error = BauhausError,
    onError = BauhausOnError,
    errorContainer = BauhausErrorContainer,
    onErrorContainer = Color(0xFF93000a)
)

/**
 * Bauhaus Dark Color Scheme (optional for future dark mode)
 */
private val DarkColorScheme = darkColorScheme(
    primary = BauhausPrimaryContainer,
    onPrimary = BauhausPrimary,
    primaryContainer = BauhausPrimaryContainer,
    onPrimaryContainer = Color(0xFF1a1a1a),

    secondary = BauhausSecondary,
    onSecondary = Color.White,
    secondaryContainer = BauhausSecondaryContainer,
    onSecondaryContainer = Color(0xFF1a1a1a),

    tertiary = BauhausTertiary,
    onTertiary = Color.White,
    tertiaryContainer = BauhausTertiaryContainer,
    onTertiaryContainer = Color(0xFF1a1a1a),

    background = BauhausPrimary,
    onBackground = BauhausSurface,
    surface = Color(0xFF2a2a2a),
    onSurface = BauhausSurface,
    surfaceVariant = Color(0xFF3a3a3a),
    onSurfaceVariant = Color(0xFFd0cbc3),

    outline = BauhausOutline,
    outlineVariant = BauhausOutlineVariant,

    error = BauhausError,
    onError = Color.White,
    errorContainer = BauhausErrorContainer,
    onErrorContainer = Color(0xFFffa4a0)
)

@Composable
fun CheflyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors for consistent Bauhaus design
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BauhausTypography,
        content = content
    )
}