package com.example.movielibrary.data.remote.api

import com.example.movielibrary.data.remote.model.MovieSearchResponse
import com.example.movielibrary.data.remote.model.ShowDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("search/shows")
    suspend fun searchMovies(
        @Query("q") query: String = "batman"
    ): List<MovieSearchResponse>

    @GET("shows/{id}")
    suspend fun getMovieDetails(
        @Path("id") id: Int
    ): ShowDto
}