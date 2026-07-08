package com.skripsi.chefly.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skripsi.chefly.data.local.entity.RecipeEntity
import com.skripsi.chefly.data.local.entity.TfidfDataEntity
import com.skripsi.chefly.data.local.entity.IdfDictionaryEntity

@Database(
    entities = [
        RecipeEntity::class,
        TfidfDataEntity::class,
        IdfDictionaryEntity::class
    ],
    version = 5,
    exportSchema = false
)
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
                    "chefly_production.db"
                )
                    .createFromAsset("database/recipesv2.db")
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            Log.d("SQL_DEBUG", "Database berhasil dibuka. Mengecek tabel...")

                            val tables = listOf("recipes", "tfidf_data", "idf_dictionary")
                            tables.forEach { tableName ->
                                val cursor = db.query("PRAGMA table_info($tableName)")

                                // Gunakan use {} agar cursor otomatis tertutup (mencegah memory leak)
                                cursor.use { c ->
                                    if (c.count > 0) {
                                        Log.d("SQL_DEBUG", "--- Struktur Tabel: $tableName ---")

                                        // Ambil index kolom satu kali saja di luar loop
                                        val nameIdx = c.getColumnIndex("name")
                                        val typeIdx = c.getColumnIndex("type")

                                        while (c.moveToNext()) {
                                            // Validasi apakah index ditemukan
                                            if (nameIdx != -1 && typeIdx != -1) {
                                                val name = c.getString(nameIdx)
                                                val type = c.getString(typeIdx)
                                                Log.d("SQL_DEBUG", "Kolom: $name | Tipe: $type")
                                            }
                                        }
                                    } else {
                                        Log.e("SQL_DEBUG", "Tabel $tableName TIDAK DITEMUKAN!")
                                    }
                                }
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