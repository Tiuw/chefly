package com.skripsi.chefly.data.repository

import android.content.Context
import android.util.Log
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton repository with pagination support.
 * Reads from Room (prepackaged DB) in background threads.
 */
object RecipeRepository {
    private var initialized = false
    private var cacheWarm = false
    private val recipeTokenMap: MutableMap<String, Set<String>> = ConcurrentHashMap()
    private val recipeIngredientTokenMap: MutableMap<String, Set<String>> = ConcurrentHashMap()
    private val cachedRecipeById: MutableMap<String, Recipe> = ConcurrentHashMap()
    private var cachedAllRecipes: List<Recipe>? = null
    private val TAG = "RecipeRepository"
    private const val PAGE_SIZE = 30

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
            e.printStackTrace() // Ini akan memunculkan stacktrace lengkap di Logcat
        }
    }

    suspend fun preloadAllRecipes(context: Context) = withContext(Dispatchers.IO) {
        if (cacheWarm) return@withContext
        getAllRecipesSuspend(context)
        cacheWarm = true
    }

    suspend fun getRecipeCount(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            getDao(context).getAllOnceCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recipe count: ${e.message}")
            0
        }
    }

    suspend fun getRecipesPaged(context: Context, pageNumber: Int): List<Recipe> =
        withContext(Dispatchers.IO) {
            try {
                val offset = pageNumber * PAGE_SIZE
                val limit = PAGE_SIZE
                val entities = getDao(context).getRecipesPaginated(limit, offset)
                // Best Practice: map toDomain()
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
                Log.e("RecipeRepository", "Error loading recommended recipes: ${e.message}")
                emptyList()
            }
        }

    suspend fun getRecipesByCategory(context: Context, category: String): List<Recipe> =
        withContext(Dispatchers.IO) {
            val q = category.trim()
            if (q.isBlank()) return@withContext emptyList()

            try {
                val startedAt = System.currentTimeMillis()
                val recipes = getDao(context).searchByCategory(q).map { entity ->
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
            if (q.isBlank()) return@withContext getRecipesPaged(context, 0)

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
        // Karena titleCleaned dihapus, kita gunakan recipe.name
        val nameTokens = recipe.name.lowercase().split(Regex("\\s+"))
        val ingredientTokens = recipe.ingredientList.flatMap { splitAndNormalizeIngredient(it) }
        return (nameTokens + ingredientTokens).filter { it.isNotBlank() }.toSet()
    }

    private fun normalizeIngredientTokens(items: List<String>): Set<String> {
        return items.flatMap { splitAndNormalizeIngredient(it) }.filter { it.isNotBlank() }.toSet()
    }
}