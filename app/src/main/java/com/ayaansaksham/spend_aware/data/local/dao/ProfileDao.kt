package com.ayaansaksham.spend_aware.data.local.dao

import androidx.room.*
import com.ayaansaksham.spend_aware.data.local.entity.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    @Update
    suspend fun updateProfile(profile: Profile)

    @Delete
    suspend fun deleteProfile(profile: Profile)

    @Query("SELECT * FROM profiles ORDER BY profileId ASC")
    fun getAllProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE profileId = :profileId LIMIT 1")
    fun getProfileById(profileId: Int): Flow<Profile?>
}