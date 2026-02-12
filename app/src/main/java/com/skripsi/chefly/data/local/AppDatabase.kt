package com.skripsi.chefly.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.skripsi.chefly.data.local.entity.RecipeEntity

@Database(entities = [RecipeEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chefly_database"
                )
                    .createFromAsset("database/recipes.db") // Path file .db kamu
                    .fallbackToDestructiveMigration(true) // ✨ TAMBAHKAN BARIS INI ✨
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}