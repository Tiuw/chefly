package com.skripsi.chefly.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.ui.theme.MutedSlate
import com.skripsi.chefly.ui.theme.WarmIvory
import com.skripsi.chefly.ui.theme.Terracotta // Pastikan warna diimport benar
import com.skripsi.chefly.ui.viewmodel.SavedScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    onRecipeClick: (String) -> Unit,
    onAddClick: () -> Unit,
    viewModel: SavedScreenViewModel = hiltViewModel()
) {
    // Observasi data dari database secara reactive
    val savedRecipes by viewModel.savedRecipes.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    // Filter lokal untuk fitur search di dalam SavedScreen
    val filteredRecipes = remember(searchQuery, savedRecipes) {
        if (searchQuery.isBlank()) savedRecipes
        else savedRecipes.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = WarmIvory,
        topBar = {
            TopAppBar(
                title = {
                    Text("Resep Tersimpan", color = Color(0xFFE36C47), fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.shadow(1.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = Color(0xFFE36C47),
                contentColor = Color.White,
                shape = CircleShape,
                // Sesuaikan padding agar tidak menutupi area klik Bottom Nav
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Resep",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            // 1. Search Bar Section
            item {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari resep tersimpan...", color = MutedSlate) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = MutedSlate) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("${filteredRecipes.size} Resep", fontSize = 14.sp, color = MutedSlate)
                    }
                }
            }

            // 2. Recipe List (DIPERBAIKI: Hanya satu blok items dan kirim parameter onDeleteClick)
            if (filteredRecipes.isEmpty()) {
                item { EmptySavedState() }
            } else {
                items(filteredRecipes, key = { it.id }) { recipe ->
                    SavedRecipeCard(
                        recipe = recipe,
                        onClick = { onRecipeClick(recipe.id) },
                        onDeleteClick = { viewModel.removeFromFavorite(recipe.id) } // Sekarang parameter sudah dikirim
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

// Update fungsi SavedRecipeCard
@Composable
fun SavedRecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit // Tambahkan parameter ini
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.height(192.dp).fillMaxWidth()) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Tombol Hapus dari Favorit (Bookmark)
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                        .clickable(
                            onClick = onDeleteClick, // Trigger hapus
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark, // Tetap bookmark isi karena ini halaman saved
                        contentDescription = "Hapus",
                        tint = Color(0xFFE36C47),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = recipe.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = recipe.category,
                    fontSize = 14.sp,
                    color = MutedSlate
                )
            }
        }
    }
}

@Composable
fun EmptySavedState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .border(2.dp, Color(0xFFE0E0E0), CircleShape), // Gunakan WhisperBorder jika sudah ada di theme
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SoupKitchen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MutedSlate
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Dapur Anda masih sepi",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333) // Gunakan DeepCharcoal jika ada di theme
        )

        Text(
            "Simpan resep untuk melihatnya di sini atau scan kulkas Anda untuk ide baru.",
            textAlign = TextAlign.Center,
            color = MutedSlate,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )
    }
}