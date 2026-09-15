package com.ayaansaksham.spend_aware.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val budgetId: Int = 0,
    val categoryId: Int? = null,
    val limitAmount: Double,
    val periodType: String = "MONTHLY",
    val startDateMillis: Long,
    val isFixed: Boolean = false,
    val profileId: Int = 1
)