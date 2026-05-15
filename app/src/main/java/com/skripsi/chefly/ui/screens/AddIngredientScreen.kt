package com.skripsi.chefly.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
// Import warna dari theme kamu
import com.skripsi.chefly.ui.screens.onboarding.Terracotta
import com.skripsi.chefly.ui.theme.MutedSlate
import com.skripsi.chefly.ui.theme.WarmIvory
import com.skripsi.chefly.ui.viewmodel.AddIngredientViewModel
import com.skripsi.chefly.ui.viewmodel.IngredientUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIngredientScreen(
    onBackClick: () -> Unit,
    onNavigateToResult: (List<String>) -> Unit,
    viewModel: AddIngredientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedIngredients by viewModel.selectedIngredients.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = WarmIvory,
        topBar = {
            TopAppBar(
                title = { Text("Tambah Bahan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Terracotta)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.shadow(2.dp)
            )
        },
        bottomBar = {
            ActionBottomBar(
                count = selectedIngredients.size,
                onClick = { onNavigateToResult(selectedIngredients.toList()) }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is IngredientUiState.Loading -> LoadingView()
            is IngredientUiState.Error -> ErrorView(state.message)
            is IngredientUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Search Bar
                    item { SearchBarComponent(searchQuery) { searchQuery = it } }

                    // Chips Bahan Terpilih
                    if (selectedIngredients.isNotEmpty()) {
                        item {
                            SelectedChipsSection(selectedIngredients.toList()) { viewModel.toggleIngredient(it) }
                        }
                    }

                    // Dynamic Groups
                    state.groups.forEach { group ->
                        val filtered = group.ingredients.filter { it.contains(searchQuery, ignoreCase = true) }
                        if (filtered.isNotEmpty()) {
                            item { IngredientGroupHeader(group.categoryName, group.icon, group.color) }
                            items(filtered.chunked(2)) { rowItems ->
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { ingredient ->
                                        IngredientCard(
                                            name = ingredient,
                                            isSelected = selectedIngredients.contains(ingredient),
                                            onToggle = { viewModel.toggleIngredient(ingredient) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 4. SUB-COMPONENTS ---

@Composable
fun IngredientCard(name: String, isSelected: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Terracotta else Color.White,
        border = BorderStroke(1.dp, if (isSelected) Terracotta else Color.Transparent),
        shadowElevation = if (isSelected) 6.dp else 2.dp
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, color = if (isSelected) Color.White else Color.Black, fontSize = 14.sp)
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Default.Add,
                null,
                tint = if (isSelected) Color.White else Terracotta,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ActionBottomBar(count: Int, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shadowElevation = 12.dp, color = Color.White) {
        Button(
            onClick = onClick,
            enabled = count > 0,
            modifier = Modifier.padding(20.dp).fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Cari Resep Terbaik ($count)", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SearchBarComponent(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        placeholder = { Text("Cari bahan...") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Terracotta)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedChipsSection(ingredients: List<String>, onRemove: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Bahan Terpilih", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ingredients.forEach { name ->
                InputChip(
                    selected = true,
                    onClick = { onRemove(name) },
                    label = { Text(name) },
                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
fun IngredientGroupHeader(title: String, icon: ImageVector, color: Color) {
    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).background(color.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun LoadingView() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Terracotta) }

@Composable
fun ErrorView(msg: String) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(msg, color = Color.Red) }