package com.example.movielibrary.data.remote.model

data class SeasonDto(
    val id: Int,
    val number: Int?,
    val episodeOrder: Int?,
    val premiereDate: String?,
    val endDate: String?
)