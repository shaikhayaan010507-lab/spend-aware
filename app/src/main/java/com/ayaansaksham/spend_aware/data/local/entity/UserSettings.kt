package com.ayaansaksham.spend_aware.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 0,
    val firebaseUid: String,
    val isBiometricLockEnabled: Boolean = false,
    val pinHash: String? = null,
    val monthlyIncome: Double? = null,
    val currencySymbol: String = "₹"
)