package com.skripsi.chefly.data.local

import androidx.room.*
import com.skripsi.chefly.data.local.entity.IdfDictionaryEntity
import com.skripsi.chefly.data.local.entity.RecipeEntity
import com.skripsi.chefly.data.local.entity.TfidfDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
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

    @Query("""
    SELECT * FROM recipes 
    WHERE (
        LOWER(title) LIKE '%' || LOWER(:query) || '%' 
        OR REPLACE(LOWER(ui_ingredients), '_', ' ') LIKE '%' || LOWER(:query) || '%'
    )
    AND (:category = 'Semua' OR LOWER(category) LIKE '%' || LOWER(:category) || '%')
    AND (:method = 'Semua' OR LOWER(primary_cooking_method) = LOWER(:method))
    ORDER BY loves DESC LIMIT 100
""")
    suspend fun searchByKeywordCategoryAndMethod(
        query: String,
        category: String,
        method: String
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

    @Query("""
    SELECT * FROM recipes 
    WHERE (:category = 'Semua' OR LOWER(category) LIKE '%' || LOWER(:category) || '%') 
    AND (:method = 'Semua' OR LOWER(primary_cooking_method) = LOWER(:method))
    ORDER BY loves DESC, id DESC 
    LIMIT :limit OFFSET :offset
""")
    suspend fun getRecipesWithFilters(
        category: String,
        method: String,
        limit: Int,
        offset: Int
    ): List<RecipeEntity>

    @Query("""
    SELECT * FROM recipes 
    WHERE (LOWER(title) LIKE '%' || LOWER(:query) || '%' 
       OR LOWER(ui_ingredients) LIKE '%' || LOWER(:query) || '%' 
       OR LOWER(category) LIKE '%' || LOWER(:query) || '%')
    AND (:method = 'Semua' OR LOWER(primary_cooking_method) = LOWER(:method))
    ORDER BY loves DESC LIMIT 100
""")
    suspend fun searchByKeywordAndMethod(query: String, method: String): List<RecipeEntity>

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

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecipeEntity?

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


