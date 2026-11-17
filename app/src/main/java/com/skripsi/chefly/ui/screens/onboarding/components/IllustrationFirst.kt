package com.skripsi.chefly.ui.screens.onboarding.components

import android.R.attr.data
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.skripsi.chefly.data.model.OnboardingData
import com.skripsi.chefly.ui.screens.onboarding.DeepCharcoal
import com.skripsi.chefly.ui.screens.onboarding.MutedSlate
import com.skripsi.chefly.ui.screens.onboarding.Terracotta

@Composable
fun IllustrationFirst(data: OnboardingData) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f),
        contentAlignment = Alignment.Center
    ) {
        val boardWidth = maxWidth
        val boardHeight = maxHeight

        // 1. Gambar Utama diambil dari data.imageRes
        AsyncImage(
            model = data.imageRes,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(40.dp)),
            contentScale = ContentScale.Crop
        )

        // 2. Detection Badge Otomatis dari label1
        if (data.label1.isNotEmpty()) {
            DetectionBadge(
                label = data.label1,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = boardWidth * 0.27f,
                        y = boardHeight * 0.57f
                    )
            )
        }

        // 3. Detection Badge Otomatis dari label2
        if (data.label2.isNotEmpty()) {
            DetectionBadge(
                label = data.label2,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = boardWidth * 0.55f,
                        y = boardHeight * 0.64f
                    )
            )
        }

        ScanningPulseEffect()
    }
}