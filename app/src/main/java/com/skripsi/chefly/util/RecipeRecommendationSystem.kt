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
        // Otomatis jalankan inisialisasi jika data cache memori masih kosong
        if (idfMap == null || recipeVectorsMap == null || recipeTitlesMap == null) {
            initializeRecommendationData()
        }

        val currentIdf = idfMap ?: return@withContext emptyList()
        val currentVectors = recipeVectorsMap ?: return@withContext emptyList()
        val currentTitles = recipeTitlesMap ?: return@withContext emptyList()

        // --- 1. PEMBENTUKAN VEKTOR QUERY USER ---
        val cleanUserIngredients = userIngredients.map { it.trim().lowercase() }
        val userVector = mutableMapOf<String, Float>()

        for (ingredient in cleanUserIngredients) {
            val idfValue = currentIdf[ingredient] ?: 0f
            if (idfValue > 0f) {
                userVector[ingredient] = idfValue // Bobot W_iq (TF [1] * IDF)
            }
        }

        // Jika tidak ada satu pun bahan masukan yang dikenali kamus IDF, batalkan kalkulasi
        if (userVector.isEmpty()) return@withContext emptyList()

        // Menghitung Magnitude / Panjang Vektor Query (|Q|)
        var userMagnitudeSquared = 0f
        for (weight in userVector.values) {
            userMagnitudeSquared += weight * weight
        }
        val userMagnitude = sqrt(userMagnitudeSquared)
        if (userMagnitude == 0f) return@withContext emptyList()

        val results = mutableListOf<RecommendationResult>()

        // --- 2. PERHITUNGAN COSINE SIMILARITY TERHADAP SETIAP DOKUMEN RESEP ---
        for ((recipeId, ingredientsVector) in currentVectors) {
            var dotProduct = 0f
            var recipeMagnitudeSquared = 0f

            // Menghitung Magnitude Vektor Dokumen Resep (|D|)
            for (weight in ingredientsVector.values) {
                recipeMagnitudeSquared += weight * weight
            }
            val recipeMagnitude = sqrt(recipeMagnitudeSquared)
            if (recipeMagnitude == 0f) continue

            // Menghitung Perkalian Titik (Dot Product) Vektor pada komponen irisan
            for ((ingredient, userWeight) in userVector) {
                val recipeWeight = ingredientsVector[ingredient] ?: 0f
                if (recipeWeight > 0f) {
                    dotProduct += userWeight * recipeWeight
                }
            }

            // Rumus: Cosine Similarity = (Q . D) / (|Q| * |D|)
            val cosineSimilarityScore = if (dotProduct > 0f) {
                dotProduct / (userMagnitude * recipeMagnitude)
            } else {
                0f
            }

            // Hanya kumpulkan resep dengan nilai kecocokan di atas 0%
            if (cosineSimilarityScore > 0f) {
                results.add(
                    RecommendationResult(
                        // Konversi String ID ke Int saat parsing data bersih ke UI model
                        recipeId = recipeId.toIntOrNull() ?: 0,
                        title = currentTitles[recipeId] ?: "Resep #$recipeId",
                        similarityScore = cosineSimilarityScore
                    )
                )
            }
        }

        // --- 3. PROSES PERANGKINGAN (DESCENDING SORTING) ---
        return@withContext results.sortedByDescending { it.similarityScore }
    }
}