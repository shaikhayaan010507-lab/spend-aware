package com.ayaansaksham.spend_aware.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val expenseId: Int = 0,
    val amount: Double,
    val categoryId: Int?,
    val note: String? = null,
    val dateMillis: Long,
    val isRecurring: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)