package com.example.movielibrary.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.movielibrary.data.local.dao.MovieDao
import com.example.movielibrary.data.local.dao.UserDao
import com.example.movielibrary.data.local.entity.MovieEntity
import com.example.movielibrary.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        MovieEntity::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun movieDao(): MovieDao
}