package com.ayaansaksham.spend_aware.viewmodel

import android.util.Base64
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayaansaksham.spend_aware.data.local.entity.User
import com.ayaansaksham.spend_aware.data.repository.UserPreferencesRepository
import com.ayaansaksham.spend_aware.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, 100000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    private fun verifyPassword(password: String, saltBase64: String, hashBase64: String): Boolean {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val computedHash = hashPassword(password, salt)
        return MessageDigest.isEqual(computedHash.toByteArray(), hashBase64.toByteArray())
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun signUp(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val existingUser = userRepository.getUserByEmail(email.trim().lowercase())
            if (existingUser != null) {
                _authState.value = AuthState.Error("Email is already registered.")
                return@launch
            }

            val saltBytes = generateSalt()
            val saltBase64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
            val passwordHash = hashPassword(password, saltBytes)

            val newUser = User(
                fullName = fullName.trim(),
                email = email.trim().lowercase(),
                passwordHash = passwordHash,
                salt = saltBase64
            )

            try {
                val userId = userRepository.insertUser(newUser).toInt()
                userPreferencesRepository.saveLoginSession(userId, newUser.fullName)
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to create account.")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val user = userRepository.getUserByEmail(email.trim().lowercase())
            if (user == null) {
                _authState.value = AuthState.Error("Invalid email or password.")
                return@launch
            }

            val isPasswordCorrect = verifyPassword(password, user.salt, user.passwordHash)
            if (!isPasswordCorrect) {
                _authState.value = AuthState.Error("Invalid email or password.")
                return@launch
            }

            userPreferencesRepository.saveLoginSession(user.id, user.fullName)
            _authState.value = AuthState.Success
        }
    }
}

class AuthViewModelFactory(
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(userRepository, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}