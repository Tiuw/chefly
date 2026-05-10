package com.skripsi.chefly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skripsi.chefly.ui.viewmodel.FridgeViewModel
import com.skripsi.chefly.ui.viewmodel.SharedViewModel

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
    sharedViewModel: SharedViewModel,
    onNavigateToHome: () -> Unit = {}
) {
    val fridgeViewModel: FridgeViewModel = viewModel()
    val expandedCategories = remember { mutableStateOf(setOf<String>()) }

    val selectedIngredients by sharedViewModel.fridgeIngredients.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Bauhaus TopAppBar
        TopAppBar(
            title = {
                Text(
                    "MY FRIDGE",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black
                )
            },
            modifier = Modifier.border(
                width = 3.dp,
                color = MaterialTheme.colorScheme.primary
            ),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SELECT INGREDIENTS",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Selected: ${selectedIngredients.size} Ingredients",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.SemiBold
                )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            items(INGREDIENT_CATEGORIES) { category ->
                val isExpanded = expandedCategories.value.contains(category.name)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, MaterialTheme.colorScheme.primary)
                        .clickable {
                            expandedCategories.value = if (isExpanded) {
                                expandedCategories.value - category.name
                            } else {
                                expandedCategories.value + category.name
                            }
                        },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (isExpanded) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                category.items.forEach { ingredient ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                sharedViewModel.toggleFridgeIngredient(ingredient)
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Checkbox(
                                            checked = sharedViewModel.isIngredientInFridge(ingredient),
                                            onCheckedChange = { sharedViewModel.toggleFridgeIngredient(ingredient) },
                                            modifier = Modifier.size(20.dp),
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary,
                                                uncheckedColor = MaterialTheme.colorScheme.outline
                                            )
                                        )
                                        Text(
                                            text = ingredient,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedIngredients.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO INGREDIENTS SELECTED",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Selected Ingredients & Action Button
        if (selectedIngredients.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, MaterialTheme.colorScheme.primary)
                        .background(Color.White)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SELECTED (${selectedIngredients.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    selectedIngredients.forEach { ingredient ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "✓ $ingredient",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Remove",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        sharedViewModel.toggleFridgeIngredient(ingredient)
                                    },
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = { sharedViewModel.clearFridgeIngredients() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            "CLEAR ALL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = { onNavigateToHome() },
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                            .border(2.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Text(
                            "🔍 FIND RECIPES",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

