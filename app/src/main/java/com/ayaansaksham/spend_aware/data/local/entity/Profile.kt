package com.ayaansaksham.spend_aware.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val profileId: Int = 0,
    val name: String,
    val role: String // e.g., "STUDENT" or "EMPLOYEE"
)