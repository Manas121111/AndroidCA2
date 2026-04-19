package com.smarttour360.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE userKey = :userKey LIMIT 1")
    fun getProfile(userKey: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(profile: UserProfileEntity)
}
