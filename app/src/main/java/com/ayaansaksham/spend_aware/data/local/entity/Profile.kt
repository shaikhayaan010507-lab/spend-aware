package com.ayaansaksham.spend_aware.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "profiles",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val profileId: Int = 0,
    val name: String,
    val role: String, // e.g., "STUDENT" or "EMPLOYEE"
    val userId: Int = 1
)