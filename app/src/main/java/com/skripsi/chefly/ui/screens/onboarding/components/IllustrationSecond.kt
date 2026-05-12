package com.skripsi.chefly.ui.screens.onboarding.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.skripsi.chefly.data.model.OnboardingData

@Composable
fun IllustrationSecond(data: OnboardingData) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val translateY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "y"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Lingkaran Dashed - DIPERBESAR (320dp)
        Canvas(modifier = Modifier.size(320.dp)) {
            drawCircle(
                color = Color(0xFFE36C47).copy(alpha = 0.3f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )
            )
        }

        // 2. Gambar Utama (Piring Rendang)
        Surface(
            modifier = Modifier.size(200.dp),
            shape = RoundedCornerShape(48.dp),
            shadowElevation = 12.dp,
            color = Color.White
        ) {
            AsyncImage(
                model = data.imageRes,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }

        // 3. Floating Icons - Disesuaikan agar pas di garis lingkaran
        // Ikon Telur (Kanan Atas)
        FloatingChip(
            icon = Icons.Default.Egg,
            alignment = Alignment.TopEnd,
            color = Color(0xFFE36C47),
            offsetY = translateY,
            // Gunakan offset negatif untuk menarik ikon ke arah tengah (mendekati lingkaran)
            offsetX = (-35).dp,
            verticalOffset = 35.dp
        )

        // Ikon Restaurant (Kiri Bawah)
        FloatingChip(
            icon = Icons.Default.Restaurant,
            alignment = Alignment.BottomStart,
            color = Color(0xFF8FAF9B),
            offsetY = translateY * 0.7f,
            // Gunakan offset positif untuk menarik ikon ke arah tengah
            offsetX = 35.dp,
            verticalOffset = (-35).dp
        )
    }
}

@Composable
fun BoxScope.FloatingChip(
    icon: ImageVector,
    alignment: Alignment,
    color: Color,
    offsetY: Float,
    offsetX: androidx.compose.ui.unit.Dp = 0.dp,
    verticalOffset: androidx.compose.ui.unit.Dp = 0.dp
) {
    Surface(
        modifier = Modifier
            .align(alignment)
            // Gabungan offset statis untuk posisi dan offsetY untuk animasi melayang
            .offset(x = offsetX, y = verticalOffset + offsetY.dp)
            .size(60.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.padding(16.dp)
        )
    }
}