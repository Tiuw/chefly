package com.skripsi.chefly.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skripsi.chefly.data.local.RecipeDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

data class RecommendationResult(
    val recipeId: Int,
    val title: String,
    val similarityScore: Float
)

@Singleton
class RecipeRecommendationSystem @Inject constructor(
    private val recipeDao: RecipeDao
) {
    private val TAG = "Chefly_IR_System"
    private val gson = Gson()

    private var idfMap: Map<String, Float>? = null
    private var recipeVectorsMap: Map<String, Map<String, Float>>? = null
    private var recipeTitlesMap: Map<String, String>? = null

    suspend fun initializeRecommendationData() = withContext(Dispatchers.IO) {
        if (idfMap != null && recipeVectorsMap != null && recipeTitlesMap != null) return@withContext

        try {
            Log.d(TAG, "🔄 Memulai sinkronisasi data IR dari Room Database...")

            val idfEntities = recipeDao.getFullDictionary()
            idfMap = idfEntities.associate { it.ingredient to it.idfWeight.toFloat() }

            val tfidfEntities = recipeDao.getAllTfidfData()
            val vectors = mutableMapOf<String, Map<String, Float>>()
            val type = object : TypeToken<Map<String, Float>>() {}.type

            tfidfEntities.forEach { entity ->
                val vectorMap: Map<String, Float> = gson.fromJson(entity.vectorJson, type)
                vectors[entity.recipeId] = vectorMap
            }
            recipeVectorsMap = vectors

            val allRecipes = recipeDao.getAllOnceList()
            recipeTitlesMap = allRecipes.associate { it.id to (it.title ?: "Resep Tanpa Nama") }

            Log.i(TAG, "✅ Sukses Sinkronisasi SQLite: ${idfMap?.size} Kamus IDF & ${recipeVectorsMap?.size} Vektor Resep.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Gagal menginisialisasi mesin komputasi IR: ${e.message}", e)
        }
    }

    suspend fun getRecommendations(
        userIngredients: List<String>,
        topN: Int = 10,
        minMatchCount: Int = 2 // Diselaraskan dengan Python
    ): List<RecommendationResult> = withContext(Dispatchers.Default) {
        // Perbaikan kondisi cache (harus == null)
        if (idfMap == null || recipeVectorsMap == null || recipeTitlesMap == null) {
            initializeRecommendationData()
        }

        val currentIdf = idfMap ?: return@withContext emptyList()
        val currentVectors = recipeVectorsMap ?: return@withContext emptyList()
        val currentTitles = recipeTitlesMap ?: return@withContext emptyList()

        val cleanUserIngredients = userIngredients.map { rawInput ->
            rawInput.trim()
                .lowercase()
                .replace(" ", "_")
        }.distinct()

        val userVector = mutableMapOf<String, Float>()
        for (ingredient in cleanUserIngredients) {
            val idfValue = currentIdf[ingredient] ?: 0f
            if (idfValue > 0f) {
                userVector[ingredient] = idfValue
            }
        }

        if (userVector.isEmpty()) return@withContext emptyList()

        var userMagnitudeSquared = 0f
        for (weight in userVector.values) {
            userMagnitudeSquared += weight * weight
        }
        val userMagnitude = sqrt(userMagnitudeSquared)
        if (userMagnitude == 0f) return@withContext emptyList()

        val results = mutableListOf<RecommendationResult>()

        for ((recipeId, ingredientsVector) in currentVectors) {
            // FILTER MIN_MATCH_COUNT (Sama seperti Python)
            var matchCount = 0
            for (ing in cleanUserIngredients) {
                if (ingredientsVector.containsKey(ing)) {
                    matchCount++
                }
            }

            if (matchCount < minMatchCount) {
                continue
            }

            var dotProduct = 0f
            var recipeMagnitudeSquared = 0f

            for (weight in ingredientsVector.values) {
                recipeMagnitudeSquared += weight * weight
            }
            val recipeMagnitude = sqrt(recipeMagnitudeSquared)
            if (recipeMagnitude == 0f) continue

            for ((ingredient, userWeight) in userVector) {
                val recipeWeight = ingredientsVector[ingredient] ?: 0f
                if (recipeWeight > 0f) {
                    dotProduct += userWeight * recipeWeight
                }
            }

            val cosineSimilarityScore = if (dotProduct > 0f) {
                dotProduct / (userMagnitude * recipeMagnitude)
            } else {
                0f
            }

            if (cosineSimilarityScore > 0f) {
                results.add(
                    RecommendationResult(
                        recipeId = recipeId.toIntOrNull() ?: 0,
                        title = currentTitles[recipeId] ?: "Resep #$recipeId",
                        similarityScore = (cosineSimilarityScore * 10000).toInt() / 10000f // Pembulatan 4 desimal
                    )
                )
            }
        }

        return@withContext results
            .sortedByDescending { it.similarityScore }
            .take(topN)
    }
}