package com.skripsi.chefly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.ui.theme.SoftSage
import com.skripsi.chefly.ui.theme.Terracotta
import com.skripsi.chefly.ui.theme.WarmIvory
import com.skripsi.chefly.ui.viewmodel.RecommendationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationScreen(
    ingredients: String, // Data bahan awal dalam format CSV
    onBackClick: () -> Unit,
    onAddMoreClick: () -> Unit, // Callback ke halaman Tambah/Pindai Bahan
    onRecipeClick: (String, Float) -> Unit,
    viewModel: RecommendationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(ingredients) {
        if (ingredients.isNotBlank()) {
            viewModel.getRecommendations(ingredients)
        }
    }

    if (uiState.isLoading) {
        RecipeLoadingScreen(query = uiState.ingredientsQuery)
    } else {
        Scaffold(
            containerColor = WarmIvory,
            topBar = {
                TopAppBar(
                    title = { Text("Rekomendasi Chefly", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Terracotta)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bahan Aktif
                item {
                    RecommendationHeader(
                        ingredients = uiState.ingredients,
                        onAddMore = onAddMoreClick
                    )
                }

                if (uiState.recipes.isEmpty()) {
                    item { EmptyRecommendationView() }
                } else {
                    items(items = uiState.recipes, key = { it.id }) { recipe ->
                        RecommendationRecipeCard(
                            recipe = recipe,
                            currentQuery = uiState.ingredientsQuery,
                            isAiMode = true,
                            onClick = { onRecipeClick(recipe.id, recipe.similarity) },
                            onFavoriteClick = { viewModel.toggleFavorite(recipe) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendationHeader(
    ingredients: List<String>,
    onAddMore: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Bahan yang digunakan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                TextButton(onClick = onAddMore) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = Terracotta)
                    Spacer(Modifier.width(4.dp))
                    Text("Ubah", fontWeight = FontWeight.Bold, color = Terracotta)
                }
            }

            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ingredients.forEach { name ->
                    Surface(
                        color = Terracotta.copy(alpha = 0.1f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Terracotta.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = name,
                            fontSize = 12.sp,
                            color = Terracotta,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeLoadingScreen(query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmIvory),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Terracotta)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Chefly sedang meracik rekomendasi...", fontWeight = FontWeight.Medium)
            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Berdasarkan: $query",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

@Composable
fun RecommendationRecipeCard(
    recipe: Recipe,
    currentQuery: String = "",
    isAiMode: Boolean = false,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val ingredientAnalysis = remember(recipe, currentQuery) {
        val allRecipeIngredients = recipe.ingredientList
        if (currentQuery.isNotBlank()) {
            val userTokens = currentQuery.split(Regex("[,\\s_]+"))
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }

            val availableCount = allRecipeIngredients.count { ingredient ->
                val cleaned = ingredient.lowercase().replace(" ", "").replace("_", "")
                userTokens.any { token -> cleaned.contains(token) }
            }
            Pair(availableCount, allRecipeIngredients.size - availableCount)
        } else {
            Pair(0, allRecipeIngredients.size)
        }
    }
    val (availableCount, missingCount) = ingredientAnalysis

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.clickable { onClick() }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(recipe.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Match Score Badge
                if (isAiMode && recipe.similarity > 0f) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        color = Terracotta,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${(recipe.similarity * 100).toInt()}% Match",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recipe.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF241916),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onFavoriteClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (recipe.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = if (recipe.isFavorite) Terracotta else Color.Gray
                        )
                    }
                }

                if (currentQuery.isNotBlank()) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("✓ $availableCount Tersedia", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = SoftSage)
                        )
                        SuggestionChip(
                            onClick = {},
                            label = { Text("+ $missingCount Kurang", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = Terracotta)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Lihat resep selengkapnya",
                    color = Terracotta,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun EmptyRecommendationView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Maaf, tidak ada resep yang cocok.", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Coba tambahkan bahan lain.", color = Color.Gray, fontSize = 14.sp)
        }
    }
}