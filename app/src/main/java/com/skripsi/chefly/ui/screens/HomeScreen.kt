package com.skripsi.chefly.ui.screens

import android.util.Log
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.repository.RecipeRepository
import com.skripsi.chefly.ui.RecipeViewModel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: RecipeViewModel,
    resetSearchTrigger: Int = 0,
    onRecipeClick: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var totalRecipes by remember { mutableStateOf(0) }
    var paginatedRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var filteredRecipesState by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var currentPage by remember { mutableStateOf(0) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isInitialLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var lastSearchQuery by remember { mutableStateOf("") }
    var lastIngredientSearchList by remember { mutableStateOf<List<String>>(emptyList()) }
    val allSelectedIngredients = viewModel.getAllSelectedIngredients()

    val lazyListState = rememberLazyListState()

    // Initial load
    LaunchedEffect(Unit) {
        try {
            isInitialLoading = true
            RecipeRepository.init(context)
            totalRecipes = RecipeRepository.getRecipeCount(context)
            loadError = null
        } catch (e: Exception) {
            loadError = "Failed to load"
        } finally {
            isInitialLoading = false
        }
    }

    // Load first page
    LaunchedEffect(isInitialLoading, totalRecipes) {
        if (!isInitialLoading && totalRecipes > 0 && paginatedRecipes.isEmpty()) {
            try {
                isLoadingMore = true
                val recipes = RecipeRepository.getRecipesPaged(context, 0)
                paginatedRecipes = recipes
                currentPage = 1
                filteredRecipesState = recipes
                lastIngredientSearchList = emptyList()  // Reset to trigger auto-search if have ingredients
            } catch (e: Exception) {
                loadError = "Error loading recipes"
            } finally {
                isLoadingMore = false
            }
        }
    }

    // IMPORTANT: Check if we need to auto-search on mount with selected ingredients
    LaunchedEffect(Unit) {
        if (allSelectedIngredients.isNotEmpty() && 
            paginatedRecipes.isNotEmpty() &&
            lastIngredientSearchList.isEmpty()) {  // Only if not yet searched
            lastIngredientSearchList = allSelectedIngredients
            try {
                isSearching = true
                val results = RecipeRepository.searchRecipesByIngredientsSusp(
                    context,
                    allSelectedIngredients
                )
                filteredRecipesState = results
                lastSearchQuery = "ingredient_search"
                isSearching = false
            } catch (e: Exception) {
                loadError = "Error searching recipes"
                isSearching = false
            }
        }
    }

    // Infinite scroll detection
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                if (visibleItems.isNotEmpty() && !isSearching) {
                    val lastVisibleItem = visibleItems.last()
                    val totalItems = lazyListState.layoutInfo.totalItemsCount

                    if (lastVisibleItem.index >= totalItems - 3 && !isLoadingMore && searchQuery.isEmpty() && allSelectedIngredients.isEmpty()) {
                        val estimatedTotal = totalRecipes
                        if (paginatedRecipes.size < estimatedTotal) {
                            scope.launch {
                                try {
                                    isLoadingMore = true
                                    val nextRecipes = RecipeRepository.getRecipesPaged(context, currentPage)
                                    if (nextRecipes.isNotEmpty()) {
                                        paginatedRecipes = paginatedRecipes + nextRecipes
                                        currentPage++
                                    }
                                } catch (e: Exception) {
                                    loadError = "Error loading more"
                                } finally {
                                    isLoadingMore = false
                                }
                            }
                        }
                    }
                }
            }
    }

    // Search with debounce - IMPROVED with longer delay and proper cancellation
    LaunchedEffect(searchQuery) {
        if (searchQuery != lastSearchQuery) {
            delay(800)  // Increased from 500ms to 800ms
            isSearching = true
            try {
                lastSearchQuery = searchQuery
                if (searchQuery.isEmpty()) {
                    filteredRecipesState = paginatedRecipes
                    isSearching = false
                } else {
                    val results = RecipeRepository.searchRecipesByQuery(context, searchQuery)
                    filteredRecipesState = results
                    isSearching = false
                }
            } catch (e: Exception) {
                isSearching = false
            }
        }
    }

    // Reset search state when caller requests (e.g., back from detail screen)
    LaunchedEffect(resetSearchTrigger) {
        if (resetSearchTrigger > 0) {
            searchQuery = ""
            lastSearchQuery = ""
            isSearching = false
            filteredRecipesState = paginatedRecipes
        }
    }


    val displayedRecipes = remember(filteredRecipesState) {
        filteredRecipesState.mapNotNull { r -> r.id?.let { id -> r to id } }
    }

    var isScrolledDown by remember { mutableStateOf(false) }
    var matchingIngredientsCache by remember { mutableStateOf<Map<String, Pair<Int, Int>>>(emptyMap()) }
    var cacheComputationInProgress by remember { mutableStateOf(false) }
    var hasInitializedSearch by remember { mutableStateOf(false) }

    // Detect scroll position for FAB visibility
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { firstVisibleIndex ->
                isScrolledDown = firstVisibleIndex > 2
            }
    }

    // Pre-compute matching ingredients for all recipes - ENSURE it runs even when back
    LaunchedEffect(Unit) {
        snapshotFlow { Pair(filteredRecipesState, allSelectedIngredients) }
            .collect { (recipes, ingredients) ->
                if (ingredients.isNotEmpty() && recipes.isNotEmpty()) {
                    cacheComputationInProgress = true
                    scope.launch {
                        try {
                            val cache = mutableMapOf<String, Pair<Int, Int>>()
                            recipes.forEach { recipe ->
                                recipe.id?.let { id ->
                                    val matchInfo = RecipeRepository.getMatchingIngredientsCountSuspend(context, id, ingredients)
                                    if (matchInfo != null) {
                                        cache[id] = matchInfo
                                    }
                                }
                            }
                            matchingIngredientsCache = cache
                            cacheComputationInProgress = false
                        } catch (e: Exception) {
                            Log.e("HomeScreen", "Error computing matching ingredients", e)
                            cacheComputationInProgress = false
                        }
                    }
                } else {
                    matchingIngredientsCache = emptyMap()
                    cacheComputationInProgress = false
                }
            }
    }

    // Auto-search when ingredients are selected (e.g., from Fridge screen)
    // IMPORTANT: Proper dependency tracking with proper tie-breaking
    LaunchedEffect(paginatedRecipes, allSelectedIngredients) {
        if (paginatedRecipes.isNotEmpty() && 
            allSelectedIngredients.isNotEmpty() && 
            !isInitialLoading) {
            
            val ingredientListHasChanged = allSelectedIngredients != lastIngredientSearchList || !hasInitializedSearch
            
            if (ingredientListHasChanged) {
                lastIngredientSearchList = allSelectedIngredients
                hasInitializedSearch = true
                delay(300)
                try {
                    isSearching = true
                    val results = RecipeRepository.searchRecipesByIngredientsSusp(
                        context,
                        allSelectedIngredients
                    )
                    filteredRecipesState = results
                    lastSearchQuery = "ingredient_search"
                    isSearching = false
                } catch (e: Exception) {
                    loadError = "Error searching recipes"
                    isSearching = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        // Search bar - IMPROVED
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(48.dp),
            placeholder = { Text("Cari resep, bahan...") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { searchQuery = "" }
                            .padding(4.dp)
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        // Total data loaded info
        if (!isInitialLoading && totalRecipes > 0) {
            Text(
                text = "Total: ${paginatedRecipes.size}/$totalRecipes Resep",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Main content
        when {
            isInitialLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                }
            }
            loadError != null && paginatedRecipes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: $loadError", color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Show selected fridge ingredients at top
                    if (allSelectedIngredients.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "🧊 Your Fridge (${allSelectedIngredients.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        allSelectedIngredients.forEach { ingredient ->
                                            Text(
                                                text = "✓ $ingredient",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    // Search button for ingredients
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    isSearching = true
                                                    lastSearchQuery = "ingredient_search"
                                                    val results = RecipeRepository.searchRecipesByIngredientsSusp(
                                                        context,
                                                        allSelectedIngredients
                                                    )
                                                    filteredRecipesState = results
                                                    isSearching = false
                                                } catch (e: Exception) {
                                                    loadError = "Error searching recipes"
                                                    isSearching = false
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text(
                                            "🔍 Cari di Resep",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

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
                        items(displayedRecipes.size) { index ->
                            val (recipe, id) = displayedRecipes[index]
                            val matchInfo = if (allSelectedIngredients.isNotEmpty()) {
                                matchingIngredientsCache[id]  // Use cache instead of computing here
                            } else {
                                null
                            }

                            MinimalRecipeCard(
                                recipe = recipe,
                                isFavorite = viewModel.isFavorite(id),
                                onFavoriteClick = { viewModel.toggleFavorite(id) },
                                onClick = { onRecipeClick(id) },
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

                        if (displayedRecipes.isEmpty() && !isSearching) {
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

        // Floating Action Button for ingredient search
        if (allSelectedIngredients.isNotEmpty() && isScrolledDown && searchQuery.isEmpty()) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        try {
                            isSearching = true
                            lastSearchQuery = "ingredient_search"
                            val results = RecipeRepository.searchRecipesByIngredientsSusp(
                                context,
                                allSelectedIngredients
                            )
                            filteredRecipesState = results
                            isSearching = false
                            // Scroll to top to show results
                            lazyListState.animateScrollToItem(0)
                        } catch (e: Exception) {
                            loadError = "Error searching recipes"
                            isSearching = false
                        }
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
