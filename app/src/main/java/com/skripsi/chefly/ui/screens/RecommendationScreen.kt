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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.skripsi.chefly.ui.viewmodel.RecommendationViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationScreen(
    ingredients: String,
    onBackClick: () -> Unit,
    onAddMoreClick: () -> Unit,
    onRecipeClick: (String, Float) -> Unit,
    viewModel: RecommendationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(ingredients) {
        if (ingredients.isNotBlank()) {
            viewModel.getRecommendations(ingredients)
        }
    }

    if (uiState.isLoading) {
        CleanIndeterminateLoadingView(query = uiState.ingredientsQuery)
    } else {
        Scaffold(
            containerColor = CheflyBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Rekomendasi Menu",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = DeepCharcoal,
                                letterSpacing = (-0.4).sp
                            )
                            Text(
                                text = "Berdasarkan bahan yang Anda miliki",
                                fontSize = 11.sp,
                                color = SecondaryText,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = DeepCharcoal
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CheflyBackground)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Compact Pantry Header
                item {
                    CompactPantryDock(
                        ingredients = uiState.ingredients,
                        onAddMore = onAddMoreClick
                    )
                }

                if (uiState.recipes.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Resep Paling Cocok",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepCharcoal
                            )
                            Text(
                                text = "${uiState.recipes.size} menu ditemukan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SecondaryText
                            )
                        }
                    }
                }

                if (uiState.recipes.isEmpty()) {
                    item {
                        EmptyRecommendationStateView(onModifyIngredients = onAddMoreClick)
                    }
                } else {
                    itemsIndexed(
                        items = uiState.recipes,
                        key = { _, recipe -> recipe.id }
                    ) { index, recipe ->
                        AnimatedRecommendationCardWrapper(
                            index = index,
                            recipe = recipe,
                            currentQuery = uiState.ingredientsQuery,
                            onClick = { onRecipeClick(recipe.id, recipe.similarity) },
                            onFavoriteClick = { viewModel.toggleFavorite(recipe) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Header Pantry Kompak & Bersih Tanpa Dot
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompactPantryDock(
    ingredients: List<String>,
    onAddMore: () -> Unit
) {
    Surface(
        color = PureSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, WhisperBorder),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CheflySurfaceContainerLow,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Kitchen,
                            contentDescription = null,
                            tint = Terracotta,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Text(
                    text = "Bahan di Dapur",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepCharcoal
                )

                Text(
                    text = "• ${ingredients.size} bahan",
                    fontSize = 11.5.sp,
                    color = SecondaryText,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ingredients.forEach { rawName ->
                    val cleanName = rawName.trim().lowercase().replaceFirstChar { it.uppercase() }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFF6F3EE),
                        border = BorderStroke(1.dp, Color(0xFFE8E3DA))
                    ) {
                        Text(
                            text = cleanName,
                            fontSize = 12.5.sp,
                            color = DeepCharcoal,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Action Pill Tambah Bahan
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Terracotta.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Terracotta.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable { onAddMore() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Terracotta,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Tambah",
                            fontSize = 12.sp,
                            color = Terracotta,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kartu Rekomendasi Menu dengan Scrim Kontras & Micro-Badge Tonal
 */
@Composable
fun ModernRecommendationCard(
    recipe: Recipe,
    currentQuery: String = "",
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val ingredientAnalysis = remember(recipe, currentQuery) {
        val allRecipeIngredients = recipe.ingredientList
        if (currentQuery.isNotBlank()) {
            val userTokens = currentQuery.split(Regex("[,\\s_]+"))
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }

            val availableCount = allRecipeIngredients.count { ingredient ->
                val cleaned = ingredient.lowercase().replace(" ", "").replace("_", "")
                userTokens.any { token -> cleaned.contains(token) }
            }
            Pair(availableCount, (allRecipeIngredients.size - availableCount).coerceAtLeast(0))
        } else {
            Pair(0, allRecipeIngredients.size)
        }
    }
    val (availableCount, missingCount) = ingredientAnalysis
    val matchPercentage = (recipe.similarity * 100).toInt()

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
                    .height(180.dp)
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

                // Balanced Top & Bottom Scrim Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                // AI Match Score Tag
                if (recipe.similarity > 0f) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        color = Terracotta,
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 1.dp
                    ) {
                        Text(
                            text = "$matchPercentage% Match",
                            color = PureSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Bookmark Floating Action
                Surface(
                    shape = CircleShape,
                    color = PureSurface.copy(alpha = 0.92f),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(34.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onFavoriteClick
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (recipe.isFavorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Simpan",
                            tint = if (recipe.isFavorite) Terracotta else DeepCharcoal,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Glassmorphic Category Tag
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    color = DeepCharcoal.copy(alpha = 0.7f),
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

            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recipe.name,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepCharcoal,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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

                // Micro-Badges Status Bahan
                if (currentQuery.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SoftSage.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "✓ $availableCount Bahan Ada",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftSage,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (missingCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CheflySurfaceContainerLow
                            ) {
                                Text(
                                    text = "+$missingCount Perlu Tambahan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Terracotta,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SoftSage.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Lengkap",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftSage,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Indeterminate Loading View Murni
 */
@Composable
fun CleanIndeterminateLoadingView(query: String) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.ai_loading))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 36.dp)
        ) {
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                if (composition != null) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator(
                        color = Terracotta,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Sedang Mencari Resep",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = DeepCharcoal,
                letterSpacing = (-0.3).sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Mencocokkan kombinasi bahan terbaik untuk Anda...",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = SecondaryText,
                textAlign = TextAlign.Center
            )

            if (query.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CheflySurfaceContainerLow
                ) {
                    Text(
                        text = "Bahan: $query",
                        fontSize = 11.sp,
                        color = Terracotta,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
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
fun AnimatedRecommendationCardWrapper(
    index: Int,
    recipe: Recipe,
    currentQuery: String,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
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
                translationY = (1f - animState.value) * 40f
                scaleX = 0.95f + (animState.value * 0.05f)
                scaleY = 0.95f + (animState.value * 0.05f)
            }
    ) {
        ModernRecommendationCard(
            recipe = recipe,
            currentQuery = currentQuery,
            onClick = onClick,
            onFavoriteClick = onFavoriteClick
        )
    }
}

/**
 * Empty State View
 */
@Composable
fun EmptyRecommendationStateView(onModifyIngredients: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.empty_search))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 40.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(160.dp)
            )
        } else {
            Surface(
                color = CheflySurfaceContainerLow,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = "Tidak Menemukan Resep Cocok",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DeepCharcoal
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Kombinasi bahan yang dipilih belum cocok dengan resep lokal. Coba sesuaikan bahan yang Anda miliki.",
            fontSize = 12.sp,
            color = SecondaryText,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onModifyIngredients,
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = PureSurface
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Ubah Bahan",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PureSurface
            )
        }
    }
}