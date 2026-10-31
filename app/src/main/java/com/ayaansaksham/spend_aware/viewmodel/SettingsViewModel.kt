package com.ayaansaksham.spend_aware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayaansaksham.spend_aware.data.local.dao.ProfileDao
import com.ayaansaksham.spend_aware.data.repository.UserPreferencesRepository
import com.ayaansaksham.spend_aware.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userRepository: UserRepository,
    private val profileDao: ProfileDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val userPersona: StateFlow<String> = userPreferencesRepository.userPersonaFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Student"
        )

    val themeMode: StateFlow<String> = userPreferencesRepository.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "System"
        )

    fun updatePersona(newPersona: String) {
        viewModelScope.launch {
            val userId = userPreferencesRepository.userIdFlow.first() ?: 1
            val targetRole = if (newPersona == "Student") "STUDENT" else "EMPLOYEE"
            val targetProfile = profileDao.getProfileByRole(userId, targetRole)

            if (targetProfile != null) {
                userPreferencesRepository.saveUserPersona(newPersona)
                userPreferencesRepository.updateActiveProfileId(targetProfile.profileId)
                userRepository.updateActiveProfileId(userId, targetProfile.profileId)
            }
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveThemeMode(mode)
        }
    }
}

class SettingsViewModelFactory(
    private val userRepository: UserRepository,
    private val profileDao: ProfileDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(userRepository, profileDao, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}