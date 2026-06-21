package com.example.movielibrary.data.remote.model

data class MovieSearchResponse(
    val score: Double?,
    val show: ShowDto
)

data class ShowDto(
    val id: Int,
    val name: String,
    val language: String?,
    val genres: List<String>?,
    val runtime: Int?,
    val averageRuntime: Int?,
    val premiered: String?,
    val summary: String?,
    val rating: RatingDto?,
    val image: ImageDto?,
    val _embedded: EmbeddedDto? = null
)

data class EmbeddedDto(
    val seasons: List<SeasonDto>? = null
)

data class RatingDto(
    val average: Double?
)

data class ImageDto(
    val medium: String?,
    val original: String?
)