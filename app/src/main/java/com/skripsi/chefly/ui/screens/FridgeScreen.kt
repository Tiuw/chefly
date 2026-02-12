package com.skripsi.chefly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skripsi.chefly.ui.RecipeViewModel

data class IngredientCategory(
    val name: String,
    val items: List<String>
)

val INGREDIENT_CATEGORIES = listOf(
    IngredientCategory(
        name = "🥬 Sayuran",
        items = listOf(
            "Bayam", "Kangkung", "Selada", "Tomat", "Terong", "Labu siam",
            "Jagung manis", "Buncis", "Wortel", "Kentang", "Ubi", "Brokoli",
            "Kubis", "Kol", "Timun", "Paprika", "Zucchini", "Jamur kuping",
            "Jamur champignon", "Kacang panjang"
        )
    ),
    IngredientCategory(
        name = "🌶️ Cabe & Bawang",
        items = listOf(
            "Cabe merah keriting", "Cabe hijau besar", "Cabe rawit merah", "Cabe rawit hijau",
            "Cabe merah", "Cabe hijau", "Cabe rawit", "Bawang merah", "Bawang putih",
            "Bawang bombay"
        )
    ),
    IngredientCategory(
        name = "🍃 Dedaunan & Rempah",
        items = listOf(
            "Daun jeruk", "Daun salam", "Daun kemangi", "Daun bawang", "Daun pandan",
            "Daun seledri", "Daun kunyit", "Daun pisang", "Daun singkong", "Daun pepaya",
            "Biji pala", "Asam jawa", "Bunga lawang", "Kapulaga", "Kayu manis",
            "Ketumbar bubuk", "Merica bubuk", "Sereh"
        )
    ),
    IngredientCategory(
        name = "🍗 Daging & Protein",
        items = listOf(
            "Daging sapi", "Daging ayam", "Daging kambing", "Ayam fillet", "Dada ayam",
            "Paha ayam", "Telur ayam", "Telur bebek", "Telur puyuh", "Tahu putih",
            "Tahu kuning", "Tahu coklat", "Tempe gembus", "Ikan lele", "Ikan nila",
            "Ikan gurame", "Ikan teri", "Teri medan", "Udang rebon", "Kacang tanah",
            "Kacang panjang", "Kacang hijau", "Kacang merah"
        )
    ),
    IngredientCategory(
        name = "🧂 Bumbu & Saus",
        items = listOf(
            "Gula merah", "Gula pasir", "Gula aren", "Kaldu jamur", "Kaldu sapi",
            "Kaldu ayam", "Kecap manis", "Kecap asin", "Kecap inggris", "Saus tiram",
            "Saus sambal", "Saus tomat", "Minyak wijen", "Minyak ikan", "Santan kental",
            "Santan encer", "Kelapa parut", "Terasi bakar", "Garam", "Merica", "Jahe"
        )
    ),
    IngredientCategory(
        name = "🌾 Tepung & Karbo",
        items = listOf(
            "Tepung terigu", "Tepung beras", "Tepung tapioka", "Tepung kanji",
            "Tepung maizena", "Tepung panir", "Tepung roti", "Tepung bumbu",
            "Beras putih", "Beras merah", "Beras ketan", "Nasi"
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridgeScreen(
    viewModel: RecipeViewModel,
    onNavigateToHome: () -> Unit = {}
) {
    val expandedCategories = remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Fridge") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Select your ingredients",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Selected: ${viewModel.fridgeIngredients.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            items(INGREDIENT_CATEGORIES) { category ->
                val isExpanded = expandedCategories.value.contains(category.name)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedCategories.value = if (isExpanded) {
                                expandedCategories.value - category.name
                            } else {
                                expandedCategories.value + category.name
                            }
                        },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }

                        if (isExpanded) {
                            Divider(modifier = Modifier.padding(horizontal = 12.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                category.items.forEach { ingredient ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.toggleFridgeIngredient(ingredient)
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = viewModel.isIngredientInFridge(ingredient),
                                            onCheckedChange = { viewModel.toggleFridgeIngredient(ingredient) },
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = ingredient,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (viewModel.fridgeIngredients.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Your ingredients (${viewModel.fridgeIngredients.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            viewModel.fridgeIngredients.forEach { ingredient ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "✓ $ingredient",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Remove",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                viewModel.toggleFridgeIngredient(ingredient)
                                            },
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        FilledTonalButton(
                            onClick = { viewModel.clearFridgeIngredients() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear All")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onNavigateToHome() },
                            modifier = Modifier.fillMaxWidth(),
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
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No ingredients selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

