package com.example.movielibrary.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.movielibrary.data.local.database.DatabaseProvider
import com.example.movielibrary.data.local.entity.MovieEntity
import com.example.movielibrary.data.remote.api.RetrofitClient
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    initialSearchQuery: String = "off campus",
    onMovieClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val movieDao = DatabaseProvider.getDatabase(context).movieDao()

    var searchQuery by remember { mutableStateOf(initialSearchQuery) }
    var movies by remember { mutableStateOf<List<MovieEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    fun loadMovies(query: String) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                if (showOnlyFavorites) {
                    movies = movieDao.getFavoriteMovies()
                } else {
                    val response = RetrofitClient.api.searchMovies(query)

                    val movieEntities = response.map { item ->
                        val existingMovie = movieDao.getMovieById(item.show.id)
                        MovieEntity(
                            id = item.show.id,
                            name = item.show.name,
                            language = item.show.language,
                            genres = item.show.genres?.joinToString(", "),
                            runtime = item.show.runtime,
                            rating = item.show.rating?.average,
                            imageUrl = item.show.image?.medium,
                            summary = item.show.summary,
                            isFavorite = existingMovie?.isFavorite ?: false
                        )
                    }

                    movieDao.insertMovies(movieEntities)
                    movies = movieEntities
                }

            } catch (e: Exception) {
                movies = if (showOnlyFavorites) {
                    movieDao.getFavoriteMovies()
                } else {
                    movieDao.getAllMovies()
                }
                errorMessage = if (movies.isEmpty()) {
                    "Could not load movies. Check internet connection."
                } else {
                    null
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleFavorite(movie: MovieEntity) {
        scope.launch {
            val newFavoriteStatus = !movie.isFavorite
            movieDao.updateFavoriteStatus(movie.id, newFavoriteStatus)
            
            // Update local state to reflect changes immediately
            movies = movies.map { 
                if (it.id == movie.id) it.copy(isFavorite = newFavoriteStatus) else it 
            }
            
            if (showOnlyFavorites && !newFavoriteStatus) {
                movies = movies.filter { it.id != movie.id }
            }
        }
    }

    LaunchedEffect(initialSearchQuery, showOnlyFavorites) {
        loadMovies(searchQuery)
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
            text = "Search movies, save them locally and open details",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search movies") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (searchQuery.isNotBlank()) {
                    loadMovies(searchQuery.trim())
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = showOnlyFavorites,
                onCheckedChange = { showOnlyFavorites = it }
            )
            Text(text = "Show only favorites")
        }

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
                    text = errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(movies) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = {
                                onMovieClick(movie.id)
                            },
                            onFavoriteToggle = {
                                toggleFavorite(movie)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MovieCard(
    movie: MovieEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
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

            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (movie.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (movie.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (movie.isFavorite) Color.Red else Color.Gray
                )
            }
        }
    }
}
