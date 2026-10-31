package com.ayaansaksham.spend_aware.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_setup_prefs")

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    
    private val USER_NAME_KEY = stringPreferencesKey("user_name")
    private val USER_PERSONA_KEY = stringPreferencesKey("user_persona")
    private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
    private val USER_ID_KEY = intPreferencesKey("user_id")
    private val ACTIVE_PROFILE_ID_KEY = intPreferencesKey("active_profile_id")
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

    val userNameFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY]
    }

    val userPersonaFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[USER_PERSONA_KEY] ?: "Student"
    }

    val isLoggedInFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN_KEY] ?: false
    }

    val userIdFlow: Flow<Int?> = dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }

    val activeProfileIdFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[ACTIVE_PROFILE_ID_KEY] ?: 1
    }

    val themeModeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY] ?: "System"
    }

    suspend fun saveUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
        }
    }

    suspend fun saveUserPersona(persona: String) {
        dataStore.edit { preferences ->
            preferences[USER_PERSONA_KEY] = persona
        }
    }

    suspend fun saveThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }

    suspend fun saveLoginSession(userId: Int, fullName: String, activeProfileId: Int) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN_KEY] = true
            preferences[USER_ID_KEY] = userId
            preferences[USER_NAME_KEY] = fullName
            preferences[ACTIVE_PROFILE_ID_KEY] = activeProfileId
        }
    }

    suspend fun updateActiveProfileId(activeProfileId: Int) {
        dataStore.edit { preferences ->
            preferences[ACTIVE_PROFILE_ID_KEY] = activeProfileId
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(IS_LOGGED_IN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USER_NAME_KEY)
            preferences.remove(ACTIVE_PROFILE_ID_KEY)
        }
    }
}