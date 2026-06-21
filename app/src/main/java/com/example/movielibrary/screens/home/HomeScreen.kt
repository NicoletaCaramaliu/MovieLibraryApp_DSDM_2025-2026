package com.example.movielibrary.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.movielibrary.data.local.database.DatabaseProvider
import com.example.movielibrary.data.local.entity.MovieEntity
import com.example.movielibrary.data.remote.api.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    initialSearchQuery: String = "batman",
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
                            runtime = item.show.runtime ?: item.show.averageRuntime,
                            rating = item.show.rating?.average,
                            imageUrl = item.show.image?.medium,
                            summary = item.show.summary,
                            isFavorite = existingMovie?.isFavorite ?: false,
                            totalEpisodes = item.show._embedded?.seasons?.sumOf { it.episodeOrder ?: 0 }
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Movie Library",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search your favorite shows...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        Button(
                            onClick = { loadMovies(searchQuery.trim()) },
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.padding(end = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Go")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Favorites
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Checkbox(
                    checked = showOnlyFavorites,
                    onCheckedChange = { showOnlyFavorites = it },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "Show My Favorites Only",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(movies) { movie ->
                            MovieCard(
                                movie = movie,
                                onClick = { onMovieClick(movie.id) },
                                onFavoriteToggle = { toggleFavorite(movie) }
                            )
                        }
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
            .height(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Movie Poster
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = movie.imageUrl,
                    contentDescription = movie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Rating badge
                if (movie.rating != null) {
                    Surface(
                        color = Color(0xFFFFD700).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                            Text(
                                text = movie.rating.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            // Movie Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = movie.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (movie.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (movie.isFavorite) Color.Red else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    movie.genres?.split(",")?.take(2)?.forEach { genre ->
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = genre.trim(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val infoText = if (movie.totalEpisodes != null && movie.totalEpisodes > 0) {
                        "${movie.totalEpisodes} episodes"
                    } else {
                        "${movie.runtime ?: "?"} min"
                    }
                    Text(
                        text = "${movie.language ?: "English"} • $infoText",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
