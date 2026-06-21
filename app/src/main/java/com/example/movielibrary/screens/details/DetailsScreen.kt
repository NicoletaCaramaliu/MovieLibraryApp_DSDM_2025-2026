package com.example.movielibrary.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.movielibrary.data.remote.api.RetrofitClient
import com.example.movielibrary.data.remote.model.ShowDto
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import com.example.movielibrary.data.remote.model.CastResponse
import com.example.movielibrary.data.remote.model.SeasonDto
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.movielibrary.data.local.database.DatabaseProvider
import com.example.movielibrary.data.local.entity.MovieEntity
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(
    movieId: Int,
    onBackClick: () -> Unit
) {
    var movie by remember { mutableStateOf<ShowDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cast by remember { mutableStateOf<List<CastResponse>>(emptyList()) }
    var seasons by remember { mutableStateOf<List<SeasonDto>>(emptyList()) }
    var isFavorite by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val movieDao = DatabaseProvider.getDatabase(context).movieDao()

    LaunchedEffect(movieId) {
        try {
            isLoading = true
            
            // Check local first for favorite status
            val localMovie = movieDao.getMovieById(movieId)
            isFavorite = localMovie?.isFavorite ?: false

            movie = RetrofitClient.api.getMovieDetails(movieId)
            cast = RetrofitClient.api.getMovieCast(movieId)
            seasons = RetrofitClient.api.getMovieSeasons(movieId)
            
            // If movie is successfully fetched from API, we might want to ensure it's in DB
            // but for now let's just use what we have.
            
            errorMessage = null
        } catch (e: Exception) {
            // If API fails, try to load from local DB
            val localMovie = movieDao.getMovieById(movieId)
            if (localMovie != null) {
                // Map localMovie to ShowDto if possible, but ShowDto is complex.
                // For simplicity, if offline, we just show error if not in DB.
                // Or we could have a better mapping.
                // Given the current structure, let's keep it simple.
                errorMessage = "Offline. Could not load latest details."
            } else {
                errorMessage = "Could not load movie details."
            }
        } finally {
            isLoading = false
        }
    }

    fun toggleFavorite() {
        scope.launch {
            val newFavoriteStatus = !isFavorite
            isFavorite = newFavoriteStatus
            
            val currentMovie = movie
            if (currentMovie != null) {
                val existingInDb = movieDao.getMovieById(movieId)
                if (existingInDb != null) {
                    movieDao.updateFavoriteStatus(movieId, newFavoriteStatus)
                } else {
                    // If not in DB, insert it first
                    val entity = MovieEntity(
                        id = currentMovie.id,
                        name = currentMovie.name,
                        language = currentMovie.language,
                        genres = currentMovie.genres?.joinToString(", "),
                        runtime = currentMovie.runtime,
                        rating = currentMovie.rating?.average,
                        imageUrl = currentMovie.image?.medium,
                        summary = currentMovie.summary,
                        isFavorite = newFavoriteStatus
                    )
                    movieDao.insertMovies(listOf(entity))
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBackClick) {
                Text("Back")
            }
            
            if (movie != null) {
                IconButton(onClick = { toggleFavorite() }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) Color.Red else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> {
                CircularProgressIndicator()
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error
                )
            }

            movie != null -> {
                val currentMovie = movie!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    AsyncImage(
                        model = currentMovie.image?.original ?: currentMovie.image?.medium,
                        contentDescription = currentMovie.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = currentMovie.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Language: ${currentMovie.language ?: "Unknown"}")
                    Text("Rating: ${currentMovie.rating?.average ?: "N/A"}")
                    Text("Runtime: ${currentMovie.runtime ?: 0} min")
                    Text("Premiered: ${currentMovie.premiered ?: "Unknown"}")
                    Text("Genres: ${currentMovie.genres?.joinToString(", ") ?: "N/A"}")

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = cleanHtml(currentMovie.summary ?: "No summary available.")
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Cast",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (cast.isEmpty()) {
                        Text("No cast information available.")
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(cast.take(10)) { castItem ->
                                CastCard(castItem)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Seasons",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (seasons.isEmpty()) {
                        Text("No seasons available.")
                    } else {
                        seasons.forEach { season ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = "Season ${season.number ?: "-"}",
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text("Episodes: ${season.episodeOrder ?: 0}")

                                    Text("Premiere: ${season.premiereDate ?: "Unknown"}")

                                    Text("End date: ${season.endDate ?: "Unknown"}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun cleanHtml(text: String): String {
    return text
        .replace("<p>", "")
        .replace("</p>", "")
        .replace("<b>", "")
        .replace("</b>", "")
        .replace("<i>", "")
        .replace("</i>", "")
        .replace("<br>", "\n")
        .replace("<br/>", "\n")
}

@Composable
fun CastCard(
    castItem: CastResponse
) {
    Card(
        modifier = Modifier.width(150.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = castItem.person.image?.medium,
                contentDescription = castItem.person.name,
                modifier = Modifier
                    .size(width = 120.dp, height = 150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = castItem.person.name,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "as ${castItem.character.name}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}