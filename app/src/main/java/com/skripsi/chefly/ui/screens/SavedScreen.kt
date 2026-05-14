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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.skripsi.chefly.ui.theme.MutedSlate
import com.skripsi.chefly.ui.theme.WarmIvory

val OnSurface = Color(0xFF241916)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen() {
    Scaffold(
        topBar = { SavedTopBar() },
        bottomBar = { SavedBottomNavigation() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Aksi Tambah */ },
                containerColor = Terracotta,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(32.dp))
            }
        },
        containerColor = WarmIvory
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Search & Filter
            SearchAndSortSection()

            Spacer(modifier = Modifier.height(24.dp))

            // Daftar Resep menggunakan LazyColumn agar efisien
            val savedRecipes = listOf(
                RecipeItem("Mangkuk Quinoa Mediterania", "https://lh3.googleusercontent.com/aida-public/AB6AXuB3FfB0AwJX5UloOhYZYkKPB4y4Kx00KgesajHKb1vuSyQd_cgVLun4qty787Nopc5NTET8ApPFx__EOHHz6nP32dlecYqwqaTa_71-3C8zeX1RIPQEmjPW2PaBGQY7uytOH9gavNS-vLN_ayUPTzwoHmZ1_z0B2oLydYHPrLrKVZmVed0KsWar3-jTl4e4YR0zNP7UzkGGb0yu7Xv3EFqmZaZY5YYM2L7soD08bPPsL4A8bIYhcRIdfTP9KS6-vK8lfD-Qmp2jCjM"),
                RecipeItem("Pasta Arrabbiata Pedas", "https://lh3.googleusercontent.com/aida-public/AB6AXuCr3w6P18NwsRU3KRIkjjI1Q_4uo9gxMcnAf9haONfQRowzR-tp_xMSjDjFHJhS-4FkGqTKJCRLhZGDomihBaZFxcezJWpa5yQWrQB2TPyxcjgzodWbl67l-rO5syY7hKVabQmNzJuiZC-9sGrmdMCf7q-911rucUix4XPhSfA0-q5MNhZYHy3-QQEZ8sHEP9E0qAvluSiB7qzElCIvnkGRSEf6xq-tqN7qm5RrDSrXO1hB0g0-baFAnYB4TS-eWkGE2UPisAyPFE4"),
                RecipeItem("Hasil Panen Panggang Madu", "https://lh3.googleusercontent.com/aida-public/AB6AXuB-mliEuKtgc9i3DQIhGhcegaTfQCj2ZEFeGpn10sY61nNBuBdLD3zDX7fNNNrxIw5f8FprW3d9sQVMUNYDkPEkLIz6Afk93yK_-J2uD34oRf9ag5dz2XM-MDWoiRk4eJcQQxUFRzaqwrmLoq0mapujMGk0G4k9w73BlNRqBsfsG9jp096JGhkkb9YqN-eWDao462oCyrWbEx4NKp3yywe0Ytt_IjJo0p_Qew9__Jsc50Chg8KALl6WgY0J1tAFtpfzsByEDd7mz_I")
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(savedRecipes) { recipe ->
                    SavedRecipeCard(recipe)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedTopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { /* Menu */ }) {
                    Icon(Icons.Default.Menu, contentDescription = null, tint = Terracotta)
                }
                Text(
                    "Resep Tersimpan",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Terracotta
                    )
                )
            }
        },
        actions = {
            Surface(
                modifier = Modifier.size(32.dp).padding(end = 8.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, WhisperBorder)
            ) {
                AsyncImage(
                    model = "https://lh3.googleusercontent.com/aida-public/AB6AXuA80uz_bZJ0OqU2XYwqBjDWZi6iIvrF82Dn6ReiQjiEwvehS25Xq62wi6VWTgteKM0qK8kIf67He-ktDAVcuYjjWG9NEv7gEIPOQ-p4RR-lhYOZhHQHGR4AaJFYaCfEqQLY1gwU6uyfT2O95TiZZfNRSLlXvmWMNFu887wKKYhjrRphyHkY1sdSpWPvx-0MIq6fIvSlqzNjbly0ZgPSTGe8YVQ9FNaC7TaxL4MdBAVQP5L-rcKN7N4AOXrJNi_Fs9bkjggYYuiJMBc",
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@Composable
fun SearchAndSortSection() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf("Terbaru") }

    Column(modifier = Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari di dapur Anda...", color = MutedSlate) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MutedSlate) },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedIndicatorColor = WhisperBorder,
                focusedIndicatorColor = Terracotta,
                cursorColor = Terracotta
            ),
            singleLine = true
        )

        // Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .background(SurfaceContainerLow, CircleShape)
                    .border(1.dp, WhisperBorder, CircleShape)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SortChip("Terbaru", selectedSort == "Terbaru") { selectedSort = "Terbaru" }
                SortChip("Alfabetis", selectedSort == "Alfabetis") { selectedSort = "Alfabetis" }
            }
            Text("24 Resep", fontSize = 14.sp, color = MutedSlate)
        }
    }
}

@Composable
fun SortChip(label: String, isActive: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isActive) Terracotta else Color.Transparent,
        shape = CircleShape,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            color = if (isActive) Color.White else Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SavedRecipeCard(recipe: RecipeItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, WhisperBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.height(192.dp).fillMaxWidth()) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Bookmark Badge
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(36.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f)
                ) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            Text(
                text = recipe.title,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SavedBottomNavigation() {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        val items = listOf(
            Triple("Beranda", Icons.Default.Home, false),
            Triple("Pindai", Icons.Default.CenterFocusStrong, false),
            Triple("Resep", Icons.Default.RestaurantMenu, false),
            Triple("Tersimpan", Icons.Default.Bookmark, true)
        )

        items.forEach { (label, icon, isSelected) ->
            NavigationBarItem(
                selected = isSelected,
                onClick = { /* Navigasi */ },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Terracotta,
                    selectedTextColor = Terracotta,
                    indicatorColor = SurfaceContainerLow,
                    unselectedIconColor = MutedSlate,
                    unselectedTextColor = MutedSlate
                )
            )
        }
    }
}

data class RecipeItem(val title: String, val imageUrl: String)

@Preview(showBackground = true, device = "spec:width=430dp,height=932dp")
@Composable
fun SavedRecipesPreview() {
    SavedScreen()
}