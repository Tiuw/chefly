package com.skripsi.chefly.ui.viewmodel

import com.skripsi.chefly.util.SettingRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: SettingRepository
) : ViewModel() {

    private val _isOnboardingCompleted = MutableStateFlow<Boolean?>(null)
    val isOnboardingCompleted: StateFlow<Boolean?> = _isOnboardingCompleted.asStateFlow()

    init {
        viewModelScope.launch {
            repository.isOnboardingCompleted.collect { completed ->
                _isOnboardingCompleted.value = completed
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.saveOnboardingStatus(true)
        }
    }
}