package com.skripsi.chefly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.ui.viewmodel.RecipeDetailViewModel
import com.skripsi.chefly.ui.viewmodel.SharedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    sharedViewModel: SharedViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val detailViewModel: RecipeDetailViewModel = viewModel()
    val isFavorite = sharedViewModel.favoriteRecipes.collectAsState()
    val recipeState = detailViewModel.recipe.collectAsState()
    val isLoading = detailViewModel.isLoading.collectAsState()
    val loadError = detailViewModel.loadError.collectAsState()

    val recipe = recipeState.value

    LaunchedEffect(Unit) {
        detailViewModel.loadRecipe(context, recipeId)
    }

    if (recipe == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TopAppBar(
                title = {
                    Text(
                        "RECIPE NOT FOUND",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                modifier = Modifier.border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                ),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Recipe not found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    recipe.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { sharedViewModel.toggleFavorite(recipeId) }) {
                    Icon(
                        imageVector = if (isFavorite.value.contains(recipeId))
                            Icons.Default.Favorite
                        else
                            Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite.value.contains(recipeId))
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            modifier = Modifier.border(
                width = 3.dp,
                color = MaterialTheme.colorScheme.primary
            ),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        RecipeDetailContent(
            recipe = recipe,
            detailViewModel = detailViewModel,
            sharedViewModel = sharedViewModel,
            recipeId = recipeId,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun RecipeDetailContent(
    recipe: Recipe,
    detailViewModel: RecipeDetailViewModel,
    sharedViewModel: SharedViewModel,
    recipeId: String,
    modifier: Modifier = Modifier
) {
    val isFavorite = sharedViewModel.favoriteRecipes.collectAsState()
    val cleanedIngredients = detailViewModel.getCleanedIngredients()
    val cleanedSteps = detailViewModel.getCleanedSteps()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Recipe Hero Image - IMPROVED with gradient overlay
        item {
            val imageUrl = recipe.imageUrl.trim()
            val isValidImageUrl = imageUrl.isNotEmpty() &&
                (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))

            if (isValidImageUrl) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)  // Taller image
                ) {
                    // Main Image with rounded bottom corners
                    SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = recipe.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(40.dp))
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Image error", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    )

                    // Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.3f)
                                    ),
                                    startY = 100f
                                )
                            )
                    )

                    // Favorite Button (Top Right)
                    IconButton(
                        onClick = { sharedViewModel.toggleFavorite(recipeId) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.9f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isFavorite.value.contains(recipeId))
                                Icons.Default.Favorite
                            else
                                Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite.value.contains(recipeId))
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Recipe Title & Category - BAUHAUS STYLE
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary)
                    .background(Color.White)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                Text(
                    text = recipe.name.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Category Badge - BAUHAUS
                Surface(
                    shape = RoundedCornerShape(0.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "📁 ${recipe.category}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(10.dp, 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Stats Row - BAUHAUS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primaryContainer)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (recipe.totalSteps != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⏱️", fontSize = 20.sp)
                            Text(
                                text = "${recipe.totalSteps}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "STEPS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (recipe.totalIngredients != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🥘", fontSize = 20.sp)
                            Text(
                                text = "${recipe.totalIngredients}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "INGREDIENTS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Ingredients Section - BAUHAUS
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "INGREDIENTS",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        // Ingredient Items
        items(cleanedIngredients.size) { index ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primaryContainer),
                color = Color.White,
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(24.dp)
                    )

                    Text(
                        text = cleanedIngredients[index],
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Steps Section - BAUHAUS
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "STEPS (${cleanedSteps.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        // Step Items - BAUHAUS
        items(cleanedSteps.size) { index ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary),
                color = Color.White,
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .border(2.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(0.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 20.sp
                            )
                        }
                    }

                    Text(
                        text = cleanedSteps[index],
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        lineHeight = 1.5.em,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun cleanRecipeText(raw: String): String {
    return raw
        .trim()
        .replace(Regex("^\\[\\s*"), "")
        .replace(Regex("\\s*]$"), "")
        .replace(Regex("^\""), "")
        .replace(Regex("\"$"), "")
        .replace(Regex("^'"), "")
        .replace(Regex("'$"), "")
        .replace("\\\"", "\"")
        .trim()
}

@Composable
fun CompactStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

