package com.skripsi.chefly.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.ui.theme.*
import com.skripsi.chefly.ui.viewmodel.RecipeDetailViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RecipeDetailScreen(
    navController: NavHostController,
    recipeId: String,
    currentQuery: String = "",
    passedSimilarity: Float = 0f,
    viewModel: RecipeDetailViewModel
) {
    val recipe by viewModel.recipe.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.loadError.collectAsState()

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var startAnimation by remember { mutableStateOf(false) }

    // Bookmark Micro-Interaction Scale State
    val bookmarkScale = remember { Animatable(1f) }

    LaunchedEffect(recipeId) {
        viewModel.loadRecipe(recipeId)
        delay(80)
        startAnimation = true
    }

    Scaffold(
        containerColor = CheflyBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Terracotta,
                    strokeWidth = 3.dp
                )
            } else if (error != null) {
                Text(
                    text = error ?: "Terjadi kesalahan",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    color = ErrorCoral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            } else {
                recipe?.let { currentRecipe ->

                    val ingredientAnalysis = remember(currentRecipe, currentQuery) {
                        val allRecipeIngredients = currentRecipe.ingredientList

                        if (currentQuery.isNotBlank()) {
                            val userTokens = currentQuery.split(Regex("[,\\s]+"))
                                .map { it.trim().lowercase().replace("_", "") }
                                .filter { it.isNotEmpty() }

                            val availableList = mutableListOf<String>()
                            val missingList = mutableListOf<String>()

                            for (ingredient in allRecipeIngredients) {
                                val cleanedIngredient = ingredient.lowercase().replace(" ", "")
                                val isMatched = userTokens.any { token -> cleanedIngredient.contains(token) }

                                if (isMatched) availableList.add(ingredient) else missingList.add(ingredient)
                            }
                            Pair(availableList, missingList)
                        } else {
                            Pair(emptyList<String>(), allRecipeIngredients)
                        }
                    }
                    val (availableIngredients, missingIngredients) = ingredientAnalysis

                    val actualSimilarity = if (currentRecipe.similarity == 0f) passedSimilarity else currentRecipe.similarity
                    val displayScore = (actualSimilarity * 100).toInt()

                    // Scroll-based Header Alpha & Translation for Sticky Bar
                    val topBarThreshold = 450f
                    val headerAlpha = (scrollState.value / topBarThreshold).coerceIn(0f, 1f)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        // --- 1. Parallax Hero Image Section ---
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .graphicsLayer {
                                    // Parallax translation effect
                                    translationY = scrollState.value * 0.45f
                                    alpha = (1f - (scrollState.value / 600f)).coerceIn(0f, 1f)
                                }
                                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                        ) {
                            AsyncImage(
                                model = currentRecipe.imageUrl,
                                contentDescription = currentRecipe.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Scrim Gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.5f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.7f)
                                            )
                                        )
                                    )
                            )

                            // Hero Bottom Badges
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(18.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DeepCharcoal.copy(alpha = 0.85f)
                                ) {
                                    Text(
                                        text = currentRecipe.category.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.6.sp,
                                        color = PureSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DeepCharcoal.copy(alpha = 0.65f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = Terracotta,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "${currentRecipe.loves}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureSurface
                                        )
                                    }
                                }
                            }
                        }

                        // --- 2. Content Body ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp)
                        ) {
                            // Title with Staggered Entrance
                            AnimatedDetailSection(isVisible = startAnimation, delayMillis = 50) {
                                Column {
                                    Spacer(Modifier.height(18.dp))
                                    Text(
                                        text = currentRecipe.name,
                                        color = DeepCharcoal,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        lineHeight = 28.sp,
                                        letterSpacing = (-0.4).sp
                                    )
                                }
                            }

                            // AI Match Score Micro-Card
                            if (actualSimilarity > 0f) {
                                AnimatedDetailSection(isVisible = startAnimation, delayMillis = 140) {
                                    Column {
                                        Spacer(Modifier.height(14.dp))
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            color = PureSurface,
                                            border = BorderStroke(1.dp, WhisperBorder),
                                            shadowElevation = 1.dp
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Surface(
                                                            modifier = Modifier.size(38.dp),
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = CheflySurfaceContainerLow
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = Icons.Default.AutoAwesome,
                                                                    contentDescription = null,
                                                                    tint = Terracotta,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                        Column {
                                                            Text(
                                                                text = "Kecocokan Bahan",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = SecondaryText
                                                            )
                                                            Text(
                                                                text = if (displayScore >= 75) "Sangat Cocok Dimasak" else "Bahan Cukup Terpenuhi",
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = DeepCharcoal
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        text = "$displayScore%",
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Terracotta
                                                    )
                                                }

                                                var progressTriggered by remember { mutableStateOf(false) }
                                                LaunchedEffect(startAnimation) {
                                                    if (startAnimation) {
                                                        delay(200)
                                                        progressTriggered = true
                                                    }
                                                }

                                                val animatedProgress by animateFloatAsState(
                                                    targetValue = if (progressTriggered) actualSimilarity else 0f,
                                                    animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                                                    label = "progressAnim"
                                                )

                                                Spacer(Modifier.height(10.dp))
                                                LinearProgressIndicator(
                                                    progress = { animatedProgress },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(CircleShape),
                                                    color = Terracotta,
                                                    trackColor = CheflySurfaceContainerLow
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // --- 3. Unified Ingredients Container ---
                            AnimatedDetailSection(isVisible = startAnimation, delayMillis = 220) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Bahan yang Dibutuhkan",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepCharcoal
                                        )
                                        Text(
                                            text = "${currentRecipe.ingredientList.size} item",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SecondaryText
                                        )
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = PureSurface,
                                        border = BorderStroke(1.dp, WhisperBorder),
                                        shadowElevation = 0.5.dp
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                            val targetIngredients = if (actualSimilarity > 0f) {
                                                availableIngredients.map { it to true } + missingIngredients.map { it to false }
                                            } else {
                                                currentRecipe.ingredientList.map { it to false }
                                            }

                                            targetIngredients.forEachIndexed { index, (name, isAvailable) ->
                                                CleanIngredientRow(
                                                    name = name,
                                                    isAvailable = isAvailable,
                                                    showDivider = index != targetIngredients.lastIndex
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(28.dp))

                            // --- 4. Cara Memasak Section ---
                            AnimatedDetailSection(isVisible = startAnimation, delayMillis = 300) {
                                Column {
                                    Text(
                                        text = "Cara Memasak",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepCharcoal
                                    )

                                    Spacer(Modifier.height(14.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        currentRecipe.stepList.forEachIndexed { index, stepText ->
                                            CleanInstructionRow(
                                                stepNumber = index + 1,
                                                instruction = stepText
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(44.dp))
                        }
                    }

                    // --- Dynamic Sticky Floating TopBar ---
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = PureSurface.copy(alpha = (headerAlpha * 0.95f)),
                        shadowElevation = if (headerAlpha > 0.8f) 3.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PureSurface.copy(alpha = if (headerAlpha > 0.5f) 0.8f else 0.92f),
                                border = BorderStroke(1.dp, WhisperBorder),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable { navController.navigateUp() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Kembali",
                                        tint = DeepCharcoal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Dynamic Title on Sticky Scroll
                            AnimatedVisibility(
                                visible = headerAlpha > 0.7f,
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = currentRecipe.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepCharcoal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Bookmark Button with Spring Bounce Micro-Interaction
                            Surface(
                                shape = CircleShape,
                                color = PureSurface.copy(alpha = if (headerAlpha > 0.5f) 0.8f else 0.92f),
                                border = BorderStroke(1.dp, WhisperBorder),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .size(38.dp)
                                    .graphicsLayer {
                                        scaleX = bookmarkScale.value
                                        scaleY = bookmarkScale.value
                                    }
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            coroutineScope.launch {
                                                bookmarkScale.animateTo(
                                                    targetValue = 0.75f,
                                                    animationSpec = tween(70)
                                                )
                                                bookmarkScale.animateTo(
                                                    targetValue = 1.2f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow
                                                    )
                                                )
                                                bookmarkScale.animateTo(1f)
                                            }
                                            viewModel.toggleFavorite()
                                        }
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (currentRecipe.isFavorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "Simpan",
                                        tint = if (currentRecipe.isFavorite) Terracotta else DeepCharcoal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Baris Bahan Bersih dengan Divider Halus
 */
@Composable
fun CleanIngredientRow(
    name: String,
    isAvailable: Boolean,
    showDivider: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isAvailable) SoftSage else Terracotta)
                )
                Text(
                    text = name,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = DeepCharcoal
                )
            }

            if (isAvailable) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SoftSage.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Tersedia",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftSage,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = WhisperBorder.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Baris Instruksi Memasak Tipografis
 */
@Composable
fun CleanInstructionRow(
    stepNumber: Int,
    instruction: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (stepNumber == 1) Terracotta else CheflySurfaceContainerLow,
            modifier = Modifier.size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$stepNumber",
                    color = if (stepNumber == 1) PureSurface else DeepCharcoal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = instruction,
            fontSize = 13.5.sp,
            color = DeepCharcoal,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Staggered Entrance Animation Container
 */
@Composable
fun AnimatedDetailSection(
    isVisible: Boolean,
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 450, delayMillis = delayMillis, easing = FastOutSlowInEasing),
        label = "alphaDetailAnim"
    )
    val translationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
            visibilityThreshold = 0.5f
        ),
        label = "transYDetailAnim"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        }
    ) {
        content()
    }
}