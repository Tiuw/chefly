package com.skripsi.chefly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.ui.viewmodel.HomeFeedMode
import com.skripsi.chefly.ui.viewmodel.HomeViewModel
import com.skripsi.chefly.ui.viewmodel.SharedViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sharedViewModel: SharedViewModel,
    onScanClick: () -> Unit,
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
    val selectedCategory by homeViewModel.selectedCategory.collectAsState()
    val activeFeed by homeViewModel.activeFeed.collectAsState()
    val scanIngredients by homeViewModel.scanIngredients.collectAsState()

    val allSelectedIngredients by sharedViewModel.allSelectedIngredients.collectAsState()
    val favorites by sharedViewModel.favoriteRecipes.collectAsState()

    val lazyGridState = rememberLazyGridState()
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

    // Sync scan results from fridge/camera state
    LaunchedEffect(allSelectedIngredients, scanIngredients) {
        if (allSelectedIngredients.isNotEmpty() && allSelectedIngredients != scanIngredients) {
            homeViewModel.searchByIngredients(context, allSelectedIngredients)
        }
    }

    // Detect scroll position
    LaunchedEffect(lazyGridState) {
        snapshotFlow { lazyGridState.firstVisibleItemIndex }
            .collectLatest { firstVisibleIndex ->
                isScrolledDown = firstVisibleIndex > 2
            }
    }

    // Infinite scroll trigger
    LaunchedEffect(lazyGridState) {
        snapshotFlow { lazyGridState.layoutInfo.visibleItemsInfo }
            .collectLatest { visibleItems ->
                if (visibleItems.isNotEmpty() && !isSearching) {
                    val lastVisibleItem = visibleItems.last()
                    val totalItems = lazyGridState.layoutInfo.totalItemsCount

                    if (lastVisibleItem.index >= totalItems - 4 && !isLoadingMore &&
                        searchQuery.isEmpty() && activeFeed == HomeFeedMode.RECOMMENDED) {
                        if (paginatedRecipes.size < totalRecipes) {
                            homeViewModel.loadMoreRecipes(context)
                        }
                    }
                }
            }
    }

    // Search debounce - REDUCED to 300ms for snappier response
    LaunchedEffect(searchQuery) {
        homeViewModel.searchRecipes(context, searchQuery)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar
            BauhausTopAppBar()

            // Main Content
            when {
                isInitialLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    }
                }
                loadError != null && paginatedRecipes.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Error: $loadError", color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 170.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        state = lazyGridState,
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            HomeHeaderSection()
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            BauhausHeroSection(
                                totalRecipes = totalRecipes,
                                onScanClick = onScanClick
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            HomeFilterSection(
                                selectedCategory = selectedCategory,
                                scanIngredients = scanIngredients,
                                onSelectAll = { homeViewModel.showRecommendedFeed() },
                                onSelectCategory = { category -> homeViewModel.selectCategory(context, category) },
                                onShowLastScan = { homeViewModel.activateLastScanFeed() }
                            )
                        }

                        if (scanIngredients.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                BauhausScanSummaryCard(
                                    ingredients = scanIngredients,
                                    onShowResults = { homeViewModel.activateLastScanFeed() }
                                )
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            BauhausSearchBar(
                                value = searchQuery,
                                isSearching = isSearching,
                                onValueChange = { homeViewModel.setSearchQuery(it) }
                            )
                        }

                        if (totalRecipes > 0) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                HomeFeedHeader(
                                    activeFeed = activeFeed,
                                    selectedCategory = selectedCategory,
                                    resultCount = filteredRecipes.size,
                                    totalCount = totalRecipes
                                )
                            }
                        }

                        if (isSearching) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                        Text(
                                            "Mencari resep...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = filteredRecipes,
                                key = { it.id ?: it.name }
                            ) { recipe ->
                                val isFav = recipe.id?.let { favorites.contains(it) } ?: false
                                val matchInfo = recipe.id?.let { matchingIngredientsCache[it] }

                                HomeRecipeCard(
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

                            if (isLoadingMore && activeFeed == HomeFeedMode.RECOMMENDED) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                    }
                                }
                            }

                            if (filteredRecipes.isEmpty() && searchQuery.isNotEmpty() && !isLoadingMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
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

                            if (filteredRecipes.isEmpty() && searchQuery.isEmpty() && !isLoadingMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (activeFeed) {
                                                HomeFeedMode.CATEGORY -> "Resep kategori tidak ditemukan"
                                                HomeFeedMode.SCAN -> "Hasil scan belum tersedia"
                                                else -> "Belum ada resep untuk ditampilkan"
                                            },
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
                        lazyGridState.animateScrollToItem(0)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
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
                modifier = Modifier.weight(1f),
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

                // Category Badge
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

@Composable
fun BauhausHeroSection(
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(0.dp)
        ) {
            Text(
                text = "SCAN BAHAN MAKANAN",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Text(
            "Halo, mau masak apa hari ini?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Akses cepat ke resep dari bahan yang dipindai atau dipilih dari filter kategori.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "${totalRecipes}+ resep tersedia offline",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Button(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .border(4.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(0.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "SCAN BAHAN MAKANAN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun HomeHeaderSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Halo, mau masak apa hari ini?",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Scan bahan, filter kategori, atau cari resep manual dengan cepat.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HomeFilterSection(
    selectedCategory: String?,
    scanIngredients: List<String>,
    onSelectAll: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onShowLastScan: () -> Unit
) {
    val categories = remember {
        listOf("Ayam", "Kambing", "Ikan", "Sayuran", "Telur")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "FILTER CEPAT",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                BauhausFilterChip(
                    label = "Semua",
                    selected = selectedCategory == null,
                    onClick = onSelectAll
                )
            }

            lazyItems(categories) { category ->
                BauhausFilterChip(
                    label = category,
                    selected = selectedCategory?.equals(category, ignoreCase = true) == true,
                    onClick = { onSelectCategory(category) }
                )
            }

            if (scanIngredients.isNotEmpty()) {
                item {
                    BauhausFilterChip(
                        label = "Scan Terakhir",
                        selected = false,
                        onClick = onShowLastScan,
                        accentColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun BauhausFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        onClick = onClick,
        color = if (selected) accentColor else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = label.uppercase(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BauhausScanSummaryCard(
    ingredients: List<String>,
    onShowResults: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(3.dp, MaterialTheme.colorScheme.primary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "HASIL SCAN TERAKHIR",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            lazyItems(ingredients, key = { it }) { ingredient ->
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(0.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = ingredient.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Button(
            onClick = onShowResults,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(3.dp, MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "LIHAT HASIL SCAN",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun HomeFeedHeader(
    activeFeed: HomeFeedMode,
    selectedCategory: String?,
    resultCount: Int,
    totalCount: Int
) {
    val title = when (activeFeed) {
        HomeFeedMode.SEARCH -> "HASIL PENCARIAN"
        HomeFeedMode.CATEGORY -> "KATEGORI: ${selectedCategory ?: ""}".trim()
        HomeFeedMode.SCAN -> "HASIL SCAN TERAKHIR"
        HomeFeedMode.RECOMMENDED -> "REKOMENDASI POPULER"
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$resultCount resep ditampilkan dari $totalCount data",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HomeRecipeCard(
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(0.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    IconButton(onClick = onFavoriteClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = recipe.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = RoundedCornerShape(0.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "📁 ${recipe.category}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (matchingIngredients != null && totalIngredients != null) {
                    Text(
                        text = "✓ $matchingIngredients/$totalIngredients bahan cocok",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                recipe.loves?.let { loves ->
                    Text(
                        text = "❤️ $loves",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

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

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            lazyItems(ingredients, key = { it }) { ingredient ->
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

@Composable
fun BauhausSearchBar(
    value: String,
    isSearching: Boolean = false,
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
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingIcon = {
            if (value.isNotEmpty() && !isSearching) {
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
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        enabled = !isSearching
    )
}


