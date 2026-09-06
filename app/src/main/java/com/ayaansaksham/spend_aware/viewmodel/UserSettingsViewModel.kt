package com.ayaansaksham.spend_aware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayaansaksham.spend_aware.data.local.entity.UserSettings
import com.ayaansaksham.spend_aware.data.repository.UserSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserSettingsViewModel(private val repository: UserSettingsRepository) : ViewModel() {

    val settings: StateFlow<UserSettings?> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun saveSettings(settings: UserSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
        }
    }
}

class UserSettingsViewModelFactory(private val repository: UserSettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserSettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}