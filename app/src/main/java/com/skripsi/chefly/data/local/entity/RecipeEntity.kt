package com.skripsi.chefly.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.skripsi.chefly.data.Recipe

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val name: String?,

    @ColumnInfo(name = "image_url")
    val imageUrl: String?,

    @ColumnInfo(name = "ui_ingredients")
    val rawIngredients: String?,

    @ColumnInfo(name = "ui_steps")
    val instructions: String?,

    @ColumnInfo(name = "category")
    val category: String?,

    @ColumnInfo(name = "total_ingredients")
    val totalIngredients: Int? = null,

    @ColumnInfo(name = "total_steps")
    val totalSteps: Int? = null,

    @ColumnInfo(name = "loves")
    val loves: Int? = null
) {
    // Gunakan @Ignore untuk variabel yang tidak ada di .db
    @Ignore var ingredientsCleaned: String? = null
    @Ignore var titleCleaned: String? = null

    fun toRecipe(): Recipe = Recipe(
        name = name ?: "",
        rawIngredients = rawIngredients ?: "",
        instructions = instructions ?: "",
        imageUrl = imageUrl ?: "",
        category = category ?: "",
        ingredientsCleaned = ingredientsCleaned,
        totalIngredients = totalIngredients,
        loves = loves,
        titleCleaned = titleCleaned,
        totalSteps = totalSteps,
        prepTime = null,
        cookTime = null,
        servings = null,
        id = id
    )

    companion object {
        fun fromRecipe(r: Recipe): RecipeEntity {
            val entity = RecipeEntity(
                id = r.id ?: "",
                name = r.name,
                imageUrl = r.imageUrl,
                rawIngredients = r.rawIngredients,
                instructions = r.instructions,
                category = r.category,
                totalIngredients = r.totalIngredients,
                totalSteps = r.totalSteps,
                loves = r.loves
            )
            // Isi kembali nilai-nilai yang di-ignore agar datanya tidak hilang
            entity.ingredientsCleaned = r.ingredientsCleaned
            entity.titleCleaned = r.titleCleaned

            return entity
        }
    }
}