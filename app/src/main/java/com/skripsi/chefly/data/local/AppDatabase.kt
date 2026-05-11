package com.skripsi.chefly.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skripsi.chefly.data.local.entity.RecipeEntity

@Database(entities = [RecipeEntity::class], version = 3, exportSchema = false) // Naikkan versi ke 3
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    // Di dalam class AppDatabase
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chefly_debug_log.db" // Gunakan nama baru lagi untuk reset total
                )
                    .createFromAsset("database/recipes.db")
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            Log.d("SQL_DEBUG", "Database berhasil dibuka. Mengecek tabel...")
                            val cursor = db.query("PRAGMA table_info(recipes)")
                            while (cursor.moveToNext()) {
                                val name = cursor.getString(cursor.getColumnIndex("name"))
                                val type = cursor.getString(cursor.getColumnIndex("type"))
                                val notNull = cursor.getInt(cursor.getColumnIndex("notnull"))
                                val pk = cursor.getInt(cursor.getColumnIndex("pk"))
                                Log.d("SQL_DEBUG", "Kolom: $name | Tipe: $type | NotNull: $notNull | PK: $pk")
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}