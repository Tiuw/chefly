package com.skripsi.chefly.data.repository

import android.content.Context
import android.util.Log
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton repository with pagination support.
 * Reads from Room (prepackaged DB) in background threads.
 */
object RecipeRepository {
    private var initialized = false
    private val recipeTokenMap: MutableMap<String, Set<String>> = ConcurrentHashMap()
    private var cachedAllRecipes: List<Recipe>? = null  // Cache untuk all recipes
    private val TAG = "RecipeRepository"
    private const val PAGE_SIZE = 30 // Load 30 recipes per page

    // Use the AppDatabase.getDatabase(...) helper defined in AppDatabase.kt
    private fun getDb(context: Context) = AppDatabase.getDatabase(context)
    private fun getDao(context: Context) = getDb(context).recipeDao()

    // Ensure DB instance is created. Room will copy the prepackaged DB from assets.
    fun init(context: Context) {
        if (initialized) return
        try {
            // Touch DB to trigger createFromAsset copy if needed
            runBlocking { getDao(context).getAllOnceCount() }
            Log.d(TAG, "RecipeRepository initialized successfully")
            initialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize RecipeRepository: ${e.message}")
        }
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

    fun getAllRecipes(context: Context): List<Recipe> = runBlocking {
        val entities = getDao(context).getAllOnceList()
        entities.map { it.toRecipe() }
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
            recipes
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all recipes: ${e.message}")
            emptyList()
        }
    }

    suspend fun getRecipeById(context: Context, id: String): Recipe? = withContext(Dispatchers.IO) {
        try {
            getDao(context).getById(id)?.toRecipe()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recipe by id: ${e.message}")
            null
        }
    }

    suspend fun searchRecipesByQuery(context: Context, query: String): List<Recipe> =
        withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext getAllRecipesSuspend(context)
        val q = query.lowercase().trim()
        val recipes = getAllRecipesSuspend(context)
        // Switch to Default for the CPU-intensive filtering
        withContext(Dispatchers.Default) {
            recipes.filter { recipe ->
                val nameMatches = recipe.name.lowercase().contains(q)
                val tokens: Set<String> = recipe.id?.let { recipeTokenMap[it] } ?: computeTokensForRecipe(recipe)
                val tokenMatches = tokens.any { it.contains(q) }
                nameMatches || tokenMatches
            }
        }
    }

    suspend fun searchRecipesByIngredientsSusp(context: Context, detectedIngredients: List<String>): List<Recipe> =
        withContext(Dispatchers.IO) {
        if (detectedIngredients.isEmpty()) return@withContext emptyList()
        
        val recipes = getAllRecipesSuspend(context)
        
        // Switch to Default for the CPU-intensive scoring calculation
        withContext(Dispatchers.Default) {
            val normalizedDetected = detectedIngredients
                .flatMap { splitAndNormalizeIngredient(it) }
                .filter { it.isNotBlank() }
                .toSet()

            val results = recipes.mapNotNull { recipe ->
                val recipeTokens = recipe.ingredientList.flatMap { splitAndNormalizeIngredient(it) }.filter { it.isNotBlank() }.toSet()
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

            results
        }
    }

    fun getMatchingIngredientsCount(context: Context, id: String, detectedIngredients: List<String>): Pair<Int, Int>? {
        val r = runBlocking { getDao(context).getById(id)?.toRecipe() } ?: return null
        if (detectedIngredients.isEmpty()) return 0 to r.ingredientList.size

        val normalizedDetected = detectedIngredients.flatMap { splitAndNormalizeIngredient(it) }.filter { it.isNotBlank() }.toSet()
        val recipeTokens = r.ingredientList.flatMap { splitAndNormalizeIngredient(it) }.filter { it.isNotBlank() }.toSet()

        val matched = normalizedDetected.intersect(recipeTokens).size
        return matched to recipeTokens.size
    }

    // Suspend version for non-blocking computation
    suspend fun getMatchingIngredientsCountSuspend(context: Context, id: String, detectedIngredients: List<String>): Pair<Int, Int>? = 
        withContext(Dispatchers.IO) {
        val r = getDao(context).getById(id)?.toRecipe() ?: return@withContext null
        if (detectedIngredients.isEmpty()) return@withContext 0 to r.ingredientList.size

        val normalizedDetected = detectedIngredients.flatMap { splitAndNormalizeIngredient(it) }.filter { it.isNotBlank() }.toSet()
        val recipeTokens = r.ingredientList.flatMap { splitAndNormalizeIngredient(it) }.filter { it.isNotBlank() }.toSet()

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
}
