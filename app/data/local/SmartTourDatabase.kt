package com.smarttour360.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smarttour360.app.ui.chatbot.rag.ChatKnowledgeDao
import com.smarttour360.app.ui.chatbot.rag.ChatKnowledgeEntity

@Database(
    entities = [UserProfileEntity::class, ChatKnowledgeEntity::class],
    version = 3,
    exportSchema = false
)
abstract class SmartTourDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun chatKnowledgeDao(): ChatKnowledgeDao

    companion object {
        @Volatile
        private var instance: SmartTourDatabase? = null

        fun getInstance(context: Context): SmartTourDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SmartTourDatabase::class.java,
                    "smarttour360.db"
                )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
