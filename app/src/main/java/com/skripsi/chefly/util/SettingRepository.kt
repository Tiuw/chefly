package com.skripsi.chefly.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chefly_settings")

@Singleton
class SettingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val isOnboardingCompletedKey = booleanPreferencesKey("is_onboarding_completed")

    // Mengambil status apakah onboarding sudah selesai (default: false)
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[isOnboardingCompletedKey] ?: false
        }

    // Mengubah status setelah pengguna menyelesaikan onboarding
    suspend fun saveOnboardingStatus(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[isOnboardingCompletedKey] = completed
        }
    }
}