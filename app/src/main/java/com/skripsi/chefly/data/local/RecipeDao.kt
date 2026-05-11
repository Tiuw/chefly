package com.skripsi.chefly.data.local

import androidx.room.*
import com.skripsi.chefly.data.local.entity.RecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY id DESC")
    fun getAll(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes ORDER BY loves DESC, id DESC")
    suspend fun getAllOnceList(): List<RecipeEntity>

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun getAllOnceCount(): Int

    @Query("SELECT * FROM recipes ORDER BY loves DESC, id DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecipesPaginated(limit: Int, offset: Int): List<RecipeEntity>

    @Query(
        """
        SELECT * FROM recipes
        WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(category) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(ui_ingredients) LIKE '%' || LOWER(:query) || '%'
        ORDER BY loves DESC, id DESC
        """
    )
    suspend fun searchByKeyword(query: String): List<RecipeEntity>

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


