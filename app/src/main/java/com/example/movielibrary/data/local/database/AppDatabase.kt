package com.example.movielibrary.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.movielibrary.data.local.dao.UserDao
import com.example.movielibrary.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
}