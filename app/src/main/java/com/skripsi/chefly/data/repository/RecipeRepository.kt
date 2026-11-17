package com.skripsi.chefly.data.repository

import android.content.Context
import android.util.Log
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.local.AppDatabase
import com.skripsi.chefly.data.local.RecipeDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton repository with pagination support.
 * Reads from Room (prepackaged DB) in background threads.
 */
@Singleton
class RecipeRepository @Inject constructor(
    private val recipeDao: RecipeDao
) {

    companion object {
        private var initialized = false
        private var cacheWarm = false
        private val recipeTokenMap: MutableMap<String, Set<String>> = ConcurrentHashMap()
        private val recipeIngredientTokenMap: MutableMap<String, Set<String>> = ConcurrentHashMap()
        private val cachedRecipeById: MutableMap<String, Recipe> = ConcurrentHashMap()
        private var cachedAllRecipes: List<Recipe>? = null
        private val TAG = "RecipeRepository"
        private const val PAGE_SIZE = 30
    }

    private fun getDb(context: Context) = AppDatabase.getDatabase(context)
    private fun getDao(context: Context) = getDb(context).recipeDao()

    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        try {
            Log.d(TAG, "Mencoba inisialisasi database...")
            val count = getDao(context).getAllOnceCount()
            Log.d(TAG, "Inisialisasi sukses. Total data ditemukan: $count")
            initialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Gagal inisialisasi Repo: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun getRecipeCount(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            getDao(context).getAllOnceCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recipe count: ${e.message}")
            0
        }
    }

    // DISATUKAN & DIPERBAIKI: Menggunakan query paginasi kategori yang valid
    suspend fun getRecipesPaged(context: Context, category: String, pageNumber: Int): List<Recipe> =
        withContext(Dispatchers.IO) {
            try {
                val offset = pageNumber * PAGE_SIZE
                val limit = PAGE_SIZE

                val entities = if (category.equals("Semua", ignoreCase = true) || category.isBlank()) {
                    // JIKA "Semua", kirim "%" ke DAO.
                    // DAO Anda sudah memiliki klausa ORDER BY loves DESC secara default!
                    getDao(context).getRecipesByCategoryPaginated("%", limit, offset)
                } else {
                    // Jika kategori spesifik (Ayam, Sapi, dll), ambil per kategori tetap urut loves DESC
                    getDao(context).getRecipesByCategoryPaginated(category, limit, offset)
                }

                entities.map { it.toDomain() }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading recipes for page $pageNumber: ${e.message}")
                emptyList()
            }
        }

    suspend fun getRecommendedRecipes(context: Context, limit: Int = 10): List<Recipe> =
        withContext(Dispatchers.IO) {
            try {
                val entities = getDao(context).getRecommendedRecipes(limit)
                entities.map { it.toDomain() }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading recommended recipes: ${e.message}")
                emptyList()
            }
        }

    // DIPERBAIKI: Parameter 'method' dihapus, beralih ke searchByKeywordAndCategory bawaan SQLite
    suspend fun searchRecipesWithFilters(
        context: Context,
        query: String,
        category: String
    ): List<Recipe> = withContext(Dispatchers.IO) {
        try {
            // 1. Pecah query menjadi potongan kata (misal: "cabe merah" jadi ["cabe", "merah"])
            val words = query.trim().split(Regex("\\s+")).filter { it.length > 1 }

            // 2. Jika hanya 1 kata, gunakan kueri biasa lewat SQLite langsung
            if (words.size <= 1) {
                val entities = getDao(context).searchByKeywordAndCategory(query.trim(), category)
                return@withContext entities.map { it.toDomain() }
            }

            // 3. Jika banyak kata, kita ambil resep di kategori tersebut (limit 500) lalu filter presisi di Kotlin
            val allInContext = getDao(context).getRecipesByCategoryPaginated(category, 500, 0)

            allInContext.filter { entity ->
                val content = "${entity.title} ${entity.uiIngredients}".lowercase().replace("_", " ")
                // Pastikan SEMUA kata yang dicari ada di dalam judul atau bahan
                words.all { word -> content.contains(word.lowercase()) }
            }.map { it.toDomain() }

        } catch (e: Exception) {
            Log.e(TAG, "Error in searchRecipesWithFilters: ${e.message}")
            emptyList()
        }
    }

    suspend fun getRecipesByCategory(context: Context, category: String): List<Recipe> =
        withContext(Dispatchers.IO) {
            val q = category.trim()
            if (q.isBlank()) return@withContext emptyList()

            try {
                // Ambil halaman pertama (limit 30) untuk inisialisasi kategori resep
                val recipes = getDao(context).getRecipesByCategoryPaginated(q, PAGE_SIZE, 0).map { entity ->
                    val recipe = entity.toDomain()
                    val id = recipe.id
                    cachedRecipeById[id] = recipe
                    recipeTokenMap[id] = computeTokensForRecipe(recipe)
                    recipeIngredientTokenMap[id] = normalizeIngredientTokens(recipe.ingredientList)
                    recipe
                }
                Log.d(TAG, "Category search '$q' returned ${recipes.size} recipes")
                recipes
            } catch (e: Exception) {
                Log.e(TAG, "Error searching recipes by category: ${e.message}")
                emptyList()
            }
        }

    private suspend fun getAllRecipesSuspend(context: Context): List<Recipe> = withContext(Dispatchers.IO) {
        if (cachedAllRecipes != null) return@withContext cachedAllRecipes!!
        try {
            val entities = getDao(context).getAllOnceList()
            val recipes = entities.map { it.toDomain() }
            cachedAllRecipes = recipes
            recipes.forEach { recipe ->
                val id = recipe.id
                cachedRecipeById[id] = recipe
                recipeTokenMap[id] = computeTokensForRecipe(recipe)
                recipeIngredientTokenMap[id] = normalizeIngredientTokens(recipe.ingredientList)
            }
            recipes
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all recipes: ${e.message}")
            emptyList()
        }
    }

    suspend fun getRecipeById(context: Context, id: String): Recipe? = withContext(Dispatchers.IO) {
        try {
            cachedRecipeById[id]?.let { return@withContext it }
            getDao(context).getById(id)?.toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recipe by id: ${e.message}")
            null
        }
    }

    suspend fun searchRecipesByQuery(context: Context, query: String): List<Recipe> =
        withContext(Dispatchers.IO) {
            val q = query.trim()

            if (q.isBlank()) return@withContext getRecipesPaged(context, "", 0)

            try {
                val entities = getDao(context).searchByKeyword(q)
                entities.map { entity ->
                    val recipe = entity.toDomain()
                    val id = recipe.id
                    cachedRecipeById[id] = recipe
                    recipeTokenMap[id] = computeTokensForRecipe(recipe)
                    recipeIngredientTokenMap[id] = normalizeIngredientTokens(recipe.ingredientList)
                    recipe
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching recipes by keyword: ${e.message}")
                emptyList()
            }
        }

    suspend fun searchRecipesByIngredientsSusp(context: Context, detectedIngredients: List<String>): List<Recipe> =
        withContext(Dispatchers.IO) {
            if (detectedIngredients.isEmpty()) return@withContext emptyList()

            val recipes = getAllRecipesSuspend(context)

            withContext(Dispatchers.Default) {
                val normalizedDetected = detectedIngredients
                    .flatMap { splitAndNormalizeIngredient(it) }
                    .filter { it.isNotBlank() }
                    .toSet()

                recipes.mapNotNull { recipe ->
                    val recipeTokens = recipeIngredientTokenMap[recipe.id]
                        ?: normalizeIngredientTokens(recipe.ingredientList)
                    if (recipeTokens.isEmpty()) return@mapNotNull null

                    val intersectionSize = normalizedDetected.intersect(recipeTokens).size
                    if (intersectionSize == 0) return@mapNotNull null

                    val unionSize = normalizedDetected.union(recipeTokens).size.coerceAtLeast(1)
                    val coverageScore = intersectionSize.toFloat() / recipeTokens.size
                    val jaccardScore = intersectionSize.toFloat() / unionSize

                    val finalScore = (coverageScore * 0.7f) + (jaccardScore * 0.3f)
                    recipe to finalScore
                }
                    .sortedByDescending { it.second }
                    .map { it.first }
            }
        }

    suspend fun getMatchingIngredientsCountSuspend(context: Context, id: String, detectedIngredients: List<String>): Pair<Int, Int>? =
        withContext(Dispatchers.IO) {
            val r = cachedRecipeById[id] ?: getDao(context).getById(id)?.toDomain() ?: return@withContext null
            if (detectedIngredients.isEmpty()) return@withContext 0 to r.ingredientList.size

            val normalizedDetected = detectedIngredients.flatMap { splitAndNormalizeIngredient(it) }.toSet()
            val recipeTokens = recipeIngredientTokenMap[id] ?: normalizeIngredientTokens(r.ingredientList)

            val matched = normalizedDetected.intersect(recipeTokens).size
            matched to recipeTokens.size
        }

    private fun splitAndNormalizeIngredient(text: String): List<String> = text
        .lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), " ")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    private fun computeTokensForRecipe(recipe: Recipe): Set<String> {
        val nameTokens = recipe.name.lowercase().split(Regex("\\s+"))
        val ingredientTokens = recipe.ingredientList.flatMap { splitAndNormalizeIngredient(it) }
        return (nameTokens + ingredientTokens).filter { it.isNotBlank() }.toSet()
    }

    private fun normalizeIngredientTokens(items: List<String>): Set<String> {
        return items.flatMap { splitAndNormalizeIngredient(it) }.filter { it.isNotBlank() }.toSet()
    }

    suspend fun getUniqueCategories(context: Context): List<String> = withContext(Dispatchers.IO) {
        try {
            getDao(context).getUniqueCategories()
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Error fetching unique categories: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchRecipesByTitle(
        context: Context,
        query: String,
        category: String,
        page: Int
    ): List<Recipe> = withContext(Dispatchers.IO) {
        try {
            val offset = page * 30 // PAGE_SIZE
            val categoryQuery = if (category == "Semua") "%" else category
            val entities = getDao(context).searchByTitleAndCategoryPaginated(query, categoryQuery, 30, offset)
            entities.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Error searching recipes by title: ${e.message}")
            emptyList()
        }
    }
}