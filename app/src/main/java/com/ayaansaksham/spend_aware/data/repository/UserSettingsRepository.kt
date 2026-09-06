package com.ayaansaksham.spend_aware.data.repository

import com.ayaansaksham.spend_aware.data.local.dao.UserSettingsDao
import com.ayaansaksham.spend_aware.data.local.entity.UserSettings
import kotlinx.coroutines.flow.Flow

class UserSettingsRepository(private val userSettingsDao: UserSettingsDao) {

    val settings: Flow<UserSettings?> = userSettingsDao.getSettings()

    suspend fun saveSettings(settings: UserSettings) {
        userSettingsDao.insertOrUpdateSettings(settings)
    }
}