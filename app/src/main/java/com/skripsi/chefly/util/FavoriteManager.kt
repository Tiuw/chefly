package com.skripsi.chefly.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Inisialisasi DataStore
private val Context.dataStore by preferencesDataStore(name = "favorites_prefs")

class FavoriteManager(private val context: Context) {

    private val FAVORITES_KEY = stringSetPreferencesKey("favorite_recipe_ids")

    // Ambil daftar ID resep yang difavoritkan
    val favoriteIds: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[FAVORITES_KEY] ?: emptySet()
        }

    // Fungsi Tambah/Hapus (Toggle)
    suspend fun toggleFavorite(recipeId: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[FAVORITES_KEY] ?: emptySet()
            if (currentFavorites.contains(recipeId)) {
                // Jika sudah ada, hapus
                preferences[FAVORITES_KEY] = currentFavorites - recipeId
            } else {
                // Jika belum ada, tambah
                preferences[FAVORITES_KEY] = currentFavorites + recipeId
            }
        }
    }
}