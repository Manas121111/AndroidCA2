package com.smarttour360.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val userKey: String,
    val name: String,
    val email: String,
    val mobile: String,
    val country: String,
    val homeCity: String,
    val budget: String,
    val tripTypes: String,
    val preferredTransport: String,
    val ecoPriority: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)
