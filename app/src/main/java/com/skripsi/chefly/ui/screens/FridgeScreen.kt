package com.skripsi.chefly.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skripsi.chefly.ui.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridgeScreen(
    viewModel: RecipeViewModel
) {
    val fridgeIngredients = remember {
        listOf(
            "Nasi",
            "Gula",
            "Garam",
            "Kecap",
            "Ketumbar",
            "Kemiri",
            "Lada",
            "Jahe",
            "Lengkuas",
            "Kunyit",
            "Kencur",
            "Daun Jeruk",
            "Sereh (Serai)",
            "Daun Salam",
            "Gula Merah"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("What's in My Fridge") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select the ingredients you currently have to get smarter recipe recommendations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(fridgeIngredients) { ingredient ->
                FridgeIngredientItem(
                    ingredient = ingredient,
                    isSelected = viewModel.isIngredientInFridge(ingredient),
                    onToggle = { viewModel.toggleFridgeIngredient(ingredient) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Selected Ingredients",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${viewModel.fridgeIngredients.size} items",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (viewModel.fridgeIngredients.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { viewModel.clearFridgeIngredients() }
                            ) {
                                Text("Clear All")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun FridgeIngredientItem(
    ingredient: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = ingredient,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

