
package com.skripsi.chefly.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skripsi.chefly.data.local.RecipeDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
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
    companion object {
        private const val TAG = "Chefly_IR_System"
        private const val MANUAL_TAG = "Chefly_Manual_Cosine"
    }

    private val gson = Gson()

    // Mencegah dua proses inisialisasi berjalan bersamaan.
    private val initializationMutex = Mutex()

    // Mencegah dua proses rekomendasi berjalan bersamaan.
    private val recommendationMutex = Mutex()

    @Volatile
    private var idfMap: Map<String, Float>? = null

    @Volatile
    private var recipeVectorsMap: Map<String, Map<String, Float>>? = null

    @Volatile
    private var recipeTitlesMap: Map<String, String>? = null

    // ========================================================
    // 1. INISIALISASI DATA REKOMENDASI
    // ========================================================

    suspend fun initializeRecommendationData() {
        withContext(Dispatchers.IO) {
            initializationMutex.withLock {
                if (
                    idfMap != null &&
                    recipeVectorsMap != null &&
                    recipeTitlesMap != null
                ) {
                    Log.d(TAG, "Data rekomendasi sudah tersedia.")
                    return@withLock
                }

                try {
                    Log.d(TAG, "Memulai inisialisasi data rekomendasi.")

                    val idfEntities = recipeDao.getFullDictionary()

                    val loadedIdf = idfEntities.associate {
                        it.ingredient to it.idfWeight.toFloat()
                    }

                    val tfidfEntities = recipeDao.getAllTfidfData()

                    val vectorType =
                        object : TypeToken<Map<String, Float>>() {}.type

                    val loadedVectors =
                        mutableMapOf<String, Map<String, Float>>()

                    tfidfEntities.forEach { entity ->
                        val vector: Map<String, Float> =
                            gson.fromJson(entity.vectorJson, vectorType)

                        loadedVectors[entity.recipeId.toString()] = vector
                    }

                    val recipes = recipeDao.getAllOnceList()

                    val loadedTitles = recipes.associate {
                        it.id.toString() to
                                (it.title ?: "Resep Tanpa Nama")
                    }

                    // Publikasikan setelah semua data berhasil dibaca.
                    idfMap = loadedIdf
                    recipeVectorsMap = loadedVectors.toMap()
                    recipeTitlesMap = loadedTitles

                    Log.d(
                        TAG,
                        "Inisialisasi berhasil: " +
                                "${loadedIdf.size} IDF, " +
                                "${loadedVectors.size} vektor resep, " +
                                "${loadedTitles.size} judul resep."
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal memuat data rekomendasi.", e)
                }
            }
        }
    }

    // ========================================================
    // 2. PROSES REKOMENDASI
    // ========================================================

    suspend fun getRecommendations(
        userIngredients: List<String>,
        topN: Int = 10,
        minMatchCount: Int = 2
    ): List<RecommendationResult> {

        return recommendationMutex.withLock {
            withContext(Dispatchers.Default) {

                Log.d(TAG, "========================================")
                Log.d(TAG, "Memulai proses rekomendasi.")
                Log.d(TAG, "Thread: ${Thread.currentThread().name}")
                Log.d(TAG, "Input: $userIngredients")

                // Pastikan data sudah tersedia.
                if (
                    idfMap == null ||
                    recipeVectorsMap == null ||
                    recipeTitlesMap == null
                ) {
                    initializeRecommendationData()
                }

                val currentIdf = idfMap
                    ?: return@withContext emptyList()

                val currentVectors = recipeVectorsMap
                    ?: return@withContext emptyList()

                val currentTitles = recipeTitlesMap
                    ?: return@withContext emptyList()

                // ------------------------------------------------
                // 3. NORMALISASI BAHAN PENGGUNA
                // ------------------------------------------------

                val cleanUserIngredients = userIngredients
                    .map {
                        it.trim()
                            .lowercase(Locale.ROOT)
                            .replace(" ", "_")
                    }
                    .filter { it.isNotBlank() }
                    .distinct()

                Log.d(
                    TAG,
                    "Bahan ternormalisasi: $cleanUserIngredients"
                )

                // ------------------------------------------------
                // 4. MEMBENTUK VEKTOR TF-IDF PENGGUNA (A)
                // ------------------------------------------------

                val userVector = mutableMapOf<String, Float>()

                cleanUserIngredients.forEach { ingredient ->
                    val weight = currentIdf[ingredient]

                    if (weight != null && weight > 0f) {
                        userVector[ingredient] = weight
                    } else {
                        Log.w(
                            TAG,
                            "Bahan '$ingredient' tidak ditemukan dalam IDF."
                        )
                    }
                }

                if (userVector.isEmpty()) {
                    Log.e(TAG, "Vektor pengguna kosong.")
                    return@withContext emptyList()
                }

                // ||A|| = sqrt(sum(A_i^2))
                val userMagnitude = sqrt(
                    userVector.values.sumOf { weight ->
                        weight.toDouble() * weight.toDouble()
                    }
                )

                if (userMagnitude == 0.0) {
                    Log.e(TAG, "Magnitude pengguna bernilai nol.")
                    return@withContext emptyList()
                }

                Log.d(TAG, "Vektor pengguna A = $userVector")
                Log.d(
                    TAG,
                    "Magnitude pengguna ||A|| = ${format(userMagnitude)}"
                )

                // ------------------------------------------------
                // 5. HITUNG COSINE SIMILARITY SETIAP RESEP
                // ------------------------------------------------

                val results = mutableListOf<RecommendationResult>()

                currentVectors.forEach { (recipeId, recipeVector) ->

                    val matchedIngredients = cleanUserIngredients.filter {
                        recipeVector.containsKey(it)
                    }

                    if (matchedIngredients.size < minMatchCount) {
                        return@forEach
                    }

                    // Dot product: sum(A_i * B_i)
                    val dotProduct = userVector.entries.sumOf { entry ->
                        val a = entry.value.toDouble()
                        val b =
                            recipeVector[entry.key]?.toDouble() ?: 0.0

                        a * b
                    }

                    // Magnitude resep: sqrt(sum(B_i^2))
                    val recipeMagnitude = sqrt(
                        recipeVector.values.sumOf { weight ->
                            weight.toDouble() * weight.toDouble()
                        }
                    )

                    if (recipeMagnitude == 0.0) {
                        return@forEach
                    }

                    // Cosine(A, B) = (A · B) / (||A|| * ||B||)
                    val cosineSimilarity =
                        dotProduct / (userMagnitude * recipeMagnitude)

                    if (!cosineSimilarity.isFinite()) {
                        return@forEach
                    }

                    val score = cosineSimilarity
                        .coerceIn(0.0, 1.0)
                        .toFloat()

                    if (score <= 0f) {
                        return@forEach
                    }

                    val numericId = recipeId.toIntOrNull()

                    if (numericId == null) {
                        Log.w(TAG, "ID resep tidak valid: $recipeId")
                        return@forEach
                    }

                    val title = currentTitles[recipeId]
                        ?: "Resep #$recipeId"

                    results.add(
                        RecommendationResult(
                            recipeId = numericId,
                            title = title,
                            similarityScore = score
                        )
                    )
                }

                // ------------------------------------------------
                // 6. URUTKAN BERDASARKAN SKOR PRESISI PENUH
                // ------------------------------------------------

                val sortedResults = results
                    .sortedByDescending { it.similarityScore }
                    .take(topN.coerceAtLeast(0))

                Log.d(TAG, "========================================")
                Log.d(TAG, "HASIL REKOMENDASI TOP $topN")
                Log.d(TAG, "Total kandidat lolos filter: ${results.size}")
                Log.d(TAG, "Jumlah hasil: ${sortedResults.size}")

                // ------------------------------------------------
                // 7. CETAK PERHITUNGAN MANUAL UNTUK TOP-N
                // ------------------------------------------------

                sortedResults.forEachIndexed { index, result ->
                    val recipeKey = result.recipeId.toString()
                    val recipeVector = currentVectors[recipeKey].orEmpty()

                    logManualCosineCalculation(
                        rank = index + 1,
                        recipeId = result.recipeId,
                        title = result.title,
                        userVector = userVector,
                        recipeVector = recipeVector,
                        similarityScore = result.similarityScore
                    )
                }

                Log.d(TAG, "Proses rekomendasi selesai.")
                Log.d(TAG, "========================================")

                sortedResults
            }
        }
    }

    // ========================================================
    // 8. PERHITUNGAN MANUAL COSINE SIMILARITY
    // ========================================================

    private fun logManualCosineCalculation(
        rank: Int,
        recipeId: Int,
        title: String,
        userVector: Map<String, Float>,
        recipeVector: Map<String, Float>,
        similarityScore: Float
    ) {
        // A. Dot product: sum(A_i * B_i)
        val dotProductTerms = userVector.map { (ingredient, aWeight) ->
            val bWeight = recipeVector[ingredient] ?: 0f

            Triple(
                ingredient,
                aWeight.toDouble(),
                bWeight.toDouble()
            )
        }

        val dotProduct = dotProductTerms.sumOf { (_, a, b) ->
            a * b
        }

        // B. Magnitude pengguna: sqrt(sum(A_i^2))
        val userSquaredTerms = userVector.map { (ingredient, weight) ->
            ingredient to weight.toDouble().let { it * it }
        }

        val userSquaredSum = userSquaredTerms.sumOf { it.second }
        val userMagnitude = sqrt(userSquaredSum)

        // C. Magnitude resep: sqrt(sum(B_i^2))
        // Seluruh komponen vektor resep digunakan.
        val recipeSquaredTerms = recipeVector.map { (ingredient, weight) ->
            ingredient to weight.toDouble().let { it * it }
        }

        val recipeSquaredSum = recipeSquaredTerms.sumOf { it.second }
        val recipeMagnitude = sqrt(recipeSquaredSum)

        // D. Cosine Similarity
        val denominator = userMagnitude * recipeMagnitude

        val manualCosine = if (denominator > 0.0) {
            dotProduct / denominator
        } else {
            0.0
        }

        // E. Susun seluruh rincian dalam SATU pesan Logcat.
        val manualLog = buildString {
            appendLine("========== RANK $rank ==========")
            appendLine("ID Resep: $recipeId")
            appendLine("Judul: $title")

            appendLine()
            appendLine("VEKTOR PENGGUNA A:")

            userVector.forEach { (ingredient, weight) ->
                appendLine(
                    "$ingredient = ${format(weight.toDouble())}"
                )
            }

            appendLine()
            appendLine("VEKTOR RESEP B:")

            recipeVector.forEach { (ingredient, weight) ->
                appendLine(
                    "$ingredient = ${format(weight.toDouble())}"
                )
            }

            appendLine()
            appendLine("DOT PRODUCT:")

            dotProductTerms.forEach { (ingredient, a, b) ->
                appendLine(
                    "$ingredient: ${format(a)} * ${format(b)} " +
                            "= ${format(a * b)}"
                )
            }

            appendLine(
                "A · B = " +
                        dotProductTerms.joinToString(" + ") {
                            format(it.third * it.second)
                        }
            )
            appendLine("A · B = ${format(dotProduct)}")

            appendLine()
            appendLine("MAGNITUDE PENGGUNA:")

            userSquaredTerms.forEach { (ingredient, squared) ->
                appendLine(
                    "$ingredient: ${format(sqrt(squared))}² " +
                            "= ${format(squared)}"
                )
            }

            appendLine(
                "Jumlah kuadrat A = " +
                        userSquaredTerms.joinToString(" + ") {
                            format(it.second)
                        }
            )
            appendLine("Jumlah kuadrat A = ${format(userSquaredSum)}")
            appendLine("||A|| = sqrt(${format(userSquaredSum)})")
            appendLine("||A|| = ${format(userMagnitude)}")

            appendLine()
            appendLine("MAGNITUDE RESEP:")

            recipeSquaredTerms.forEach { (ingredient, squared) ->
                appendLine(
                    "$ingredient: ${format(sqrt(squared))}² " +
                            "= ${format(squared)}"
                )
            }

            appendLine(
                "Jumlah kuadrat B = " +
                        recipeSquaredTerms.joinToString(" + ") {
                            format(it.second)
                        }
            )
            appendLine("Jumlah kuadrat B = ${format(recipeSquaredSum)}")
            appendLine("||B|| = sqrt(${format(recipeSquaredSum)})")
            appendLine("||B|| = ${format(recipeMagnitude)}")

            appendLine()
            appendLine("COSINE SIMILARITY:")
            appendLine("Cosine = (A · B) / (||A|| * ||B||)")
            appendLine(
                "Cosine = ${format(dotProduct)} / " +
                        "(${format(userMagnitude)} * " +
                        "${format(recipeMagnitude)})"
            )
            appendLine(
                "Cosine = ${format(dotProduct)} / " +
                        "${format(denominator)}"
            )
            appendLine("Cosine hasil manual = ${format(manualCosine)}")
            appendLine(
                "Cosine hasil rekomendasi = " +
                        format(similarityScore.toDouble())
            )
            appendLine("Skor persen = ${format(manualCosine * 100)}%")
            appendLine("==============================")
        }

        Log.d(MANUAL_TAG, manualLog)
    }

    // ========================================================
    // 9. FORMAT ANGKA
    // ========================================================

    private fun format(value: Double): String {
        return String.format(Locale.US, "%.6f", value)
    }
}
