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
    // Di dalam FavoriteManager.kt bagian toggleFavorite
    suspend fun toggleFavorite(recipeId: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[FAVORITES_KEY] ?: emptySet()

            if (currentFavorites.contains(recipeId)) {
                preferences[FAVORITES_KEY] = currentFavorites - recipeId
            } else {
                // Gunakan LinkedHashSet agar urutan masuk (insertion order) terjaga
                // Kita taruh yang baru di posisi paling awal
                val newList = mutableListOf(recipeId)
                newList.addAll(currentFavorites)
                preferences[FAVORITES_KEY] = newList.toSet()
            }
        }
    }
}