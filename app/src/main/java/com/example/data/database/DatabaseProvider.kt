package com.example.data.database

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "ugc_video_studio_database"
            )
            .fallbackToDestructiveMigration(true)
            .build()
            INSTANCE = instance
            instance
        }
    }
}
