package com.skripsi.chefly.ui.screens.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skripsi.chefly.data.model.OnboardingData

// Warna-warna sesuai HTML
val Terracotta = Color(0xFFE36C47)
val SoftSage = Color(0xFF8FAF9B)
val WhisperBorder = Color(0x4D94A3B8) // rgba(203, 213, 225, 0.3)

@Composable
fun IllustrationThird(data: OnboardingData) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        // --- Background Decorative Gradients (Blurs) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-20).dp, y = 20.dp)
                .size(100.dp)
                .blur(40.dp)
                .background(SoftSage.copy(alpha = 0.15f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-20).dp)
                .size(80.dp)
                .blur(30.dp)
                .background(Terracotta.copy(alpha = 0.15f), CircleShape)
        )

        // --- Main Book Card ---
        Surface(
            modifier = Modifier
                .size(width = 220.dp, height = 220.dp),
            shape = RoundedCornerShape(40.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Book Icon with Wifi-Off Badge
                Box(contentAlignment = Alignment.Center) {
                    // Placeholder for the Book "Container"
                    Surface(
                        modifier = Modifier.size(110.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xFFFAF7F2) // surface-container-low
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Terracotta,
                            modifier = Modifier.padding(20.dp).fillMaxSize()
                        )
                    }

                    // Wifi Off Badge (Top Right of Icon)
                    Surface(
                        color = Terracotta,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(4.dp, Color.White),
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Skeleton lines (mimicking the HTML space-y-2)
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(8.dp)
                        .background(Color(0xFFF3DED8), CircleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(8.dp)
                        .background(WhisperBorder, CircleShape)
                )
            }
        }
    }
}