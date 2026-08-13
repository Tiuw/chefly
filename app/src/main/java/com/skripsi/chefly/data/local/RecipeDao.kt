package com.skripsi.chefly.data.local

import androidx.room.*
import com.skripsi.chefly.data.local.entity.IdfDictionaryEntity
import com.skripsi.chefly.data.local.entity.RecipeEntity
import com.skripsi.chefly.data.local.entity.TfidfDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    // --- Query untuk Tabel recipes ---

    @Query("SELECT * FROM recipes ORDER BY id DESC")
    fun getAll(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes ORDER BY loves DESC, id DESC")
    suspend fun getAllOnceList(): List<RecipeEntity>

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun getAllOnceCount(): Int

    @Query("SELECT * FROM recipes WHERE LOWER(category) LIKE '%' || LOWER(:category) || '%' ORDER BY loves DESC, id DESC LIMIT 50")
    suspend fun searchByCategoryLimited(category: String): List<RecipeEntity>

    @Query("""
        SELECT * FROM recipes 
        WHERE LOWER(category) LIKE '%' || LOWER(:category) || '%' 
        ORDER BY loves DESC, id DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getRecipesByCategoryPaginated(
        category: String,
        limit: Int,
        offset: Int
    ): List<RecipeEntity>

    @Query(
        """
        SELECT * FROM recipes
        WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(category) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(ui_ingredients) LIKE '%' || LOWER(:query) || '%'
        ORDER BY loves DESC, id DESC
        LIMIT 100
        """
    )
    suspend fun searchByKeyword(query: String): List<RecipeEntity>

    /**
     * Digunakan oleh Repository untuk pencarian kombinasi kata kunci dan kategori
     * saat query pengguna hanya mengandung satu kata.
     */
    @Query(
        """
        SELECT * FROM recipes
        WHERE (LOWER(title) LIKE '%' || LOWER(:query) || '%' OR LOWER(ui_ingredients) LIKE '%' || LOWER(:query) || '%')
          AND LOWER(category) LIKE '%' || LOWER(:category) || '%'
        ORDER BY loves DESC, id DESC
        LIMIT 100
        """
    )
    suspend fun searchByKeywordAndCategory(query: String, category: String): List<RecipeEntity>

    @Query(
        """
        SELECT * FROM recipes
        WHERE LOWER(category) LIKE '%' || LOWER(:category) || '%'
        ORDER BY loves DESC, id DESC
        """
    )
    suspend fun searchByCategory(category: String): List<RecipeEntity>

    @Query("SELECT * FROM recipes ORDER BY loves DESC LIMIT :limit")
    suspend fun getRecommendedRecipes(limit: Int): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecipeEntity?

    @Query("SELECT DISTINCT category FROM recipes WHERE category IS NOT NULL AND category != '' ORDER BY category ASC")
    suspend fun getUniqueCategories(): List<String>

    @Query("""
    SELECT * FROM recipes 
    WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%' 
    AND LOWER(category) LIKE '%' || LOWER(:category) || '%'
    ORDER BY loves DESC, id DESC 
    LIMIT :limit OFFSET :offset
""")
    suspend fun searchByTitleAndCategoryPaginated(
        query: String,
        category: String,
        limit: Int,
        offset: Int
    ): List<RecipeEntity>


    // --- Query untuk Tabel tfidf_data ---

    @Query("SELECT * FROM tfidf_data")
    suspend fun getAllTfidfData(): List<TfidfDataEntity>

    @Query("SELECT * FROM tfidf_data WHERE recipe_id = :id LIMIT 1")
    suspend fun getTfidfByRecipeId(id: String): TfidfDataEntity?


    // --- Query untuk Tabel idf_dictionary ---

    @Query("SELECT * FROM idf_dictionary")
    suspend fun getFullDictionary(): List<IdfDictionaryEntity>

    @Query("SELECT idf_weight FROM idf_dictionary WHERE ingredient = :ingredient LIMIT 1")
    suspend fun getIdfWeightByIngredient(ingredient: String): Double?

    @Query("SELECT * FROM idf_dictionary WHERE ingredient IN (:ingredients)")
    suspend fun getIdfWeightsForIngredients(ingredients: List<String>): List<IdfDictionaryEntity>


    // --- Operasi CUD (Create, Update, Delete) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: RecipeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipes: List<RecipeEntity>)

    @Update
    suspend fun update(recipe: RecipeEntity)

    @Delete
    suspend fun delete(recipe: RecipeEntity)

    @Query("DELETE FROM recipes")
    suspend fun clearAll()
}