package com.skripsi.chefly.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.*
import com.skripsi.chefly.R
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.ui.theme.*
import com.skripsi.chefly.ui.viewmodel.SavedScreenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    onRecipeClick: (String) -> Unit,
    onAddClick: () -> Unit,
    viewModel: SavedScreenViewModel = hiltViewModel()
) {
    val savedRecipes by viewModel.savedRecipes.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val filteredRecipes = remember(searchQuery, savedRecipes) {
        if (searchQuery.isBlank()) savedRecipes
        else savedRecipes.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = CheflyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Resep Tersimpan",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepCharcoal,
                            letterSpacing = (-0.4).sp
                        )
                        Text(
                            text = "Koleksi menu favorit untuk dapur Anda",
                            fontSize = 11.sp,
                            color = SecondaryText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CheflyBackground)
            )
        },
        floatingActionButton = {
            if (filteredRecipes.isNotEmpty() || searchQuery.isNotBlank()) {
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = Terracotta,
                    contentColor = PureSurface,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Bahan & Resep",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Bar & Counter Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "Cari menu tersimpan...",
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

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
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

                    if (filteredRecipes.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daftar Koleksi",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepCharcoal
                            )
                            Text(
                                text = "${filteredRecipes.size} resep",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SecondaryText
                            )
                        }
                    }
                }
            }

            // Context-Aware Empty State
            if (filteredRecipes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(bottom = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (searchQuery.isNotBlank()) {
                            EmptySearchResultState(
                                query = searchQuery,
                                onClearSearch = { searchQuery = "" }
                            )
                        } else {
                            InitialEmptySavedState(
                                onExploreClick = onAddClick
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = filteredRecipes,
                    key = { _, recipe -> recipe.id }
                ) { index, recipe ->
                    AnimatedSavedRecipeCard(
                        index = index,
                        recipe = recipe,
                        onClick = { onRecipeClick(recipe.id) },
                        onDeleteClick = { viewModel.removeFromFavorite(recipe.id) }
                    )
                }
            }
        }
    }
}

/**
 * Kartu Koleksi Resep Tersimpan Modern
 */
@Composable
fun ModernSavedRecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val bookmarkScale = remember { Animatable(1f) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = PureSurface,
        border = BorderStroke(1.dp, WhisperBorder),
        shadowElevation = 0.5.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
            ) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Scrim Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                // Bookmark Active Button
                Surface(
                    shape = CircleShape,
                    color = PureSurface.copy(alpha = 0.92f),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                        .size(34.dp)
                        .graphicsLayer {
                            scaleX = bookmarkScale.value
                            scaleY = bookmarkScale.value
                        }
                        .clickable(
                            onClick = {
                                coroutineScope.launch {
                                    bookmarkScale.animateTo(0.75f, tween(70))
                                    bookmarkScale.animateTo(
                                        1.2f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                    bookmarkScale.animateTo(1f)
                                }
                                onDeleteClick()
                            },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Hapus dari Tersimpan",
                            tint = Terracotta,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Category Tag
                if (recipe.category.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        color = DeepCharcoal.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = recipe.category.uppercase(),
                            color = PureSurface,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Recipe Name & Info Body
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recipe.name,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepCharcoal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(start = 8.dp)
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

/**
 * Animated Entrance Wrapper
 */
@Composable
fun AnimatedSavedRecipeCard(
    index: Int,
    recipe: Recipe,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val animState = remember { Animatable(initialValue = 0f) }

    LaunchedEffect(recipe.id) {
        val delayTime = (index.coerceAtMost(5) * 45)
        delay(delayTime.toLong())
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
                translationY = (1f - animState.value) * 35f
                scaleX = 0.95f + (animState.value * 0.05f)
                scaleY = 0.95f + (animState.value * 0.05f)
            }
    ) {
        ModernSavedRecipeCard(
            recipe = recipe,
            onClick = onClick,
            onDeleteClick = onDeleteClick
        )
    }
}

/**
 * Empty State: Saat Pencarian Tidak Ditemukan
 */
@Composable
fun EmptySearchResultState(
    query: String,
    onClearSearch: () -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.empty_search))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(150.dp)
            )
        }

        Text(
            text = "Menu Tidak Ditemukan",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DeepCharcoal
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Tidak ada resep tersimpan yang cocok dengan kata kunci \"$query\".",
            textAlign = TextAlign.Center,
            color = SecondaryText,
            fontSize = 12.5.sp,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = onClearSearch,
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = PureSurface
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Hapus Pencarian",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PureSurface
            )
        }
    }
}

/**
 * Empty State: Saat Belum Ada Resep Tersimpan
 */
@Composable
fun InitialEmptySavedState(onExploreClick: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.empty_search))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
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
                shape = CircleShape,
                color = CheflySurfaceContainerLow,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = Terracotta
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            text = "Belum Ada Resep Tersimpan",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DeepCharcoal
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Simpan resep favorit saat menjelajah agar mudah dimasak kembali sewaktu-waktu.",
            textAlign = TextAlign.Center,
            color = SecondaryText,
            fontSize = 12.5.sp,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = onExploreClick,
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Kitchen,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = PureSurface
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Pilih Bahan Kulkas",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PureSurface
            )
        }
    }
}