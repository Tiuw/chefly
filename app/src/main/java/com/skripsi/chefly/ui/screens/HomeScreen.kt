package com.skripsi.chefly.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.ui.viewmodel.HomeViewModel
import com.skripsi.chefly.ui.viewmodel.SharedViewModel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sharedViewModel: SharedViewModel,
    onRecipeClick: (String) -> Unit
) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel()
    val scope = rememberCoroutineScope()

    // State from ViewModels
    val totalRecipes by homeViewModel.totalRecipes.collectAsState()
    val paginatedRecipes by homeViewModel.paginatedRecipes.collectAsState()
    val filteredRecipes by homeViewModel.filteredRecipes.collectAsState()
    val isLoadingMore by homeViewModel.isLoadingMore.collectAsState()
    val isInitialLoading by homeViewModel.isInitialLoading.collectAsState()
    val loadError by homeViewModel.loadError.collectAsState()
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val isSearching by homeViewModel.isSearching.collectAsState()
    val matchingIngredientsCache by homeViewModel.matchingIngredientsCache.collectAsState()

    val allSelectedIngredients by sharedViewModel.allSelectedIngredients.collectAsState()
    val favorites by sharedViewModel.favoriteRecipes.collectAsState()

    val lazyListState = rememberLazyListState()
    var isScrolledDown by remember { mutableStateOf(false) }

    // Initialize on mount
    LaunchedEffect(Unit) {
        homeViewModel.initializeHomeScreen(context)
    }

    // Load first page after initialization
    LaunchedEffect(isInitialLoading, totalRecipes) {
        if (!isInitialLoading && totalRecipes > 0 && paginatedRecipes.isEmpty()) {
            homeViewModel.loadFirstPage(context)
        }
    }

    // Auto-search by ingredients when they change
    LaunchedEffect(paginatedRecipes, allSelectedIngredients) {
        if (paginatedRecipes.isNotEmpty() && allSelectedIngredients.isNotEmpty()) {
            homeViewModel.searchByIngredients(context, allSelectedIngredients)
        }
    }

    // Precompute matching ingredients cache
    LaunchedEffect(filteredRecipes, allSelectedIngredients) {
        if (filteredRecipes.isNotEmpty() && allSelectedIngredients.isNotEmpty()) {
            homeViewModel.precomputeMatchingIngredients(context, filteredRecipes, allSelectedIngredients)
        }
    }

    // Detect scroll position
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { firstVisibleIndex ->
                isScrolledDown = firstVisibleIndex > 2
            }
    }

    // Infinite scroll trigger
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                if (visibleItems.isNotEmpty() && !isSearching) {
                    val lastVisibleItem = visibleItems.last()
                    val totalItems = lazyListState.layoutInfo.totalItemsCount

                    if (lastVisibleItem.index >= totalItems - 3 && !isLoadingMore &&
                        searchQuery.isEmpty() && allSelectedIngredients.isEmpty()) {
                        if (paginatedRecipes.size < totalRecipes) {
                            homeViewModel.loadMoreRecipes(context)
                        }
                    }
                }
            }
    }

    // Search debounce
    LaunchedEffect(searchQuery) {
        homeViewModel.searchRecipes(context, searchQuery)
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar
            BauhausTopAppBar()

            // Main Content
            when {
                isInitialLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    }
                }
                loadError != null && paginatedRecipes.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Error: $loadError", color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        state = lazyListState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Hero Section
                        item {
                            BauhausHeroSection(
                                recipesCount = paginatedRecipes.size,
                                totalRecipes = totalRecipes,
                                onScanClick = { /* Handle scan click */ }
                            )
                        }

                        // Recently Detected Ingredients
                        if (allSelectedIngredients.isNotEmpty()) {
                            item {
                                BauhausRecentlyDetected(ingredients = allSelectedIngredients)
                            }
                        }

                        // Search Bar
                        item {
                            BauhausSearchBar(
                                value = searchQuery,
                                onValueChange = { homeViewModel.setSearchQuery(it) }
                            )
                        }

                        // Total Data Info
                        if (!isInitialLoading && totalRecipes > 0) {
                            item {
                                Text(
                                    text = "Total: ${paginatedRecipes.size}/$totalRecipes Resep",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }

                        // Recipes List
                        if (isSearching) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                }
                            }
                        } else {
                            items(filteredRecipes.size) { index ->
                                val recipe = filteredRecipes[index]
                                val isFav = recipe.id?.let { favorites.contains(it) } ?: false
                                val matchInfo = recipe.id?.let { matchingIngredientsCache[it] }

                                MinimalRecipeCard(
                                    recipe = recipe,
                                    isFavorite = isFav,
                                    onFavoriteClick = {
                                        recipe.id?.let { sharedViewModel.toggleFavorite(it) }
                                    },
                                    onClick = { recipe.id?.let { onRecipeClick(it) } },
                                    matchingIngredients = if (allSelectedIngredients.isNotEmpty()) matchInfo?.first else null,
                                    totalIngredients = if (allSelectedIngredients.isNotEmpty()) matchInfo?.second else null
                                )
                            }

                            if (isLoadingMore && !isSearching) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                    }
                                }
                            }

                            if (filteredRecipes.isEmpty() && !isSearching) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Resep tidak ditemukan",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB for ingredient search
        if (allSelectedIngredients.isNotEmpty() && isScrolledDown && searchQuery.isEmpty()) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        homeViewModel.searchByIngredients(context, allSelectedIngredients)
                        lazyListState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    "🔍 Cari di Resep",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
fun MinimalRecipeCard(
    recipe: Recipe,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    matchingIngredients: Int? = null,
    totalIngredients: Int? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            // Content
            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp
                )

                // Category Badge - IMPROVED
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "📁 ${recipe.category}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(6.dp, 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Matching ingredients (if present)
                if (matchingIngredients != null && totalIngredients != null) {
                    Text(
                        text = "✓ $matchingIngredients/$totalIngredients ingredients",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Favorite Button
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Bauhaus styled TopAppBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BauhausTopAppBar() {
    TopAppBar(
        title = {
            Text(
                "CHEFLY",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(100.dp)
            )
        },
        modifier = Modifier.border(
            width = 4.dp,
            color = MaterialTheme.colorScheme.primary
        ),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

/**
 * Bauhaus Hero Section - "WHAT'S IN YOUR FRIDGE?"
 */
@Composable
fun BauhausHeroSection(
    recipesCount: Int,
    totalRecipes: Int,
    onScanClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(0.dp)
            )
            .border(4.dp, MaterialTheme.colorScheme.primary)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "WHAT'S IN YOUR\nFRIDGE?",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            "Scan ingredients, get recipes instantly. The Bauhaus way of cooking starts with raw simplicity.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Button(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(4.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text(
                "START SCAN",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * Recently Detected Ingredients Display
 */
@Composable
fun BauhausRecentlyDetected(ingredients: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "RECENTLY DETECTED",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.primary)
        )

        Flow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ingredients.forEach { ingredient ->
                Surface(
                    modifier = Modifier
                        .border(2.dp, MaterialTheme.colorScheme.primary),
                    color = Color.White
                ) {
                    Text(
                        ingredient.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp, 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Bauhaus Search Bar
 */
@Composable
fun BauhausSearchBar(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(4.dp, MaterialTheme.colorScheme.primary),
        placeholder = {
            Text(
                "SEARCH FOR RECIPES, INGREDIENTS...",
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onValueChange("") }
                )
            }
        },
        shape = RoundedCornerShape(0.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
    )
}

/**
 * Simple Flow Layout Composable (since Compose doesn't have native Flow)
 */
@Composable
fun Flow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}
