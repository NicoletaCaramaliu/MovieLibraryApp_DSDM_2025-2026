package com.example.movielibrary.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val language: String?,
    val genres: String?,
    val runtime: Int?,
    val rating: Double?,
    val imageUrl: String?,
    val summary: String?
)