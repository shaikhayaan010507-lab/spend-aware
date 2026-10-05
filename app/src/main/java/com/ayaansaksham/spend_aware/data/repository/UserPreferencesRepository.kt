package com.ayaansaksham.spend_aware.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_setup_prefs")

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    
    private val USER_NAME_KEY = stringPreferencesKey("user_name")
    private val USER_PERSONA_KEY = stringPreferencesKey("user_persona")

    val userNameFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY]
    }

    val userPersonaFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[USER_PERSONA_KEY] ?: "Student"
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
}