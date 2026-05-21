package com.skripsi.chefly.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.ui.theme.*
import com.skripsi.chefly.ui.viewmodel.CategoryData
import com.skripsi.chefly.ui.viewmodel.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    initialQuery: String = "",
    onRecipeClick: (String, Float) -> Unit, // 🟢 Sudah sinkron menerima ID dan Score
    onScanClick: () -> Unit,
    viewModel: RecipeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && initialQuery != uiState.searchQuery) {
            viewModel.onSearchQueryChanged(initialQuery)
        }
    }

    Scaffold(
        topBar = { ExploreTopBar() },
        containerColor = WarmIvory
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SearchBarSection(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) }
                )
            }

            item {
                Column {
                    Text(
                        text = "Metode Memasak",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.cookingMethods) { method ->
                            FilterChip(
                                selected = uiState.selectedMethod == method,
                                onClick = { viewModel.onMethodSelected(method) },
                                label = { Text(method, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(999.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CheflyPrimaryContainer,
                                    selectedLabelColor = CheflyOnPrimaryContainer,
                                    containerColor = PureSurface,
                                    labelColor = CheflyOnSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            item {
                CategoriesSection(
                    categories = uiState.categories,
                    onCategoryClick = { viewModel.onCategorySelected(it) }
                )
            }

            item {
                Text(
                    text = if (uiState.isAiSearchActive) "Hasil Perangkingan Cosine Similarity" else "Rekomendasi untuk Anda",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(
                items = uiState.recipes,
                key = { it.id }
            ) { recipe ->
                ExtendedRecipeCard(
                    recipe = recipe,
                    isAiMode = uiState.isAiSearchActive,
                    currentQuery = uiState.searchQuery,
                    onClick = {
                        // 🟢 FIX MATCH: Terbungkus blok lambda untuk mencegah Type Mismatch Error
                        onRecipeClick(recipe.id, recipe.similarity)
                    },
                    onFavoriteClick = { viewModel.toggleFavorite(recipe) }
                )
            }

            if (uiState.isLoadMore) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Terracotta)
                    }
                }
            }

            if (uiState.isEndReached && uiState.recipes.isNotEmpty()) {
                item {
                    Text(
                        text = "Semua resep telah dimuat",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreTopBar() {
    TopAppBar(
        title = { Text("Chefly", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Terracotta) },
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Menu, contentDescription = null, tint = Terracotta)
            }
        },
        actions = {
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBviyGcZOdfBHa_0CH1RQ85ukRNCvllycYFIz_RakQtYAuWev9AdYkExjBowHy2YL8xV_ZadG0H_pETTo9dOsNx9CDaYNLizos5sDl7HOjd4jYoiT3nxJoZa8XdJNVb-LKUZC5xB_wPmsMKajHh9AsPo2pImJ3PabJCTQ3DDEMDSZ67UC-zskWqechXnIusH2kvuBs7i3hnOLypq_Z93awntBsEDJr1LwsB-t2ak_M1aUQrn94gkPkl3Bgh8dJwTwy95xgvaiPOXYM",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
        modifier = Modifier.shadow(1.dp)
    )
}

@Composable
fun SearchBarSection(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Cari resep, bahan...", color = MutedSlate) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = MutedSlate) },
        trailingIcon = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Tune, null, tint = Color.Black)
            }
        },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Terracotta,
            unfocusedBorderColor = Color(0xFFE2E8F0).copy(alpha = 0.3f)
        )
    )
}

@Composable
fun CategoriesSection(categories: List<CategoryData>, onCategoryClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Kategori", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(categories) { category ->
                CategoryCard(category = category, onClick = { onCategoryClick(category.name) })
            }
        }
    }
}

@Composable
fun CategoryCard(category: CategoryData, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = PureSurface,
            border = BorderStroke(
                width = if (category.isActive) 2.dp else 1.dp,
                color = if (category.isActive) Terracotta else WhisperBorder
            ),
            shadowElevation = 1.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    category.icon,
                    contentDescription = null,
                    tint = if (category.isActive) Terracotta else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = category.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (category.isActive) Terracotta else Color.Gray
        )
    }
}

@Composable
fun ExtendedRecipeCard(
    recipe: Recipe,
    isAiMode: Boolean,
    currentQuery: String,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    // 🟢 LOCK CONDITION: Memisahkan nama bahan HANYA jika query terisi DAN flag isAiMode aktif
    val ingredientAnalysis = remember(recipe, currentQuery, isAiMode) {
        val allRecipeIngredients = recipe.ingredientList

        if (currentQuery.isNotBlank() && isAiMode) {
            val userTokens = currentQuery.split(Regex("[,\\s]+"))
                .map { it.trim().lowercase().replace("_", "") }
                .filter { it.isNotEmpty() }

            val availableList = mutableListOf<String>()
            val missingList = mutableListOf<String>()

            for (ingredient in allRecipeIngredients) {
                val cleanedIngredient = ingredient.lowercase().replace(" ", "")
                val isMatched = userTokens.any { token -> cleanedIngredient.contains(token) }

                if (isMatched) {
                    availableList.add(ingredient)
                } else {
                    missingList.add(ingredient)
                }
            }

            Pair(availableList, missingList)
        } else {
            Pair(emptyList<String>(), allRecipeIngredients)
        }
    }

    val (availableIngredients, missingIngredients) = ingredientAnalysis

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.clickable { onClick() }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(192.dp)
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

                if (isAiMode) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Analytics, null, tint = Terracotta, modifier = Modifier.size(18.dp))
                            val displayScore = (recipe.similarity * 100).toInt()
                            Text(text = "$displayScore% Cocok", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Terracotta)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = recipe.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF241916),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onFavoriteClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (recipe.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = if (recipe.isFavorite) Terracotta else MutedSlate
                        )
                    }
                }

                // 🟢 HIDE CONDITIONAL LAYOUT: Menyembunyikan boks bahan sepenuhnya jika dalam mode search manual teks
                if (isAiMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (availableIngredients.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SoftSage.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = SoftSage, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                                val availableText = availableIngredients.take(3).joinToString(", ")
                                val extraAvailable = if (availableIngredients.size > 3) " (+${availableIngredients.size - 3} lainnya)" else ""
                                Text(
                                    text = "Tersedia: $availableText$extraAvailable",
                                    fontSize = 12.sp, color = SoftSage, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (missingIngredients.isNotEmpty() && currentQuery.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ErrorCoral.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AddCircle, null, tint = ErrorCoral, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                                val missingText = missingIngredients.take(3).joinToString(", ")
                                val extraMissing = if (missingIngredients.size > 3) " (+${missingIngredients.size - 3} lainnya)" else ""
                                Text(
                                    text = "Butuh: $missingText$extraMissing",
                                    fontSize = 12.sp, color = ErrorCoral, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else {
                    // Berikan sedikit jarak napas pengganti boks jika dalam mode resep reguler
                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalDivider(color = Color(0xFFCBD5E1).copy(alpha = 0.3f), thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Schedule, null, tint = MutedSlate, modifier = Modifier.size(18.dp))
                            Text("35m", fontSize = 14.sp, color = MutedSlate)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Restaurant, null, tint = MutedSlate, modifier = Modifier.size(18.dp))
                            val difficulty = if (recipe.stepList.size > 8) "Sedang" else "Mudah"
                            Text(difficulty, fontSize = 14.sp, color = MutedSlate)
                        }
                    }
                    Text(text = "Lihat Detail", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Terracotta)
                }
            }
        }
    }
}