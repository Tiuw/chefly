package com.skripsi.chefly.ui.screens.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skripsi.chefly.ui.screens.onboarding.Terracotta

@Composable
fun DetectionBadge(label: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // Bounding Box
        Box(
            modifier = Modifier
                .size(80.dp) // Ukuran kotak bisa disesuaikan
                .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
        )

        // Label (Tomat/Paprika)
        Surface(
            color = Terracotta,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.offset(x = 0.dp, y = (-10).dp), // Menempel pas di garis atas
            shadowElevation = 4.dp
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}