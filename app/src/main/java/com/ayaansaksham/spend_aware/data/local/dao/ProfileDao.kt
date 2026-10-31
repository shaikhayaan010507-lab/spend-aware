package com.ayaansaksham.spend_aware.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ayaansaksham.spend_aware.data.local.entity.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    @Query("SELECT * FROM profiles WHERE userId = :userId ORDER BY profileId ASC")
    fun getProfilesForUser(userId: Int): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE userId = :userId AND role = :role LIMIT 1")
    suspend fun getProfileByRole(userId: Int, role: String): Profile?

    @Query("SELECT * FROM profiles WHERE profileId = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: Int): Profile?
}