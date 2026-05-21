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

/**
 * Data class untuk output hasil perangkingan resep ke UI layer
 */
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

    // RAM Cache lokal untuk menghindari query SQLite berulang saat scroll / komposisi ulang UI
    private var idfMap: Map<String, Float>? = null
    private var recipeVectorsMap: Map<String, Map<String, Float>>? = null
    private var recipeTitlesMap: Map<String, String>? = null

    /**
     * Memuat kamus bobot IDF dan representasi Vektor Resep langsung dari SQLite Room Local
     */
    suspend fun initializeRecommendationData() = withContext(Dispatchers.IO) {
        if (idfMap != null && recipeVectorsMap != null && recipeTitlesMap != null) return@withContext

        try {
            Log.d(TAG, "🔄 Memulai sinkronisasi data IR dari Room Database...")

            // 1. Ambil data kamus IDF dari tabel idf_dictionary via Dao
            val idfEntities = recipeDao.getFullDictionary()
            idfMap = idfEntities.associate { it.ingredient to it.idfWeight.toFloat() }

            // 2. Ambil data representasi matriks dari tabel tfidf_data via Dao
            val tfidfEntities = recipeDao.getAllTfidfData()
            val vectors = mutableMapOf<String, Map<String, Float>>()
            val type = object : TypeToken<Map<String, Float>>() {}.type

            tfidfEntities.forEach { entity ->
                val vectorMap: Map<String, Float> = gson.fromJson(entity.vectorJson, type)
                // entity.recipeId (String) masuk ke key String map penampung tanpa mismatch
                vectors[entity.recipeId] = vectorMap
            }
            recipeVectorsMap = vectors

            // 3. Ambil data nama/judul resep dari tabel resep utama untuk pemetaan UI
            val allRecipes = recipeDao.getAllOnceList()
            recipeTitlesMap = allRecipes.associate { it.id to (it.title ?: "Resep Tanpa Nama") }

            Log.i(TAG, "✅ Sukses Sinkronisasi SQLite: ${idfMap?.size} Kamus IDF & ${recipeVectorsMap?.size} Vektor Resep Berhasil Dicache.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Gagal menginisialisasi mesin komputasi IR: ${e.message}", e)
        }
    }

    /**
     * FUNGSI INTI: Komputasi Vektor Jarak Spasial Menggunakan Rumus Cosine Similarity
     * @param userIngredients Daftar komponen bahan pangan masukan (Kamera YOLO26 / Centang Manual)
     */
    suspend fun getRecommendations(userIngredients: List<String>): List<RecommendationResult> = withContext(Dispatchers.Default) {
        if (idfMap == null || recipeVectorsMap == null || recipeTitlesMap != null) {
            initializeRecommendationData()
        }

        val currentIdf = idfMap ?: return@withContext emptyList()
        val currentVectors = recipeVectorsMap ?: return@withContext emptyList()
        val currentTitles = recipeTitlesMap ?: return@withContext emptyList()

        val cleanUserIngredients = userIngredients.map { rawInput ->
            rawInput.trim()
                .lowercase()
                .replace(" ", "_") // 🟢 Mengunci spasi kembali menjadi underscore (_) agar sinkron 100% dengan database kamu
        }.distinct()

        // --- 2. PEMBENTUKAN VEKTOR QUERY USER ---
        val userVector = mutableMapOf<String, Float>()
        for (ingredient in cleanUserIngredients) {
            val idfValue = currentIdf[ingredient] ?: 0f
            if (idfValue > 0f) {
                userVector[ingredient] = idfValue
            }
        }

// Pantau hasilnya di Logcat untuk sidang
        android.util.Log.d("Chefly_Math_Debug", "Bahan dari UI: $userIngredients")
        android.util.Log.d("Chefly_Math_Debug", "Format yang masuk ke Cosine: $cleanUserIngredients")
        android.util.Log.d("Chefly_Math_Debug", "Vector Query Terbentuk: $userVector")

        if (userVector.isEmpty()) return@withContext emptyList()

        // --- 2. PERHITUNGAN COSINE SIMILARITY (Tetap Murni Tanpa Merusak Rumus Matematika) ---
        var userMagnitudeSquared = 0f
        for (weight in userVector.values) { userMagnitudeSquared += weight * weight }
        val userMagnitude = sqrt(userMagnitudeSquared)
        if (userMagnitude == 0f) return@withContext emptyList()

        val results = mutableListOf<RecommendationResult>()

        for ((recipeId, ingredientsVector) in currentVectors) {
            var dotProduct = 0f
            var recipeMagnitudeSquared = 0f

            for (weight in ingredientsVector.values) { recipeMagnitudeSquared += weight * weight }
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
                        similarityScore = cosineSimilarityScore
                    )
                )
            }
        }

        return@withContext results.sortedByDescending { it.similarityScore }
    }
}