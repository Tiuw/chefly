package com.skripsi.chefly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.skripsi.chefly.ui.viewmodel.RecipeDetailViewModel
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder

// Palette Warna
val Terracotta = Color(0xFFE36C47)
val SoftSage = Color(0xFF8FAF9B)
val ErrorCoral = Color(0xFFEF4444)
val OnSurfaceVariant = Color(0xFF57423C)
val WhisperBorder = Color(0xFFCBD5E1).copy(alpha = 0.3f)
val SurfaceContainerLow = Color(0xFFFFF1ED)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    navController: NavHostController,
    recipeId: String,
    viewModel: RecipeDetailViewModel
) {
    val recipe by viewModel.recipe.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.loadError.collectAsState()

    // Trigger load data saat screen dibuka
    LaunchedEffect(recipeId) {
        viewModel.loadRecipe(recipeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chefly", color = Terracotta, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    // Di dalam RecipeDetailScreen.kt pada bagian klik back:
                    IconButton(onClick = {
                        navController.navigateUp() // 🟢 Menggunakan navigateUp agar tumpukan backstack di atasnya dilepas secara alami tanpa menghancurkan state bawah
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // --- Hero Section ---
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                            AsyncImage(
                                model = currentRecipe.imageUrl,
                                contentDescription = currentRecipe.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .background(Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                        startY = 200f
                                    ))
                            )
                            Text(
                                text = currentRecipe.name,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                            )

                            // FITUR BARU: Tombol Simpan di Pojok Kanan Atas Gambar
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
                                    contentDescription = "Simpan Resep",
                                    tint = Terracotta,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        // --- Content ---
                        Column(modifier = Modifier.padding(16.dp)) {
                            IngredientMatchBar(100) // Contoh 100%

                            Spacer(modifier = Modifier.height(24.dp))

                            // Gunakan list dari model Recipe.kt
                            IngredientsSection(currentRecipe.ingredientList)

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 24.dp),
                                thickness = 1.dp,
                                color = WhisperBorder
                            )

                            // Gunakan list dari model Recipe.kt
                            InstructionsSection(currentRecipe.stepList)

                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IngredientMatchBar(percentage: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("KECOCOKAN BAHAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text("$percentage%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Terracotta)
        }
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = Terracotta,
            trackColor = WhisperBorder
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IngredientsSection(ingredients: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Bahan yang diperlukan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ingredients.forEach { name ->
                IngredientChip(name = name, isCheck = true)
            }
        }
    }
}

@Composable
fun IngredientChip(name: String, isCheck: Boolean) {
    Surface(
        shape = CircleShape,
        border = BorderStroke(1.dp, if (isCheck) WhisperBorder else ErrorCoral.copy(alpha = 0.3f)),
        color = if (isCheck) Color.White else ErrorCoral.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isCheck) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                contentDescription = null,
                tint = if (isCheck) SoftSage else ErrorCoral,
                modifier = Modifier.size(18.dp)
            )
            Text(text = name, fontSize = 14.sp, color = if (isCheck) OnSurfaceVariant else ErrorCoral)
        }
    }
}

@Composable
fun InstructionsSection(steps: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Instruksi", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        steps.forEachIndexed { index, step ->
            InstructionStep(number = (index + 1).toString().padStart(2, '0'), description = step)
        }
    }
}

@Composable
fun InstructionStep(number: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = SurfaceContainerLow
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = number, color = Terracotta, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = description,
            fontSize = 15.sp,
            color = OnSurfaceVariant,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f)
        )
    }
}