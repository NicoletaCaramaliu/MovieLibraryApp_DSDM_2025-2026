package com.example.movielibrary.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.movielibrary.data.local.database.DatabaseProvider
import com.example.movielibrary.data.local.entity.MovieEntity
import com.example.movielibrary.data.remote.api.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val movieDao = DatabaseProvider.getDatabase(context).movieDao()

    var movies by remember { mutableStateOf<List<MovieEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true

                val response = RetrofitClient.api.searchMovies("batman")

                val movieEntities = response.map { item ->
                    MovieEntity(
                        id = item.show.id,
                        name = item.show.name,
                        language = item.show.language,
                        genres = item.show.genres?.joinToString(", "),
                        runtime = item.show.runtime,
                        rating = item.show.rating?.average,
                        imageUrl = item.show.image?.medium,
                        summary = item.show.summary
                    )
                }

                movieDao.insertMovies(movieEntities)
                movies = movieDao.getAllMovies()
                errorMessage = null

            } catch (e: Exception) {
                movies = movieDao.getAllMovies()
                errorMessage = if (movies.isEmpty()) {
                    "Nu s-au putut încărca filmele. Verifică internetul."
                } else {
                    null
                }
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Movie Library",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Movies loaded from API and saved locally",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "Eroare necunoscută",
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(movies) { movie ->
                        MovieCard(movie = movie)
                    }
                }
            }
        }
    }
}

@Composable
fun MovieCard(movie: MovieEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            AsyncImage(
                model = movie.imageUrl,
                contentDescription = movie.name,
                modifier = Modifier
                    .size(width = 90.dp, height = 130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movie.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text("Language: ${movie.language ?: "Unknown"}")
                Text("Rating: ${movie.rating ?: "N/A"}")
                Text("Runtime: ${movie.runtime ?: 0} min")

                Text(
                    text = "Genres: ${movie.genres ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}