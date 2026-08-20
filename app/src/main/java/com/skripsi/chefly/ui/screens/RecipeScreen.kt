package com.skripsi.chefly.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.*
import com.skripsi.chefly.R
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.ui.theme.*
import com.skripsi.chefly.ui.viewmodel.CategoryData
import com.skripsi.chefly.ui.viewmodel.RecipeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    initialQuery: String = "",
    initialCategory: String = "",
    onRecipeClick: (String, Float) -> Unit,
    onScanClick: () -> Unit,
    viewModel: RecipeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val gridState = rememberLazyStaggeredGridState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(initialQuery, initialCategory) {
        if (initialQuery.isNotBlank() && initialQuery != uiState.searchQuery) {
            viewModel.onSearchQueryChanged(initialQuery)
        }
        if (initialCategory.isNotBlank() && !initialCategory.equals(uiState.selectedCategory, ignoreCase = true)) {
            viewModel.onCategorySelected(initialCategory)
        }
    }

    LaunchedEffect(uiState.selectedCategory, uiState.searchQuery) {
        if (uiState.recipes.isNotEmpty()) {
            coroutineScope.launch {
                gridState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        containerColor = CheflyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CheflySurfaceContainerLow,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.RestaurantMenu,
                                    contentDescription = null,
                                    tint = Terracotta,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Koleksi Resep",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = DeepCharcoal,
                                letterSpacing = (-0.4).sp
                            )
                            Text(
                                text = "Eksplorasi hidangan nusantara",
                                fontSize = 11.sp,
                                color = SecondaryText,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onScanClick,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(38.dp)
                            .background(PureSurface, CircleShape)
                            .border(1.dp, WhisperBorder, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.CenterFocusStrong,
                            contentDescription = "Pindai Kamera",
                            tint = Terracotta,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CheflyBackground)
            )
        }
    ) { innerPadding ->

        LazyVerticalStaggeredGrid(
            state = gridState,
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp
        ) {
            // 1. Search Dock Container
            item(span = StaggeredGridItemSpan.FullLine) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = PureSurface,
                    border = BorderStroke(1.dp, if (uiState.searchQuery.isNotEmpty()) Terracotta else WhisperBorder),
                    shadowElevation = 1.5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (uiState.searchQuery.isNotEmpty()) Terracotta else CheflySurfaceContainerLow,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (uiState.searchQuery.isNotEmpty()) PureSurface else Terracotta,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari judul resep atau bumbu...",
                                    color = SecondaryText.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepCharcoal
                                ),
                                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                cursorBrush = SolidColor(Terracotta),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        AnimatedVisibility(
                            visible = uiState.searchQuery.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = CheflySurfaceContainerLow,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { viewModel.onSearchQueryChanged("") }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
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

            // 2. Editorial Text-Based Category Bar
            item(span = StaggeredGridItemSpan.FullLine) {
                EditorialCategoryBar(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategoryClick = { viewModel.onCategorySelected(it) }
                )
            }

            // 3. Section Title & Counter
            item(span = StaggeredGridItemSpan.FullLine) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.searchQuery.isNotBlank()) "Hasil Penelusuran" else "Jelajahi Menu",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepCharcoal
                    )

                    if (uiState.recipes.isNotEmpty()) {
                        Surface(
                            color = CheflySurfaceContainerLow,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${uiState.recipes.size} resep",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Terracotta
                            )
                        }
                    }
                }
            }

            // 4. Loading Initial State
            if (uiState.isLoading && uiState.recipes.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Terracotta,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // 5. Empty State Lottie
            if (!uiState.isLoading && uiState.recipes.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    EmptySearchState()
                }
            }

            // 6. Staggered Recipe Cards
            itemsIndexed(
                items = uiState.recipes,
                key = { _, recipe -> recipe.id }
            ) { index, recipe ->
                val imageHeight = if (index % 3 == 0) 180.dp else 145.dp

                AnimatedRecipeCardWrapper(
                    index = index,
                    recipe = recipe,
                    imageHeight = imageHeight,
                    onClick = { onRecipeClick(recipe.id, 0f) },
                    onFavoriteClick = { viewModel.toggleFavorite(recipe) }
                )
            }

            // 7. Infinite Scroll Trigger
            if (uiState.recipes.isNotEmpty() && !uiState.isEndReached && !uiState.isLoadMore) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    LaunchedEffect(Unit) {
                        viewModel.loadNextPage()
                    }
                }
            }

            // 8. Indicator Load More
            if (uiState.isLoadMore) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Terracotta,
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            }

            // 9. Footer
            if (uiState.isEndReached && uiState.recipes.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = "Semua menu telah dimuat",
                        fontSize = 11.sp,
                        color = SecondaryText.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Editorial Category Bar dengan Garis Indikator Halus
 */
@Composable
fun EditorialCategoryBar(
    categories: List<CategoryData>,
    selectedCategory: String,
    onCategoryClick: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
    ) {
        items(categories) { category ->
            val isSelected = category.name.equals(selectedCategory, ignoreCase = true)

            val textColor by animateColorAsState(
                targetValue = if (isSelected) DeepCharcoal else SecondaryText.copy(alpha = 0.6f),
                animationSpec = tween(durationMillis = 200),
                label = "catTextColor"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCategoryClick(category.name) }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = category.name,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                    color = textColor,
                    letterSpacing = (-0.3).sp
                )

                Spacer(Modifier.height(5.dp))

                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(if (isSelected) 18.dp else 0.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Terracotta else Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun AnimatedRecipeCardWrapper(
    index: Int,
    recipe: Recipe,
    imageHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val animState = remember { Animatable(initialValue = 0f) }

    LaunchedEffect(recipe.id) {
        val delayTime = (index.coerceAtMost(6) * 40)
        kotlinx.coroutines.delay(delayTime.toLong())
        animState.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animState.value
                translationY = (1f - animState.value) * 50f
                scaleX = 0.94f + (animState.value * 0.06f)
                scaleY = 0.94f + (animState.value * 0.06f)
            }
    ) {
        StaggeredRecipeCard(
            recipe = recipe,
            imageHeight = imageHeight,
            onClick = onClick,
            onFavoriteClick = onFavoriteClick
        )
    }
}

@Composable
fun StaggeredRecipeCard(
    recipe: Recipe,
    imageHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, WhisperBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(recipe.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.22f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.52f)
                                )
                            )
                        )
                )

                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomStart),
                    color = DeepCharcoal.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = recipe.category.uppercase(),
                        color = PureSurface,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = PureSurface.copy(alpha = 0.92f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .clickable { onFavoriteClick() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (recipe.isFavorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Simpan",
                            tint = if (recipe.isFavorite) Terracotta else MutedSlate,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = recipe.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepCharcoal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${recipe.loves}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryText
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySearchState() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.empty_search))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(150.dp)
            )
        } else {
            Surface(
                color = CheflySurfaceContainerLow,
                shape = CircleShape,
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        Text(
            text = "Resep Tidak Ditemukan",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DeepCharcoal
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Coba gunakan kata kunci lain atau pilih kategori yang berbeda.",
            fontSize = 12.sp,
            color = SecondaryText,
            textAlign = TextAlign.Center
        )
    }
}