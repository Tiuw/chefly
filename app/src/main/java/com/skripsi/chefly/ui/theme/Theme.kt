package com.skripsi.chefly.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Chefly Light Color Scheme
 * Artisan Warmth: High contrast, culinary-inspired, and tech-forward
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
    onBackground = DeepCharcoal,
    surface = CheflySurface,
    onSurface = CheflyOnSurface,
    surfaceVariant = CheflySurfaceVariant,
    onSurfaceVariant = CheflyOnSurfaceVariant,

    outline = CheflyOutline,
    outlineVariant = CheflyOutlineVariant,

    error = CheflyError,
    onError = CheflyOnError
)

/**
 * Chefly Dark Color Scheme
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB59F),
    onPrimary = Color(0xFF5F1500),
    primaryContainer = Color(0xFF862200),
    onPrimaryContainer = Color(0xFFFFDBCF),

    secondary = Color(0xFFE7BDB2),
    onSecondary = Color(0xFF442A22),
    secondaryContainer = Color(0xFF5D4037),
    onSecondaryContainer = Color(0xFFFFDBCF),

    tertiary = Color(0xFFA5D6A7),
    onTertiary = Color(0xFF003915),
    tertiaryContainer = Color(0xFF005322),
    onTertiaryContainer = Color(0xFFC8E6C9),

    background = Color(0xFF1C1917),
    onBackground = Color(0xFFEDE0DC),
    surface = Color(0xFF24201D),
    onSurface = Color(0xFFEDE0DC),
    surfaceVariant = Color(0xFF53433F),
    onSurfaceVariant = Color(0xFFD8C2BC),

    outline = Color(0xFFA08C87),
    outlineVariant = Color(0xFF53433F),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun CheflyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = colorScheme.background.toArgb()
                it.navigationBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CheflyTypography,
        content = content
    )
}