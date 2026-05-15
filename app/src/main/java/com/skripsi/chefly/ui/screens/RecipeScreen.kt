package com.skripsi.chefly.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.skripsi.chefly.data.local.entity.RecipeEntity
import com.skripsi.chefly.ui.theme.*
import com.skripsi.chefly.ui.viewmodel.CategoryData
import com.skripsi.chefly.ui.viewmodel.RecipeUIState
import com.skripsi.chefly.ui.viewmodel.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    onRecipeClick: (String) -> Unit,
    onScanClick: () -> Unit,
    viewModel: RecipeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    // Logika Deteksi Paging (Infinite Scroll)
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            // Trigger jika user sudah melihat 2 item terakhir
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 2
        }
    }

    // Jalankan loadNextPage saat user scroll ke bawah
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !uiState.isLoadMore && !uiState.isEndReached) {
            viewModel.loadNextPage()
        }
    }

    Scaffold(
        topBar = { ExploreTopBar() },
        containerColor = WarmIvory // Sesuai tema skripsi Anda
    ) { innerPadding ->

        // Sumber scroll tunggal agar paging & layout terbaca dengan benar
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            // Memberikan jarak napas di pinggir layar (Solusi image_9d4c37.png)
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 120.dp // Agar tidak tertutup Bottom Bar
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp), // Jarak antar kolom
            verticalArrangement = Arrangement.spacedBy(24.dp)   // Jarak antar baris
        ) {

            // 1. Section Search Bar
            item(span = { GridItemSpan(2) }) {
                SearchBarSection(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) }
                )
            }

            item(span = { GridItemSpan(2) }) {
                Column {
                    Text(
                        "Metode Memasak",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.cookingMethods) { method ->
                            FilterChip(
                                selected = uiState.selectedMethod == method,
                                onClick = { viewModel.onMethodSelected(method) },
                                label = { Text(method) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Terracotta,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 2. Section Kategori (Horizontal Scroll tetap di dalam item ini)
            item(span = { GridItemSpan(2) }) {
                CategoriesSection(
                    categories = uiState.categories,
                    onCategoryClick = { viewModel.onCategorySelected(it) }
                )
            }

            // 3. Header Label
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Rekomendasi untuk Anda",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(
                items = uiState.recipes,
                key = { it.id }
            ) { recipe ->
                RecipeGridItem(
                    recipe = recipe,
                    onClick = { onRecipeClick(recipe.id) },
                    // TAMBAHKAN INI: Sekarang Icon Hati bisa diklik langsung
                    onFavoriteClick = { viewModel.toggleFavorite(recipe) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5. Loading Indicator saat Paging (Bawah)
            if (uiState.isLoadMore) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFFE36C47) // Terracotta
                        )
                    }
                }
            }

            // 6. Pesan jika data sudah habis
            if (uiState.isEndReached && uiState.recipes.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
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
        title = {
            Text(
                "Resep",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Terracotta
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@Composable
fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari resep, bahan...", color = MutedSlate) },
            leadingIcon = {
                // Ikon diletakkan di sini, bukan di KeyboardOptions
                Icon(Icons.Default.Search, contentDescription = null, tint = MutedSlate)
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            // PERBAIKAN DI SINI:
            keyboardOptions = KeyboardOptions(
                autoCorrect = false, // Mematikan auto-correct untuk pencarian bahan resep
                imeAction = androidx.compose.ui.text.input.ImeAction.Search // Gunakan ImeAction, bukan Icon
            ),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedIndicatorColor = WhisperBorder,
                focusedIndicatorColor = Terracotta,
                cursorColor = Terracotta
            )
        )
    }
}

@Composable
fun CategoriesSection(
    categories: List<CategoryData>,
    onCategoryClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            "Kategori",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories) { category ->
                CategoryCard(
                    category = category,
                    onClick = { onCategoryClick(category.name) }
                )
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
            color = Color.White,
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
            category.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (category.isActive) Terracotta else Color.Gray
        )
    }
}

@Composable
fun RecipeGridItem(
    recipe: com.skripsi.chefly.data.Recipe,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
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

            // Surface untuk Tombol Tersimpan (Bookmark)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clickable(
                        onClick = onFavoriteClick,
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.9f)
            ) {
                Icon(
                    // GANTI DI SINI: Pakai Bookmark agar konsisten dengan SavedScreen
                    imageVector = if (recipe.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = Terracotta,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = recipe.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )
    }
}