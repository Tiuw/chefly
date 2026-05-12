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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.skripsi.chefly.ui.theme.* // Pastikan warna diimport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onScanClick: () -> Unit,
    onRecipeClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recipes by viewModel.suggestedRecipes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        containerColor = WarmIvory,
        topBar = {
            // Menggunakan TopAppBar (Default Aligned Left)
            TopAppBar(
                title = {
                    Text(
                        text = "Chefly",
                        color = Terracotta,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        // Memberikan padding kiri agar sejajar dengan konten grid di bawah
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White, // Menjaga kontras logo di atas putih
                    scrolledContainerColor = Color.White
                ),
                // Efek elevasi halus saat di-scroll
                modifier = Modifier.shadow(2.dp)
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
            // 1. Hero Section (Scanner)
            item(span = { GridItemSpan(2) }) {
                ScannerHero(onScanClick = onScanClick)
            }

            // 2. Section Header
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Masakan Populer", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "LIHAT SEMUA",
                        color = Terracotta,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onSeeAllClick() }.padding(8.dp)
                    )
                }
            }

            // 3. Loading State or Recipe Items
            if (isLoading && recipes.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Terracotta)
                    }
                }
            } else {
                items(recipes) { recipe ->
                    RecipeCard(recipe = recipe, onClick = { onRecipeClick(recipe.id) })
                }
            }
        }
    }
}

@Composable
fun ScannerHero(onScanClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            // Pulse Effect
            Box(Modifier.size(180.dp * pulseScale).clip(CircleShape).background(Terracotta.copy(0.1f)))
            Box(Modifier.size(150.dp * pulseScale).clip(CircleShape).background(Terracotta.copy(0.15f)))

            Surface(
                shape = CircleShape,
                color = Terracotta,
                shadowElevation = 6.dp,
                modifier = Modifier.size(110.dp),
                onClick = onScanClick
            ) {
                Icon(
                    Icons.Default.CenterFocusStrong,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(28.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Pindai Bahan", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Temukan resep berdasarkan bahan makanan yang kamu punya",
            color = SecondaryText,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun RecipeCard(recipe: RecipeUiModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, WhisperBorder)
    ) {
        Column {
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.3f).background(Color(0xFFF3F3F3))
            )
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = recipe.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, null, tint = Terracotta, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${recipe.loves} suka", color = SecondaryText, fontSize = 11.sp)
                }
            }
        }
    }
}