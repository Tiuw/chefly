package com.skripsi.chefly.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Chefly Typography System
 * Display/Headers: Outfit
 * Body/Content: Inter
 * Mono/Metadata: JetBrains Mono
 */

// Define font families - Using Default as fallback if custom fonts aren't bundled yet
private val Outfit = FontFamily.Default
private val Inter = FontFamily.Default
private val JetBrainsMono = FontFamily.Default

val CheflyTypography = Typography(
    // Display (Recipe Titles): clamp(1.5rem, 5vw, 2rem) -> approx 32sp
    displayLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.02).sp
    ),

    // Heading 1 (Screen Titles): 1.25rem (20px)
    headlineLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),

    // Heading 2 (Section Headers): 1.125rem (18px)
    headlineMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),

    // Body (Instructions, Descriptions): 1rem (16px)
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),

    // Small/Meta (Cook Time, Servings): 0.875rem (14px)
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    // Mono (Percentages, Timers): 0.875rem (14px)
    labelMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 14.sp
    ),

    // Label Caps: 11px, Outfit 600, letter-spacing +0.05em
    labelSmall = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 11.sp,
        letterSpacing = 0.05.sp
    )
)
