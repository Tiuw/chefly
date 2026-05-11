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
    private var cachedAllRecipes: List<Recipe>? = null  // Cache untuk all recipes
    private val TAG = "RecipeRepository"
    private const val PAGE_SIZE = 30 // Load 30 recipes per page

    // Use the AppDatabase.getDatabase(...) helper defined in AppDatabase.kt
    private fun getDb(context: Context) = AppDatabase.getDatabase(context)
    private fun getDao(context: Context) = getDb(context).recipeDao()

    // Ensure DB instance is created. Room will copy the prepackaged DB from assets.
    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        try {
            // Touch DB to trigger createFromAsset copy if needed
            getDao(context).getAllOnceCount()
            Log.d(TAG, "RecipeRepository initialized successfully")
            initialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize RecipeRepository: ${e.message}")
        }
    }

    suspend fun preloadAllRecipes(context: Context) = withContext(Dispatchers.IO) {
        if (cacheWarm) return@withContext
        getAllRecipesSuspend(context)
        cacheWarm = true
    }

    /**
     * Get total recipe count
     */
    suspend fun getRecipeCount(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            getDao(context).getAllOnceCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recipe count: ${e.message}")
            0
        }
    }

    /**
     * Get paginated recipes
     * @param pageNumber 0-based page number
     */
    suspend fun getRecipesPaged(context: Context, pageNumber: Int): List<Recipe> =
        withContext(Dispatchers.IO) {
        try {
            val offset = pageNumber * PAGE_SIZE
            val limit = PAGE_SIZE
            val entities = getDao(context).getRecipesPaginated(limit, offset)
            Log.d(TAG, "Loaded page $pageNumber with ${entities.size} recipes")
            entities.map { it.toRecipe() }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading recipes for page $pageNumber: ${e.message}")
            emptyList()
        }
    }

    suspend fun getRecommendedRecipes(context: Context, limit: Int = PAGE_SIZE): List<Recipe> =
        withContext(Dispatchers.IO) {
            try {
                getDao(context).getRecommendedRecipes(limit).map { it.toRecipe() }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading recommended recipes: ${e.message}")
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
                    val recipe = entity.toRecipe()
                    recipe.id?.let { id ->
                        cachedRecipeById[id] = recipe
                        recipeTokenMap[id] = computeTokensForRecipe(recipe)
                        recipeIngredientTokenMap[id] = normalizeIngredientTokens(recipe.ingredientList)
                    }
                    recipe
                }
                Log.d(TAG, "Category search '$q' returned ${recipes.size} recipes in ${System.currentTimeMillis() - startedAt}ms")
                recipes
            } catch (e: Exception) {
                Log.e(TAG, "Error searching recipes by category: ${e.message}")
                emptyList()
            }
        }

    suspend fun getAllRecipes(context: Context): List<Recipe> = withContext(Dispatchers.IO) {
        getAllRecipesSuspend(context)
    }

    // Suspend version with caching for better performance
    private suspend fun getAllRecipesSuspend(context: Context): List<Recipe> = withContext(Dispatchers.IO) {
        if (cachedAllRecipes != null) {
            return@withContext cachedAllRecipes!!
        }
        try {
            val entities = getDao(context).getAllOnceList()
            val recipes = entities.map { it.toRecipe() }
            cachedAllRecipes = recipes
            recipes.forEach { recipe ->
                recipe.id?.let { id ->
                    cachedRecipeById[id] = recipe
                    recipeTokenMap[id] = computeTokensForRecipe(recipe)
                    recipeIngredientTokenMap[id] = normalizeIngredientTokens(recipe.ingredientList)
                }
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
            getDao(context).getById(id)?.toRecipe()
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
            val startedAt = System.currentTimeMillis()
            val entities = getDao(context).searchByKeyword(q)
            val recipes = entities.map { entity ->
                val recipe = entity.toRecipe()
                recipe.id?.let { id ->
                    cachedRecipeById[id] = recipe
                    recipeTokenMap[id] = computeTokensForRecipe(recipe)
                    recipeIngredientTokenMap[id] = normalizeIngredientTokens(recipe.ingredientList)
                }
                recipe
            }
            Log.d(TAG, "Keyword search '$q' returned ${recipes.size} recipes in ${System.currentTimeMillis() - startedAt}ms")
            recipes
        } catch (e: Exception) {
            Log.e(TAG, "Error searching recipes by keyword: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchRecipesByIngredientsSusp(context: Context, detectedIngredients: List<String>): List<Recipe> =
        withContext(Dispatchers.IO) {
        if (detectedIngredients.isEmpty()) return@withContext emptyList()
        
        val startedAt = System.currentTimeMillis()
        val recipes = getAllRecipesSuspend(context)
        
        // Switch to Default for the CPU-intensive scoring calculation
        val results = withContext(Dispatchers.Default) {
            val normalizedDetected = detectedIngredients
                .flatMap { splitAndNormalizeIngredient(it) }
                .filter { it.isNotBlank() }
                .toSet()

            recipes.mapNotNull { recipe ->
                val recipeTokens = recipe.id?.let { recipeIngredientTokenMap[it] }
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
        Log.d(TAG, "Ingredient search returned ${results.size} recipes in ${System.currentTimeMillis() - startedAt}ms")
        results
    }


    // Suspend version for non-blocking computation
    suspend fun getMatchingIngredientsCountSuspend(context: Context, id: String, detectedIngredients: List<String>): Pair<Int, Int>? = 
        withContext(Dispatchers.IO) {
        val r = cachedRecipeById[id] ?: getDao(context).getById(id)?.toRecipe() ?: return@withContext null
        if (detectedIngredients.isEmpty()) return@withContext 0 to r.ingredientList.size

        val normalizedDetected = detectedIngredients.flatMap { splitAndNormalizeIngredient(it) }.filter { it.isNotBlank() }.toSet()
        val recipeTokens = recipeIngredientTokenMap[id] ?: normalizeIngredientTokens(r.ingredientList)

        val matched = normalizedDetected.intersect(recipeTokens).size
        matched to recipeTokens.size
    }

    // Tokenization helpers (kept from original implementation)
    private fun splitAndNormalizeIngredient(text: String): List<String> {
        val cleaned = text
            .lowercase()
            .replace("(", "")
            .replace(")", "")
            .replace("[", "")
            .replace("]", "")
            .replace("\"", "")
            .replace(Regex("[^a-z0-9\\s-]"), " ")
            .replace(Regex("\\s{2,}"), " ")

        return cleaned
            .split(Regex("[,;:/\\-]|\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun computeTokensForRecipe(recipe: Recipe): Set<String> {
        val nameTokens = (recipe.titleCleaned?.lowercase() ?: recipe.name.lowercase())
        val ingredientTokens = recipe.ingredientList.flatMap { splitAndNormalizeIngredient(it) }
        val combined = mutableSetOf<String>()
        combined.addAll(nameTokens.split(Regex("\\s+")))
        combined.addAll(ingredientTokens.map { it.lowercase() })
        return combined.filter { it.isNotBlank() }.toSet()
    }

    private fun normalizeIngredientTokens(items: List<String>): Set<String> {
        return items
            .flatMap { splitAndNormalizeIngredient(it) }
            .filter { it.isNotBlank() }
            .toSet()
    }
}
