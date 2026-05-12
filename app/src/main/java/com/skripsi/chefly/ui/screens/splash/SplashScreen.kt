package com.skripsi.chefly.ui.screens.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skripsi.chefly.ui.theme.DeepCharcoal
import com.skripsi.chefly.ui.theme.MutedSlate
import com.skripsi.chefly.ui.theme.Terracotta
import com.skripsi.chefly.ui.theme.WarmIvory
import kotlinx.coroutines.delay
import com.skripsi.chefly.R

@Composable
fun SplashScreen(
    onTimeout: () -> Unit // Callback untuk pindah ke Onboarding/Home
) {
    // Navigasi otomatis setelah 3 detik
    LaunchedEffect(Unit) {
        delay(3000)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmIvory)
    ) {
        // 1. Background Decoration (Lingkaran Tipis/Outline)
        BackgroundCircles()

        // 2. Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Utama
            Image(
                painter = painterResource(id = R.drawable.chefly_logo), // Ganti dengan resource Anda
                contentDescription = "Chefly Logo",
                modifier = Modifier
                    .size(350.dp)
                    .padding(bottom = 16.dp),
                contentScale = ContentScale.Fit
            )

            // Slogan
            Text(
                text = "Masak asik, nggak pake pusing.",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DeepCharcoal.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 34.sp,
                modifier = Modifier.widthIn(max = 320.dp)
            )
        }

        // 3. Footer Section (Culinary Intelligence)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Dot Indicator
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Terracotta.copy(alpha = 0.3f))
            )

            // Metadata Text
            Text(
                text = "CULINARY INTELLIGENCE",
                fontSize = 14.sp,
                letterSpacing = 4.sp,
                color = MutedSlate,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun BackgroundCircles() {
    // Dekorasi lingkaran tipis di latar belakang (opacity 0.03)
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.5.dp.toPx()
            val color = Terracotta.copy(alpha = 0.03f)

            // Lingkaran Kiri Atas
            drawCircle(
                color = color,
                radius = 128.dp.toPx(),
                center = Offset(-40.dp.toPx(), -40.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )

            // Lingkaran Kanan Tengah
            drawCircle(
                color = color,
                radius = 160.dp.toPx(),
                center = Offset(size.width + 60.dp.toPx(), size.height / 2),
                style = Stroke(width = strokeWidth)
            )

            // Lingkaran Bawah
            drawCircle(
                color = color,
                radius = 96.dp.toPx(),
                center = Offset(size.width * 0.25f, size.height + 30.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}