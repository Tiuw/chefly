package com.skripsi.chefly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skripsi.chefly.ui.screens.onboarding.Terracotta
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
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = WarmIvory,
        topBar = {
            TopAppBar(
                title = { Text("Tambah Bahan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Terracotta)
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Search Bar Component menyambung ke ViewModel
                    item(key = "search_bar") {
                        SearchBarComponent(
                            query = searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChange(it) }
                        )
                    }

                    // Chips Bahan Terpilih (Kamera YOLO26 / Centang Manual)
                    if (selectedIngredients.isNotEmpty()) {
                        item(key = "selected_chips_section") {
                            SelectedChipsSection(
                                ingredients = selectedIngredients.toList(),
                                onRemove = { viewModel.toggleIngredient(it) }
                            )
                        }
                    }

                    // Kelompok Bahan Terkategori (Sudah difilter oleh ViewModel)
                    state.groups.forEach { group ->
                        item(key = "header_${group.categoryName}") {
                            IngredientGroupHeader(
                                title = group.categoryName,
                                icon = group.icon,
                                color = group.color
                            )
                        }

                        // Menggunakan chunked(2) untuk grid 2 kolom di dalam LazyColumn
                        items(
                            items = group.ingredients.chunked(2),
                            key = { row -> "${group.categoryName}_${row.joinToString()}" }
                        ) { rowItems ->
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
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
                                if (rowItems.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun IngredientCard(
    name: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Terracotta else Color.White,
        border = BorderStroke(1.dp, if (isSelected) Terracotta else Color.Transparent),
        shadowElevation = if (isSelected) 6.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                color = if (isSelected) Color.White else Color.Black,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Add,
                contentDescription = null,
                tint = if (isSelected) Color.White else Terracotta,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ActionBottomBar(
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 12.dp,
        color = Color.White
    ) {
        Button(
            onClick = onClick,
            enabled = count > 0,
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Terracotta,
                disabledContainerColor = Color.LightGray
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (count > 0) "Cari Resep Terbaik ($count)" else "Pilih Bahan Terlebih Dahulu",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun SearchBarComponent(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        placeholder = { Text("Cari bahan masakan...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Terracotta,
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedChipsSection(
    ingredients: List<String>,
    onRemove: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Bahan Terpilih", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ingredients.forEach { name ->
                InputChip(
                    selected = true,
                    onClick = { onRemove(name) },
                    label = { Text(name) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
                    },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = Terracotta.copy(alpha = 0.15f),
                        selectedLabelColor = Terracotta,
                        selectedTrailingIconColor = Terracotta
                    )
                )
            }
        }
    }
}

@Composable
fun IngredientGroupHeader(
    title: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Terracotta)
    }
}

@Composable
fun ErrorView(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(msg, color = Color.Red, fontSize = 14.sp)
    }
}