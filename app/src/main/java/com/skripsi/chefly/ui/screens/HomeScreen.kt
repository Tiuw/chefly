package com.skripsi.chefly.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.skripsi.chefly.R
import com.skripsi.chefly.ui.theme.*
import com.skripsi.chefly.ui.viewmodel.HomeViewModel
import com.skripsi.chefly.ui.viewmodel.RecipeUiModel
import kotlinx.coroutines.launch

data class QuickIngredientItem(
    val name: String,
    @DrawableRes val imageRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onScanClick: () -> Unit,
    onRecipeClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    onCategoryClick: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recipes by viewModel.suggestedRecipes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

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
                                text = "Chefly",
                                color = DeepCharcoal,
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp,
                                letterSpacing = (-0.4).sp
                            )
                            Text(
                                text = "Smart Cooking Assistant",
                                color = SecondaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSeeAllClick,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(38.dp)
                            .background(PureSurface, CircleShape)
                            .border(1.dp, WhisperBorder, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Cari Resep",
                            tint = DeepCharcoal,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CheflyBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // 1. Hero Scanner Banner
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ModernScannerHero(onScanClick = onScanClick)
                }
            }

            // 2. Quick Ingredient Section
            item {
                QuickIngredientSection(onItemClick = onCategoryClick)
            }

            // 3. Section Header Inspirasi
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader(
                        title = "Inspirasi Menu Hari Ini",
                        subtitle = "Koleksi resep terpopuler siap masak",
                        onSeeAll = onSeeAllClick
                    )
                }
            }

            // 4. Horizontal Recipes Row
            item {
                if (isLoading && recipes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Terracotta, strokeWidth = 3.dp)
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(recipes, key = { it.id }) { recipe ->
                            HorizontalRecipeCard(
                                recipe = recipe,
                                onClick = { onRecipeClick(recipe.id) },
                                onFavoriteClick = { viewModel.toggleFavorite(recipe.id) }
                            )
                        }
                    }
                }
            }

            // 5. Kitchen Tip Banner
            item {
                KitchenTipCard()
            }
        }
    }
}

@Composable
fun QuickIngredientSection(onItemClick: (String) -> Unit) {
    val context = LocalContext.current

    val quickItems = remember {
        listOf(
            QuickIngredientItem("Ayam", R.drawable.ic_ingredient_ayam),
            QuickIngredientItem("Sapi", R.drawable.ic_ingredient_sapi),
            QuickIngredientItem("Telur", R.drawable.ic_ingredient_telur),
            QuickIngredientItem("Tahu", R.drawable.ic_ingredient_tahu),
            QuickIngredientItem("Tempe", R.drawable.ic_ingredient_tempe),
            QuickIngredientItem("Ikan", R.drawable.ic_ingredient_ikan),
            QuickIngredientItem("Udang", R.drawable.ic_ingredient_udang)
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Punya Bahan Apa?",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = DeepCharcoal
            )
            Text(
                text = "Lihat Semua",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Terracotta,
                modifier = Modifier.clickable { onItemClick("") }
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickItems) { item ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = PureSurface,
                    border = BorderStroke(1.dp, WhisperBorder),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.clickable { onItemClick(item.name) }
                ) {
                    Row(
                        modifier = Modifier.padding(start = 5.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.size(28.dp),
                            color = CheflySurfaceContainerLow
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.imageRes)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Text(
                            text = item.name,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepCharcoal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernScannerHero(onScanClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, WhisperBorder),
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFB84524),
                            Color(0xFF8B2B11)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Surface(
                    color = PureSurface.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PureSurface,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "EDGE AI CAMERA",
                            color = PureSurface,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Bingung mau masak\napa hari ini?",
                    color = PureSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 25.sp,
                    letterSpacing = (-0.3).sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Arahkan kamera ke isi kulkas, AI akan merekomendasikan menu lokal paling cocok seketika.",
                    color = PureSurface.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = onScanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PureSurface),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Icon(
                        Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Pindai Bahan Makanan",
                        color = Terracotta,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HorizontalRecipeCard(
    recipe: RecipeUiModel,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val bookmarkScale = remember { Animatable(1f) }

    Surface(
        modifier = Modifier
            .width(185.dp)
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
                    .height(125.dp)
            ) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                            )
                        )
                )

                // Bookmark Floating Action
                Surface(
                    color = PureSurface.copy(alpha = 0.92f),
                    shape = CircleShape,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .graphicsLayer {
                            scaleX = bookmarkScale.value
                            scaleY = bookmarkScale.value
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
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
                                onFavoriteClick()
                            }
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (recipe.isFavorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Simpan Favorit",
                            tint = if (recipe.isFavorite) Terracotta else DeepCharcoal,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = recipe.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = DeepCharcoal
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = CheflySurfaceContainerLow,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Populer",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.5.sp,
                            color = Terracotta,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Terracotta,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "${recipe.loves}",
                            fontSize = 11.sp,
                            color = SecondaryText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KitchenTipCard() {
    Surface(
        color = PureSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, WhisperBorder),
        shadowElevation = 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CheflySurfaceContainerLow,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Trik Dapur Hari Ini",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepCharcoal
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Simpan cabai bersama 1 siung bawang putih kupas di wadah tertutup agar tetap segar berminggu-minggu.",
                    fontSize = 11.5.sp,
                    color = SecondaryText,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = DeepCharcoal,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = SecondaryText,
                fontWeight = FontWeight.Medium
            )
        }
        TextButton(
            onClick = onSeeAll,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Text(
                text = "Lihat Semua",
                color = Terracotta,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Terracotta,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}