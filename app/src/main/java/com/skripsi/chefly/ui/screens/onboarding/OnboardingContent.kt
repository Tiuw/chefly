package com.skripsi.chefly.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skripsi.chefly.data.model.OnboardingData
import com.skripsi.chefly.ui.screens.onboarding.components.*

@Composable
fun OnboardingContent(data: OnboardingData, index: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Memastikan konten berada di tengah vertikal
    ) {
        // Ilustrasi Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f),
            contentAlignment = Alignment.Center
        ) {
            when (index) {
                0 -> IllustrationFirst(data)
                1 -> IllustrationSecond(data)
                2 -> IllustrationThird(data)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Teks Area
        Text(
            text = data.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = DeepCharcoal,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = data.description,
            fontSize = 16.sp,
            color = MutedSlate,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Memberikan ruang di bawah agar tidak terlalu mepet dengan tombol lanjut
        Spacer(modifier = Modifier.height(64.dp))
    }
}