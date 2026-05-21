package com.skripsi.chefly.di

import android.content.Context
import com.skripsi.chefly.data.local.AppDatabase
import com.skripsi.chefly.data.local.RecipeDao
import com.skripsi.chefly.data.repository.RecipeRepository
import com.skripsi.chefly.ml.YOLO26Detector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton // 🟢 TAMBAHKAN SINGLETON: Agar Dao dikunci satu instance di memori untuk menghemat database connection pool
    fun provideRecipeDao(database: AppDatabase): RecipeDao {
        return database.recipeDao()
    }

    @Provides
    @Singleton
    fun provideRecipeRepository(
        recipeDao: RecipeDao // 🟢 REVISI UTAMA: Alirkan RecipeDao ke dalam repository lewat parameter Hilt, JANGAN kosongan!
    ): RecipeRepository {
        return RecipeRepository(recipeDao)
    }

    // 🟢 GABUNG DI SINI: Menyediakan instance Singleton YOLO26Detector untuk CameraViewModel
    @Provides
    @Singleton
    fun provideYOLO26Detector(
        @ApplicationContext context: Context
    ): YOLO26Detector {
        // Membaca file labels.txt dari folder assets secara otomatis saat app dibuka
        val labels = context.assets.open("labels.txt").bufferedReader().use { it.readText() }
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // Mengembalikan objek detector tunggal ke dalam Dependency Graph Hilt
        return YOLO26Detector(
            context = context,
            modelFilename = "yolo26n_float32.tflite",
            detectionClasses = labels,
            useNNAPI = false
        )
    }
}