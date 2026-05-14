package com.skripsi.chefly.di

import android.content.Context
import com.skripsi.chefly.data.local.AppDatabase
import com.skripsi.chefly.data.local.RecipeDao
import com.skripsi.chefly.data.repository.RecipeRepository
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
    fun provideRecipeDao(database: AppDatabase): RecipeDao {
        return database.recipeDao()
    }

    @Provides
    @Singleton
    fun provideRecipeRepository(): RecipeRepository {
        return com.skripsi.chefly.data.repository.RecipeRepository()
    }
}