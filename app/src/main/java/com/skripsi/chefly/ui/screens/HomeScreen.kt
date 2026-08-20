package com.skripsi.chefly.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.RestaurantMenu,
                                    contentDescription = null,
                                    tint = Terracotta,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Chefly",
                                color = DeepCharcoal,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                letterSpacing = (-0.5).sp
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
                            .size(40.dp)
                            .background(PureSurface, CircleShape)
                            .border(1.dp, WhisperBorder, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Cari Resep",
                            tint = DeepCharcoal,
                            modifier = Modifier.size(20.dp)
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Hero Scanner Banner
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ModernScannerHero(onScanClick = onScanClick)
                }
            }

            // 2. Quick Ingredient Section (Full Offline via Local Drawable)
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
                            .height(220.dp),
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepCharcoal
            )
            Text(
                text = "Cari Resep",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Terracotta
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(quickItems) { item ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PureSurface,
                    border = BorderStroke(1.dp, WhisperBorder),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.clickable { onItemClick(item.name) }
                ) {
                    Row(
                        modifier = Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp),
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
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item.name,
                            fontSize = 13.sp,
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
fun KitchenTipCard() {
    Surface(
        color = AlertAmber.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AlertAmber.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = AlertAmber,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = PureSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Trik Dapur Hari Ini",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CheflyOnPrimaryFixedVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Simpan cabai bersama 1 siung bawang putih kupas di wadah tertutup agar tetap segar berminggu-minggu.",
                    fontSize = 11.sp,
                    color = CheflyOnSurfaceVariant,
                    lineHeight = 15.sp
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
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = DeepCharcoal
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = SecondaryText
            )
        }
        TextButton(onClick = onSeeAll) {
            Text(
                text = "Lihat Semua",
                color = Terracotta,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Terracotta,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ModernScannerHero(onScanClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Terracotta.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-30).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Terracotta.copy(alpha = 0.35f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Surface(
                    color = Terracotta.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Terracotta.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Terracotta,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "EDGE AI CAMERA",
                            color = Terracotta,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Bingung mau masak\napa hari ini?",
                    color = PureSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 26.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Arahkan kamera ke bahan makananmu, AI akan menemukan resep lezat yang cocok dalam hitungan detik.",
                    color = PureSurface.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onScanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = PureSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Pindai Bahan Makanan",
                        color = PureSurface,
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
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, WhisperBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
            ) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    color = PureSurface.copy(alpha = 0.92f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clickable { onFavoriteClick() }
                ) {
                    Icon(
                        imageVector = if (recipe.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Simpan Favorit",
                        tint = if (recipe.isFavorite) Terracotta else MutedSlate,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = DeepCharcoal
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = CheflySurfaceContainerLow,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Rekomendasi",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = Terracotta,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Terracotta,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "${recipe.loves}",
                            fontSize = 11.sp,
                            color = SecondaryText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}