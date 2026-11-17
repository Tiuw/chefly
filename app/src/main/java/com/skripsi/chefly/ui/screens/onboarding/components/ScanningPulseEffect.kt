package com.skripsi.chefly.ui.screens.onboarding.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ScanningPulseEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")

    // Animasi denyut (pulse) lingkaran luar
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "pulseAlpha"
    )

    Box(contentAlignment = Alignment.Center) {

        // 1. Scanning Pulse Ring (Lingkaran yang menyebar keluar)
        Box(
            modifier = Modifier
                .size(64.dp * pulseScale)
                .border(1.5.dp, Color.White.copy(alpha = pulseAlpha), CircleShape)
        )

        // 2. Main Glass Container (Ikon pusat dengan efek Blur/Glassmorphism)
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.2f), // Putih transparan (bg-white/20)
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)), // border-white/40
            shadowElevation = 8.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Ikon "center_focus_strong" (Material Symbols Outlined)
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}