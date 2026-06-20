package com.example.movielibrary.data.remote.model

data class CastResponse(
    val person: PersonDto,
    val character: CharacterDto
)

data class PersonDto(
    val id: Int,
    val name: String,
    val image: ImageDto?
)

data class CharacterDto(
    val id: Int,
    val name: String,
    val image: ImageDto?
)