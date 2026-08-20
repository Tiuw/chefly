package com.skripsi.chefly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    initialCategory: String = "",
    onRecipeClick: (String, Float) -> Unit,
    onScanClick: () -> Unit,
    viewModel: RecipeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Memproses query pencarian atau filter kategori yang dilempar dari navigasi
    LaunchedEffect(initialQuery, initialCategory) {
        // Jika ada query dari pencarian
        if (initialQuery.isNotBlank() && initialQuery != uiState.searchQuery) {
            viewModel.onSearchQueryChanged(initialQuery)
        }

        // Jika ada kategori dari Quick Section
        if (initialCategory.isNotBlank() && !initialCategory.equals(uiState.selectedCategory, ignoreCase = true)) {
            viewModel.onCategorySelected(initialCategory)
        }
    }

    Scaffold(
        topBar = { ExploreTopBar() },
        containerColor = CheflyBackground
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Search Bar Section
            item {
                SearchBarSection(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) }
                )
            }

            // 2. Filter Kategori
            item {
                CategoriesSection(
                    categories = uiState.categories,
                    onCategoryClick = { viewModel.onCategorySelected(it) }
                )
            }

            // 3. Header Section
            item {
                Text(
                    text = if (uiState.searchQuery.isNotBlank()) "Hasil Pencarian Resep" else "Koleksi Resep Pilihan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepCharcoal,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // 4. State Loading Awal
            if (uiState.isLoading && uiState.recipes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Terracotta)
                    }
                }
            }

            // 5. State Kosong (Hasil Tidak Ditemukan)
            if (!uiState.isLoading && uiState.recipes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = SecondaryText,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak ada resep yang cocok dengan pencarian ini.",
                            fontSize = 14.sp,
                            color = SecondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 6. List Kartu Resep
            items(
                items = uiState.recipes,
                key = { it.id }
            ) { recipe ->
                ExtendedRecipeCard(
                    recipe = recipe,
                    onClick = { onRecipeClick(recipe.id, 0f) },
                    onFavoriteClick = { viewModel.toggleFavorite(recipe) }
                )
            }

            // 7. Paginasi Otomatis (Infinite Scroll)
            if (uiState.recipes.isNotEmpty() && !uiState.isEndReached && !uiState.isLoadMore) {
                item {
                    LaunchedEffect(Unit) {
                        viewModel.loadNextPage()
                    }
                }
            }

            // 8. Indicator Load More
            if (uiState.isLoadMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Terracotta)
                    }
                }
            }

            // 9. Footer Akhir Data
            if (uiState.isEndReached && uiState.recipes.isNotEmpty()) {
                item {
                    Text(
                        text = "Semua resep telah dimuat",
                        fontSize = 12.sp,
                        color = SecondaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
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
        title = {
            Text(
                text = "Eksplorasi Resep",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DeepCharcoal
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = CheflyBackground)
    )
}

@Composable
fun SearchBarSection(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Cari judul resep atau bahan...", color = SecondaryText) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = SecondaryText) },
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PureSurface,
            unfocusedContainerColor = PureSurface,
            focusedBorderColor = Terracotta,
            unfocusedBorderColor = WhisperBorder
        )
    )
}

@Composable
fun CategoriesSection(categories: List<CategoryData>, onCategoryClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "Kategori Bahan",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DeepCharcoal,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(60.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (category.isActive) CheflySurfaceContainerLow else PureSurface,
            border = BorderStroke(
                width = if (category.isActive) 1.5.dp else 1.dp,
                color = if (category.isActive) Terracotta else WhisperBorder
            ),
            shadowElevation = 0.5.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    category.icon,
                    contentDescription = null,
                    tint = if (category.isActive) Terracotta else SecondaryText,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Text(
            text = category.name,
            fontSize = 11.sp,
            fontWeight = if (category.isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (category.isActive) Terracotta else DeepCharcoal
        )
    }
}

@Composable
fun ExtendedRecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, WhisperBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(recipe.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Badge Kategori di atas gambar
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.BottomStart),
                    color = DeepCharcoal.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = recipe.category.uppercase(),
                        color = PureSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recipe.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepCharcoal,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = onFavoriteClick, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (recipe.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Simpan Favorit",
                                tint = if (recipe.isFavorite) Terracotta else MutedSlate
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Buka Detail",
                            tint = SecondaryText.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}