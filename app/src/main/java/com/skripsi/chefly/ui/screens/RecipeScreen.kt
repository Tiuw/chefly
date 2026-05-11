package com.skripsi.chefly.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.skripsi.chefly.ui.theme.MutedSlate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeExploreScreen() {
    Scaffold(
        topBar = { ExploreTopBar() },
        floatingActionButton = { ExploreFAB() },
        containerColor = WarmIvory
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Search Bar
            SearchBarSection()

            // Categories
            CategoriesSection()

            // Recipe Grid
            RecommendationsSection()

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreTopBar() {
    TopAppBar(
        title = {
            Text(
                "Resep",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Terracotta
            )
        },
        navigationIcon = {
            IconButton(onClick = { /* Menu Action */ }) {
                Icon(Icons.Default.Menu, contentDescription = null, tint = Terracotta)
            }
        },
        actions = {
            Surface(
                modifier = Modifier.size(32.dp).padding(end = 8.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, WhisperBorder)
            ) {
                AsyncImage(
                    model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCVbB41s3QDA3JOMYwa9dmtunF8D44UoqcM1gc1fczYRc1fbuNqm_QxZ8ncCvxZA5b1SIFAwSk6wC_ZI7kC6Mzq7jdn4P4Rr8MA8MftiHREQ9WfkI4iyQvev0WBNHfyv-vrKQ6-Nyplj6ldSYDRVjcE52j5G_DVl7CmlGZ9La3d-tWgSCQ7SKvls4GOeUJldvOUx1nwdW1bunwbdxappfQ1n5Z7FszcW7GaTKdkE0A8eGSHjT_HO2v3zBMnlWHn6-z7hyAuBZps4fs",
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@Composable
fun SearchBarSection() {
    Box(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari resep, bahan...", color = MutedSlate) },
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
    }
}

@Composable
fun CategoriesSection() {
    val categories = listOf(
        CategoryData("Ayam", Icons.Default.Restaurant, true),
        CategoryData("Sapi", Icons.Default.DinnerDining, false),
        CategoryData("Telur", Icons.Default.EggAlt, false),
        CategoryData("Tahu", Icons.Default.BakeryDining, false),
        CategoryData("Tempe", Icons.Default.BreakfastDining, false),
        CategoryData("Ikan", Icons.Default.SetMeal, false)
    )

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            "Kategori",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories) { category ->
                CategoryCard(category)
            }
        }
    }
}

@Composable
fun CategoryCard(category: CategoryData) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(if (category.isActive) 2.dp else 1.dp, if (category.isActive) Terracotta else WhisperBorder),
            shadowElevation = 1.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    category.icon,
                    contentDescription = null,
                    tint = if (category.isActive) Terracotta else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            category.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (category.isActive) Terracotta else Color.Gray
        )
    }
}

@Composable
fun RecommendationsSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rekomendasi untuk Anda", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("LIHAT SEMUA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Terracotta)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid simulasi (karena di dalam Scrollable Column)
        val items = listOf(
            RecipeGridData("Artisan Avocado & Radish Sourdough", "https://lh3.googleusercontent.com/aida-public/AB6AXuAl8ut6fVvMkKLq3isHMBuvVyiWVFckfOErBjXR2oBQHAJ0D1f8rL3t8oKirrxRdc0MxzVgxeIHqOor7E3kmFZQFXEZBWsdPftWsMbPbO_yirmrM-dv_XnvkJRPh1r_4sZBzs77mqhTUf3uV11yQcrxhwXlg8SJiiPvALqXWLfU_8ntKnTDhLpiMvTUGH38eaYd94Xrfzn_jDkpQ_dE9O7zXgKFwIbLsywuP2dmaTmJ6b8hZU4VaDbEDQzAQrYCUs31DTBINzX7ArE", true),
            RecipeGridData("Crispy Chickpea Kale Mediterranean", "https://lh3.googleusercontent.com/aida-public/AB6AXuBLWoXJz_5ufItO5ge1QaMxXWH7dm-ZORCqq7BtFmD-ojpmtsi6Zsha7qc53CoRR80sqKioXNHVS5ixkAs87cUm8FmgargsXYb0MHyR1x0YrYxudLbNsMj5_Fzv5aUu4sKIjlDCO3SOjOQU1-cF8hNWL2ZwucSkBDn62FGXxuhOALc4I-_iosSxXGjpPGkf4b7YEtP85azgnqA7TPvjgyjSiBojy6lHCygY71fx375vaTPbpWz3X_Ww8ir_rkU0XMN1HjmqRVtsplo", false),
            RecipeGridData("Roasted Pine Nut & Basil Linguine", "https://lh3.googleusercontent.com/aida-public/AB6AXuDo0sV2S_ov4JHr_w5x0F6ChKZ_UgOwqQ2ESqaazSzdpF02D01aO31X1O_44TO0YraHpayft9cDFA-vhTaxFDvtLlkbgWr3gJKpmmLgrDr92HjdmUd6wa_I1RrMPFf06-oUfcNKgwg2hTreZSt37pjiTn3JDsWQwoBsG8YILasGrScbROsbMwc526_KM9BIDipB_k380g_V_mlLi-gAIpfqMX_q-79pX-EKe4k_LXSYNI4DhNVIgZB7JcPUooplkXZUQueuPDecH5M", false),
            RecipeGridData("Summer Berry & Greek Yogurt", "https://lh3.googleusercontent.com/aida-public/AB6AXuCq88n3gzPT_CVFWPS_PrfSF3BlRSOvysjLLL8ilD5eXsEFq4_P7v-l3_gt6x5er9YrOxcsitfOPWFGngq7yRhxrurcjUoLFyYj3b7WKd8jjeTkgbgH3U9mKO2uBclbSVS8r2gcFlDHPCQxI2KEB9A15L92GyGUN_hSAISuktpO14YKWQCSewdzakFI7coYrbAH8R-WiTF9sjIwmXFCU-xiVSIc1mkq0yaLck31wiuSaGuelduQSaw_JgiUpqXvC4MTZICETosELLo", false)
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items.chunked(2).forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowItems.forEach { item ->
                        RecipeGridItem(item, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeGridItem(data: RecipeGridData, modifier: Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
        ) {
            AsyncImage(
                model = data.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.9f)
            ) {
                Icon(
                    imageVector = if (data.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (data.isFavorite) Terracotta else MutedSlate,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = data.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun ExploreFAB() {
    FloatingActionButton(
        onClick = { /* Scan */ },
        containerColor = Terracotta,
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier
            .size(80.dp)
            .padding(bottom = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(32.dp))
            Text("PINDAI", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

data class CategoryData(val name: String, val icon: ImageVector, val isActive: Boolean)
data class RecipeGridData(val title: String, val imageUrl: String, val isFavorite: Boolean)

@Preview(showBackground = true, device = "spec:width=430dp,height=932dp")
@Composable
fun ExplorePreview() {
    RecipeExploreScreen()
}