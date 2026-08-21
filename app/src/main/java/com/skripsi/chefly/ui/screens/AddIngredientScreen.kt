package com.skripsi.chefly.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skripsi.chefly.ui.theme.*
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
        containerColor = CheflyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kelola Bahan",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepCharcoal,
                            letterSpacing = (-0.4).sp
                        )
                        Text(
                            text = "Pilih bahan yang tersedia di dapur Anda",
                            fontSize = 11.sp,
                            color = SecondaryText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = DeepCharcoal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CheflyBackground)
            )
        },
        bottomBar = {
            FluidActionBottomBar(
                count = selectedIngredients.size,
                onClick = {
                    viewModel.saveToRepository()
                    onNavigateToResult(selectedIngredients.toList())
                }
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
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Search Bar
                    item(key = "search_bar") {
                        ModernSearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChange(it) }
                        )
                    }

                    // Selected Ingredients Tray
                    item(key = "selected_chips_section") {
                        AnimatedVisibility(
                            visible = selectedIngredients.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            SelectedSummaryDock(
                                ingredients = selectedIngredients.toList(),
                                onRemove = { viewModel.toggleIngredient(it) }
                            )
                        }
                    }

                    // Kategori Bahan dalam Card Container Terpadu
                    state.groups.forEach { group ->
                        item(key = "group_${group.categoryName}") {
                            CategoryIngredientSection(
                                title = group.categoryName,
                                icon = group.icon,
                                color = group.color,
                                ingredients = group.ingredients,
                                selectedIngredients = selectedIngredients,
                                onToggle = { viewModel.toggleIngredient(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Container Kategori dengan Fluid Flow Pills
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryIngredientSection(
    title: String,
    icon: ImageVector,
    color: Color,
    ingredients: List<String>,
    selectedIngredients: Set<String>,
    onToggle: (String) -> Unit
) {
    Surface(
        color = PureSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, WhisperBorder),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Bar Kategori
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = color.copy(alpha = 0.12f),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Text(
                        text = title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepCharcoal
                    )
                }

                Text(
                    text = "${ingredients.size} item",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = SecondaryText
                )
            }

            Spacer(Modifier.height(10.dp))

            // Fluid Chips: Lebar fleksibel mengikuti panjang kata
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ingredients.forEach { rawName ->
                    val cleanName = rawName.trim().lowercase().replaceFirstChar { it.uppercase() }
                    val isSelected = selectedIngredients.contains(rawName)

                    FluidSelectablePill(
                        name = cleanName,
                        isSelected = isSelected,
                        onToggle = { onToggle(rawName) }
                    )
                }
            }
        }
    }
}

/**
 * Fluid Selectable Pill (Bahan Pilihan)
 */
@Composable
fun FluidSelectablePill(
    name: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) Terracotta else Color(0xFFF6F3EE),
        animationSpec = tween(150),
        label = "pillBgAnim"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) PureSurface else DeepCharcoal,
        animationSpec = tween(150),
        label = "pillTextAnim"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isSelected) Terracotta else Color(0xFFE8E3DA),
        animationSpec = tween(150),
        label = "pillBorderAnim"
    )

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = animatedBg,
        border = BorderStroke(1.dp, animatedBorder),
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onToggle
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = name,
                fontSize = 12.5.sp,
                color = animatedTextColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PureSurface,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Tray Ringkasan Bahan yang Sudah Terpilih
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedSummaryDock(
    ingredients: List<String>,
    onRemove: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PureSurface,
        border = BorderStroke(1.dp, WhisperBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bahan Terpilih",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepCharcoal
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CheflySurfaceContainerLow
                ) {
                    Text(
                        text = "${ingredients.size} dipilih",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Terracotta,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ingredients.forEach { name ->
                    val cleanName = name.trim().lowercase().replaceFirstChar { it.uppercase() }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = CheflySurfaceContainerLow,
                        border = BorderStroke(1.dp, Terracotta.copy(alpha = 0.25f)),
                        modifier = Modifier.clickable { onRemove(name) }
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = cleanName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepCharcoal
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus",
                                tint = Terracotta,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Search Bar Ramping
 */
@Composable
fun ModernSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PureSurface,
        border = BorderStroke(1.dp, WhisperBorder),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = SecondaryText,
                modifier = Modifier.size(20.dp)
            )

            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Cari bahan (cth: tempe, tahu, ayam)...",
                        fontSize = 13.sp,
                        color = SecondaryText
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Hapus",
                        tint = SecondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bottom Bar Mengambang
 */
@Composable
fun FluidActionBottomBar(
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = PureSurface,
        border = BorderStroke(1.dp, WhisperBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onClick,
                enabled = count > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Terracotta,
                    disabledContainerColor = CheflySurfaceContainerLow
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (count > 0) PureSurface else SecondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (count > 0) "Cari Resep ($count Bahan)" else "Pilih Minimal 1 Bahan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = if (count > 0) PureSurface else SecondaryText
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = Terracotta,
            strokeWidth = 3.dp
        )
    }
}

@Composable
fun ErrorView(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = msg,
            color = ErrorCoral,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}