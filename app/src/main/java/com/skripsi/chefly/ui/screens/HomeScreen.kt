package com.skripsi.chefly.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.skripsi.chefly.ui.viewmodel.HomeViewModel
import com.skripsi.chefly.ui.viewmodel.RecipeUiModel

// Color Palette
val SecondaryText = Color(0xFF5F5E5B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onScanClick: () -> Unit = {},
    onRecipeClick: (String) -> Unit = {},
    onSeeAllClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recipes by viewModel.suggestedRecipes.collectAsState()

    Scaffold(
        containerColor = WarmIvory,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Chefly", color = Terracotta, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Scanner Section (Spans full width)
            item(span = { GridItemSpan(2) }) {
                ScannerHero(onScanClick = onScanClick)
            }

            // Section Header
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Masakan Populer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Text(
                        text = "LIHAT SEMUA",
                        color = Terracotta,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onSeeAllClick() } // Trigger navigasi di sini
                            .padding(8.dp) // Memberikan area sentuh yang lebih luas (UX Friendly)
                    )
                }
            }

            // Recipe Cards dinamis dari Database
            items(recipes) { recipe ->
                RecipeCard(
                    recipe = recipe,
                    onClick = { onRecipeClick(recipe.id) }
                )
            }
        }
    }
}

@Composable
fun ScannerHero(onScanClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1250, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(192.dp)) {
            Box(Modifier.size(192.dp * pulseScale).clip(CircleShape).background(Terracotta.copy(0.1f)))
            Box(Modifier.size(160.dp * pulseScale).clip(CircleShape).background(Terracotta.copy(0.15f)))

            Surface(
                shape = CircleShape,
                color = Terracotta,
                shadowElevation = 8.dp,
                modifier = Modifier.size(128.dp),
                onClick = onScanClick
            ) {
                Icon(
                    Icons.Default.CenterFocusStrong,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(32.dp).fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Pindai Bahan", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Arahkan kamera ke bahan makanan untuk menemukan resep instan",
            color = SecondaryText,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            modifier = Modifier.width(240.dp)
        )
    }
}

@Composable
fun RecipeCard(recipe: RecipeUiModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, WhisperBorder)
    ) {
        Column {
            // Bagian Gambar
            Box(modifier = Modifier.aspectRatio(1.33f)) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(Color(0xFFF3DED8))
                )
            }

            // Bagian Konten Teks
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = recipe.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2, // Ditingkatkan ke 2 baris agar judul panjang terlihat bagus
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${recipe.loves} suka",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}