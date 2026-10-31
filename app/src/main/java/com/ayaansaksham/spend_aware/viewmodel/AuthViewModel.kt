package com.ayaansaksham.spend_aware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayaansaksham.spend_aware.data.local.dao.BudgetDao
import com.ayaansaksham.spend_aware.data.local.dao.CategoryDao
import com.ayaansaksham.spend_aware.data.local.dao.ProfileDao
import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.local.entity.Category
import com.ayaansaksham.spend_aware.data.local.entity.Profile
import com.ayaansaksham.spend_aware.data.local.entity.User
import com.ayaansaksham.spend_aware.data.repository.UserPreferencesRepository
import com.ayaansaksham.spend_aware.data.repository.UserRepository
import com.ayaansaksham.spend_aware.domain.logic.PasswordHasher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Base64
import java.util.Calendar

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val userRepository: UserRepository,
    private val profileDao: ProfileDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun signUp(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val cleanEmail = email.trim().lowercase()
            val existingUser = userRepository.getUserByEmail(cleanEmail)
            
            // Check if existing user is NOT the unauthenticated pre-migration placeholder
            if (existingUser != null && existingUser.email != "default@spendaware.local") {
                _authState.value = AuthState.Error("Email is already registered.")
                return@launch
            }

            val saltBytes = PasswordHasher.generateSalt()
            val saltBase64 = Base64.getEncoder().encodeToString(saltBytes)
            val passwordHash = PasswordHasher.hashPassword(password, saltBytes)

            val placeholderUser = userRepository.getUserByEmail("default@spendaware.local")

            val userId: Int
            val studentProfileId: Int

            try {
                if (placeholderUser != null && cleanEmail != "default@spendaware.local") {
                    // Claim pre-migration placeholder user #1
                    userId = placeholderUser.id
                    val studentProfile = profileDao.getProfileByRole(userId, "STUDENT")
                    studentProfileId = studentProfile?.profileId ?: 1

                    // Update User #1 credentials
                    userRepository.insertUser(
                        placeholderUser.copy(
                            fullName = fullName.trim(),
                            email = cleanEmail,
                            passwordHash = passwordHash,
                            salt = saltBase64,
                            activeProfileId = studentProfileId
                        )
                    )
                } else {
                    // Create brand new isolated user
                    val newUser = User(
                        fullName = fullName.trim(),
                        email = cleanEmail,
                        passwordHash = passwordHash,
                        salt = saltBase64,
                        activeProfileId = 0
                    )
                    userId = userRepository.insertUser(newUser).toInt()

                    // Create isolated Profiles for this user
                    studentProfileId = profileDao.insertProfile(
                        Profile(name = fullName.trim(), role = "STUDENT", userId = userId)
                    ).toInt()
                    
                    val employeeProfileId = profileDao.insertProfile(
                        Profile(name = fullName.trim(), role = "EMPLOYEE", userId = userId)
                    ).toInt()

                    // Update active profile ID for the user
                    userRepository.updateActiveProfileId(userId, studentProfileId)

                    // Populate Student Categories
                    categoryDao.insertCategory(Category(name = "Canteen", colorHex = "#00685D", isDefault = true, profileId = studentProfileId))
                    categoryDao.insertCategory(Category(name = "Commute", colorHex = "#F89C00", isDefault = true, profileId = studentProfileId))
                    categoryDao.insertCategory(Category(name = "Stationery", colorHex = "#6D7A77", isDefault = true, profileId = studentProfileId))
                    categoryDao.insertCategory(Category(name = "Events", colorHex = "#875200", isDefault = true, profileId = studentProfileId))
                    categoryDao.insertCategory(Category(name = "Hostel Fees", colorHex = "#B02528", isDefault = true, profileId = studentProfileId))

                    // Populate Employee Categories
                    categoryDao.insertCategory(Category(name = "Groceries", colorHex = "#16A34A", isDefault = true, profileId = employeeProfileId))
                    categoryDao.insertCategory(Category(name = "Rent", colorHex = "#B02528", isDefault = true, profileId = employeeProfileId))
                    categoryDao.insertCategory(Category(name = "Fuel", colorHex = "#F89C00", isDefault = true, profileId = employeeProfileId))
                    categoryDao.insertCategory(Category(name = "Investments", colorHex = "#008376", isDefault = true, profileId = employeeProfileId))
                    categoryDao.insertCategory(Category(name = "Dining", colorHex = "#00685D", isDefault = true, profileId = employeeProfileId))

                    // Insert Baseline Budgets
                    val startOfMonth = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    budgetDao.insertBudget(Budget(limitAmount = 0.0, periodType = "MONTHLY", startDateMillis = startOfMonth, categoryId = null, profileId = studentProfileId))
                    budgetDao.insertBudget(Budget(limitAmount = 0.0, periodType = "MONTHLY", startDateMillis = startOfMonth, categoryId = null, profileId = employeeProfileId))
                }

                // Save session
                userPreferencesRepository.saveLoginSession(userId, fullName.trim(), studentProfileId)
                userPreferencesRepository.saveUserPersona("Student")
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to create account: ${e.localizedMessage}")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val user = userRepository.getUserByEmail(email.trim().lowercase())
            if (user == null || user.email == "default@spendaware.local") {
                _authState.value = AuthState.Error("Invalid email or password.")
                return@launch
            }

            val isPasswordCorrect = PasswordHasher.verifyPassword(password, user.salt, user.passwordHash)
            if (!isPasswordCorrect) {
                _authState.value = AuthState.Error("Invalid email or password.")
                return@launch
            }

            var activeProfileId = user.activeProfileId
            if (activeProfileId == 0) {
                val studentProfile = profileDao.getProfileByRole(user.id, "STUDENT")
                activeProfileId = studentProfile?.profileId ?: 1
            }

            // Determine persona for setting
            val activeProfile = profileDao.getProfileById(activeProfileId)
            val persona = if (activeProfile?.role == "EMPLOYEE") "Employee" else "Student"
            userPreferencesRepository.saveUserPersona(persona)

            userPreferencesRepository.saveLoginSession(user.id, user.fullName, activeProfileId)
            _authState.value = AuthState.Success
        }
    }
}

class AuthViewModelFactory(
    private val userRepository: UserRepository,
    private val profileDao: ProfileDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(
                userRepository,
                profileDao,
                categoryDao,
                budgetDao,
                userPreferencesRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}