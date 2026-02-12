package com.skripsi.chefly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.RecipeRepository
import com.skripsi.chefly.ui.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: RecipeViewModel,
    onRecipeClick: (Int) -> Unit
) {
    val recipes = remember { RecipeRepository.getAllRecipes() }
    var searchQuery by remember { mutableStateOf("") }

    val allSelectedIngredients = viewModel.getAllSelectedIngredients()

    val filteredRecipes = remember(searchQuery, viewModel.detectedIngredients, viewModel.fridgeIngredients) {
        if (allSelectedIngredients.isNotEmpty() && searchQuery.isEmpty()) {
            RecipeRepository.searchRecipesByIngredients(allSelectedIngredients)
        } else if (searchQuery.isNotEmpty()) {
            recipes.filter { recipe ->
                recipe.name.contains(searchQuery, ignoreCase = true) ||
                recipe.ingredients.any { it.contains(searchQuery, ignoreCase = true) }
            }
        } else {
            recipes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chefly - Recipe App") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search recipes or ingredients...") },
                singleLine = true
            )

            // Show detected ingredients chip
            if (allSelectedIngredients.isNotEmpty() && searchQuery.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Showing recipes with your ingredients:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (viewModel.detectedIngredients.isNotEmpty()) {
                            Text(
                                text = "📸 Detected: ${viewModel.detectedIngredients.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (viewModel.fridgeIngredients.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🧊 From Fridge: ${viewModel.fridgeIngredients.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Recipe list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRecipes) { recipe ->
                    val matchInfo = if (allSelectedIngredients.isNotEmpty()) {
                        RecipeRepository.getMatchingIngredientsCount(recipe.id, allSelectedIngredients)
                    } else {
                        null
                    }

                    RecipeCard(
                        recipe = recipe,
                        isFavorite = viewModel.isFavorite(recipe.id),
                        onFavoriteClick = { viewModel.toggleFavorite(recipe.id) },
                        onClick = { onRecipeClick(recipe.id) },
                        matchingIngredients = matchInfo?.first,
                        totalIngredients = matchInfo?.second
                    )
                }

                if (filteredRecipes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recipes found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeCard(
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show matching ingredients badge if available
                    if (matchingIngredients != null && totalIngredients != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "✓ $matchingIngredients/$totalIngredients match",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Chip(text = recipe.difficulty)
                    Chip(text = recipe.category)
                }
            }

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun Chip(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

