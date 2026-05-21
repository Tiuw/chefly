package com.skripsi.chefly.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.ui.viewmodel.RecipeDetailViewModel

// --- PALET WARNA DESIGN SYSTEM ---
val Terracotta = Color(0xFFE36C47)
val SoftSage = Color(0xFF8FAF9B)
val ErrorCoral = Color(0xFFEF4444)
val OnSurfaceVariant = Color(0xFF57423C)
val WhisperBorder = Color(0xFFCBD5E1).copy(alpha = 0.3f)
val SurfaceContainerLow = Color(0xFFFFF1ED)
val WarmIvory = Color(0xFFFAF7F2)
val PureSurface = Color(0xFFFFFFFF)
val MutedSlate = Color(0xFF6B7280)
val CheflySecondary = Color(0xFF5F5E5B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    navController: NavHostController,
    recipeId: String,
    currentQuery: String = "",
    passedSimilarity: Float = 0f,
    viewModel: RecipeDetailViewModel
) {
    val recipe by viewModel.recipe.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.loadError.collectAsState()

    LaunchedEffect(recipeId) {
        viewModel.loadRecipe(recipeId)
    }

    Scaffold(
        containerColor = WarmIvory,
        topBar = {
            TopAppBar(
                title = { Text("Chefly", color = Terracotta, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Terracotta)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.shadow(1.dp)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Terracotta)
            } else if (error != null) {
                Text(text = error!!, modifier = Modifier.align(Alignment.Center), color = ErrorCoral)
            } else {
                recipe?.let { currentRecipe ->

                    // 🟢 1. LOGIKA EVALUASI BAHAN (INTERSEKTOR)
                    val ingredientAnalysis = remember(currentRecipe, currentQuery) {
                        val allRecipeIngredients = currentRecipe.ingredientList

                        if (currentQuery.isNotBlank()) {
                            val userTokens = currentQuery.split(Regex("[,\\s]+"))
                                .map { it.trim().lowercase().replace("_", "") }
                                .filter { it.isNotEmpty() }

                            val availableList = mutableListOf<String>()
                            val missingList = mutableListOf<String>()

                            for (ingredient in allRecipeIngredients) {
                                val cleanedIngredient = ingredient.lowercase().replace(" ", "")
                                val isMatched = userTokens.any { token -> cleanedIngredient.contains(token) }

                                if (isMatched) availableList.add(ingredient) else missingList.add(ingredient)
                            }
                            Pair(availableList, missingList)
                        } else {
                            Pair(emptyList<String>(), allRecipeIngredients)
                        }
                    }
                    val (availableIngredients, missingIngredients) = ingredientAnalysis

                    // 🟢 2. SINKRONISASI SKOR COSINE SIMILARITY
                    val actualSimilarity = if (currentRecipe.similarity == 0f) passedSimilarity else currentRecipe.similarity
                    val displayScore = (actualSimilarity * 100).toInt()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // --- 1. Hero Image Section ---
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                            AsyncImage(
                                model = currentRecipe.imageUrl,
                                contentDescription = currentRecipe.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Blending Gradient Gelap Bawah Gambar (Dipertahankan untuk estetika kontras)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                            startY = 150f
                                        )
                                    )
                            )

                            // --- Info Kategori & Durasi Memasak (Teks Melayang di atas Hero Image) ---
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Terracotta.copy(alpha = 0.3f),
                                    border = BorderStroke(1.dp, Terracotta.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = currentRecipe.category.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (currentRecipe.stepList.size > 8) "35 MIN" else "15 MIN",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Tombol Simpan / Bookmark di Pojok Kanan Atas Gambar
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .size(40.dp)
                                    .clickable(
                                        onClick = { viewModel.toggleFavorite() },
                                        indication = null,
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    ),
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.9f),
                                shadowElevation = 2.dp
                            ) {
                                Icon(
                                    imageVector = if (currentRecipe.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = Terracotta,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        // --- 2. AI Match Score Banner (Hanya Muncul Jika Jalur AI) ---
                        if (actualSimilarity > 0f) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .offset(y = (-24).dp)
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = PureSurface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(48.dp).background(SoftSage.copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Analytics, null, tint = SoftSage)
                                            }
                                            Column {
                                                Text("AI MATCH SCORE", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MutedSlate)
                                                Text(
                                                    text = if (displayScore >= 85) "Sangat Cocok!" else "Cukup Cocok",
                                                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black
                                                )
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("$displayScore%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Terracotta)
                                            Text("MATCH", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MutedSlate)
                                        }
                                    }
                                }

                                // Progress Bar Animasi Mikro-interaksi
                                var progressTriggered by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) { progressTriggered = true }

                                val animatedProgress by animateFloatAsState(
                                    targetValue = if (progressTriggered) actualSimilarity else 0f,
                                    animationSpec = tween(durationMillis = 1200)
                                )

                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp).clip(CircleShape),
                                    color = Terracotta,
                                    trackColor = WhisperBorder
                                )
                            }
                        } else {
                            // Jarak napas pengganti spanduk melayang agar tidak terlalu menempel dengan batas gambar
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // --- 3. Content Section ---
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = if (actualSimilarity > 0f) 0.dp else 8.dp)
                        ) {

                            // Judul Resep Aman (Tidak bertabrakan dengan spanduk melayang)
                            Text(
                                text = currentRecipe.name,
                                color = Color.Black,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 34.sp,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Bahan-bahan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("${currentRecipe.ingredientList.size} Items", fontSize = 14.sp, color = MutedSlate)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // List Bahan A: Tersedia di Kulkas (Hanya Muncul Jika Jalur AI)
                            if (availableIngredients.isNotEmpty() && actualSimilarity > 0f) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Inventory2, null, tint = SoftSage, modifier = Modifier.size(18.dp))
                                    Text("TERSEDIA DI KULKAS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SoftSage, letterSpacing = 0.5.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                availableIngredients.forEach { name ->
                                    IngredientDetailRow(name = name, isAvailable = true)
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            // Sub-Header List Bahan B: Dinamis tergantung jenis pencarian aktif
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = if (actualSimilarity > 0f) Icons.Default.ShoppingBasket else Icons.Default.RestaurantMenu,
                                    contentDescription = null,
                                    tint = Terracotta,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (actualSimilarity > 0f) "PERLU DITAMBAHKAN" else "DAFTAR BAHAN BAKU",
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Terracotta, letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Isi Data Bahan B
                            if (actualSimilarity > 0f) {
                                missingIngredients.forEach { name ->
                                    IngredientDetailRow(name = name, isAvailable = false)
                                }
                            } else {
                                currentRecipe.ingredientList.forEach { name ->
                                    IngredientDetailRow(name = name, isAvailable = false)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), thickness = 1.dp, color = WhisperBorder)

                            // --- 4. Instruksi Section ---
                            Text("Instruksi Memasak", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))

                            currentRecipe.stepList.forEachIndexed { index, step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(16.dp))
                                        .background(PureSurface.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Surface(
                                        modifier = Modifier.size(32.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (index == 0) Terracotta else CheflySecondary
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = "${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                    Text(
                                        text = step,
                                        fontSize = 16.sp,
                                        color = OnSurfaceVariant,
                                        lineHeight = 24.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // --- 5. Footer Preview Metadata ---
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "\"AI Chefly memprediksi hasil sempurna dalam ${if (currentRecipe.stepList.size > 8) "35" else "15"} menit.\"",
                                fontSize = 14.sp, color = MutedSlate, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IngredientDetailRow(name: String, isAvailable: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PureSurface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isAvailable) Color(0xFFF0F0F0) else Terracotta.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isAvailable) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                contentDescription = null,
                tint = if (isAvailable) SoftSage else Terracotta,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = name,
                fontSize = 16.sp,
                color = Color.Black,
                fontWeight = FontWeight.Normal
            )
        }
    }
}