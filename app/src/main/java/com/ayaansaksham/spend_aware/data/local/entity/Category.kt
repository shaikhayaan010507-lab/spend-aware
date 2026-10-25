package com.ayaansaksham.spend_aware.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "profileId"], unique = true)]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val categoryId: Int = 0,
    val name: String,
    val icon: String? = null,
    val colorHex: String = "#6750A4",
    val isDefault: Boolean = false,
    val profileId: Int = 1
)