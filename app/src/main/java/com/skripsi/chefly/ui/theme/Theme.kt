package com.skripsi.chefly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Chefly Light Color Scheme
 * Warm, culinary-inspired, and tech-forward
 */
private val LightColorScheme = lightColorScheme(
    primary = CheflyPrimary,
    onPrimary = CheflyOnPrimary,
    primaryContainer = CheflyPrimaryContainer,
    onPrimaryContainer = CheflyOnPrimaryContainer,

    secondary = CheflySecondary,
    onSecondary = CheflyOnSecondary,
    secondaryContainer = CheflySecondaryContainer,
    onSecondaryContainer = CheflyOnSecondaryContainer,

    tertiary = CheflyTertiary,
    onTertiary = CheflyOnTertiary,
    tertiaryContainer = CheflyTertiaryContainer,
    onTertiaryContainer = CheflyOnTertiaryContainer,

    background = CheflyBackground,
    onBackground = CheflyOnBackground,
    surface = CheflySurface,
    onSurface = CheflyOnSurface,
    surfaceVariant = CheflySurfaceVariant,
    onSurfaceVariant = CheflyOnSurfaceVariant,

    outline = CheflyOutline,
    outlineVariant = CheflyOutlineVariant,

    error = CheflyError,
    onError = CheflyOnError,
    errorContainer = CheflyErrorContainer,
    onErrorContainer = CheflyOnErrorContainer,

    inverseSurface = CheflyInverseSurface,
    inverseOnSurface = CheflyInverseOnSurface,
    inversePrimary = CheflyPrimaryFixedDim,
    surfaceTint = CheflySurfaceTint
)

/**
 * Chefly Dark Color Scheme
 * Using same palette but adjusted for dark backgrounds
 */
private val DarkColorScheme = darkColorScheme(
    primary = CheflyPrimaryFixedDim,
    onPrimary = CheflyOnPrimaryFixed,
    primaryContainer = CheflyPrimary,
    onPrimaryContainer = CheflyOnPrimary,

    secondary = CheflySecondaryContainer,
    onSecondary = CheflyOnSecondaryContainer,
    secondaryContainer = CheflySecondary,
    onSecondaryContainer = CheflyOnSecondary,

    tertiary = CheflyTertiaryContainer,
    onTertiary = CheflyOnTertiaryContainer,
    tertiaryContainer = CheflyTertiary,
    onTertiaryContainer = CheflyOnTertiary,

    background = CheflyInverseSurface,
    onBackground = CheflyInverseOnSurface,
    surface = CheflyInverseSurface,
    onSurface = CheflyInverseOnSurface,
    surfaceVariant = CheflyOnSurfaceVariant,
    onSurfaceVariant = CheflySurfaceVariant,

    outline = CheflyOutlineVariant,
    outlineVariant = CheflyOutline,

    error = CheflyError,
    onError = CheflyOnError,
    errorContainer = CheflyErrorContainer,
    onErrorContainer = CheflyOnErrorContainer
)

@Composable
fun CheflyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors for consistent Design System
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CheflyTypography,
        content = content
    )
}
