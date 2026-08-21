package com.skripsi.chefly.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * CHEFLY COLOR SYSTEM - ARTISAN WARMTH THEME
 * Full backward-compatible color definitions.
 */

// --- BRAND IDENTITY COLORS ---
val Terracotta = Color(0xFFD9532F)       // Primary Action, Match Score % & Active Tabs
val TerracottaDark = Color(0xFFA6381B)   // Pressed state / Deep header
val SoftSage = Color(0xFF2E6B47)         // Status "Tersedia" (High Contrast)
val SoftSageLight = Color(0xFFE8F5E9)    // Container chip status tersedia
val AlertAmber = Color(0xFFD97706)       // Perlu tambahan bahan / warning
val ErrorCoral = Color(0xFFDC2626)       // Error & Hapus

// --- NEUTRALS & TEXT TOKENS ---
val DeepCharcoal = Color(0xFF1C1917)     // Text Primary / Heading
val SecondaryText = Color(0xFF78716C)    // Text Secondary / Subtitles
val WhisperBorder = Color(0xFFE7E5E4)    // Border Card & Divider tipis
val MutedSlate = Color(0xFF6B7280)       // Legacy Text Neutral

// --- BACKGROUND & SURFACE TOKENS ---
val CheflyBackground = Color(0xFFFAF8F5) // Canvas dasar layar (Warm Alabaster)
val PureSurface = Color(0xFFFFFFFF)      // Permukaan Card / TopBar / Dialog
val WarmIvory = Color(0xFFFAF8F5)        // Background sekunder (selaras dengan CheflyBackground)

// --- M3 COLOR SCHEME TOKENS ---
val CheflyPrimary = Terracotta
val CheflyOnPrimary = Color(0xFFFFFFFF)
val CheflyPrimaryContainer = Color(0xFFFFDBCF)
val CheflyOnPrimaryContainer = Color(0xFF380C00)

val CheflySecondary = Color(0xFF77574E)
val CheflyOnSecondary = Color(0xFFFFFFFF)
val CheflySecondaryContainer = Color(0xFFF5EBE6)
val CheflyOnSecondaryContainer = Color(0xFF2C150F)

val CheflyTertiary = SoftSage
val CheflyOnTertiary = Color(0xFFFFFFFF)
val CheflyTertiaryContainer = SoftSageLight
val CheflyOnTertiaryContainer = Color(0xFF00210E)

val CheflySurface = PureSurface
val CheflyOnSurface = DeepCharcoal
val CheflyOnBackground = DeepCharcoal
val CheflySurfaceVariant = Color(0xFFF5DED7)
val CheflyOnSurfaceVariant = Color(0xFF53433F)

val CheflyOutline = Color(0xFFD6D3D1)
val CheflyOutlineVariant = WhisperBorder
val CheflyError = ErrorCoral
val CheflyOnError = Color(0xFFFFFFFF)
val CheflyErrorContainer = Color(0xFFFFDAD6)
val CheflyOnErrorContainer = Color(0xFF93000A)

// --- LEGACY M3 VARIATION TOKENS (Untuk mencegah error kode lama) ---
val CheflySurfaceDim = Color(0xFFEAD6D0)
val CheflySurfaceBright = Color(0xFFFAF8F5)
val CheflySurfaceContainerLowest = Color(0xFFFFFFFF)
val CheflySurfaceContainerLow = Color(0xFFF5EBE6)
val CheflySurfaceContainer = Color(0xFFFFE9E4)
val CheflySurfaceContainerHigh = Color(0xFFF9E4DE)
val CheflySurfaceContainerHighest = Color(0xFFF3DED8)
val CheflyInverseSurface = Color(0xFF3A2E2A)
val CheflyInverseOnSurface = Color(0xFFFFEDE8)
val CheflySurfaceTint = Terracotta
val CheflyPrimaryFixed = Color(0xFFFFDBD1)
val CheflyPrimaryFixedDim = Color(0xFFFFB59F)
val CheflyOnPrimaryFixed = Color(0xFF3A0A00)
val CheflyOnPrimaryFixedVariant = Color(0xFF832605)