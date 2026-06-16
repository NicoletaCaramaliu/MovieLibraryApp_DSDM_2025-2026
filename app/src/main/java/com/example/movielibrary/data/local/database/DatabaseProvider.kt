package com.example.movielibrary.data.local.database

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    private var INSTANCE: AppDatabase? = null

    fun getDatabase(
        context: Context
    ): AppDatabase {

        return INSTANCE ?: synchronized(this) {

            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "movie_library_db"
            )
                .fallbackToDestructiveMigration()
                .build()

            INSTANCE = instance

            instance
        }
    }
}