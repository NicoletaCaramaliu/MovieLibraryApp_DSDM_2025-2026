package com.example.movielibrary.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.movielibrary.data.local.database.DatabaseProvider
import com.example.movielibrary.data.local.entity.MovieEntity
import com.example.movielibrary.data.remote.api.RetrofitClient
import com.example.movielibrary.data.remote.model.CastResponse
import com.example.movielibrary.data.remote.model.SeasonDto
import com.example.movielibrary.data.remote.model.ShowDto
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
            val localMovie = movieDao.getMovieById(movieId)
            isFavorite = localMovie?.isFavorite ?: false

            movie = RetrofitClient.api.getMovieDetails(movieId)
            cast = RetrofitClient.api.getMovieCast(movieId)
            seasons = RetrofitClient.api.getMovieSeasons(movieId)
            
            errorMessage = null
        } catch (e: Exception) {
            val localMovie = movieDao.getMovieById(movieId)
            if (localMovie != null) {
                errorMessage = "Offline mode. Showing saved data."
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
                    val entity = MovieEntity(
                        id = currentMovie.id,
                        name = currentMovie.name,
                        language = currentMovie.language,
                        genres = currentMovie.genres?.joinToString(", "),
                        runtime = currentMovie.runtime,
                        rating = currentMovie.rating?.average,
                        imageUrl = currentMovie.image?.medium,
                        summary = currentMovie.summary,
                        isFavorite = newFavoriteStatus,
                        totalEpisodes = seasons.sumOf { it.episodeOrder ?: 0 }
                    )
                    movieDao.insertMovies(listOf(entity))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (movie != null) {
                        IconButton(onClick = { toggleFavorite() }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                errorMessage != null && movie == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                    }
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
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = currentMovie.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = currentMovie.language ?: "Unknown",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                val totalEpisodes = seasons.sumOf { it.episodeOrder ?: 0 }
                                val runtimeText = "${currentMovie.runtime ?: currentMovie.averageRuntime ?: "?"} min"
                                val episodesText = if (totalEpisodes > 0) " • $totalEpisodes episodes" else ""
                                
                                Text(
                                    text = "$runtimeText$episodesText • ${currentMovie.premiered?.take(4) ?: "N/A"}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currentMovie.genres?.forEach { genre ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Text(
                                            text = genre,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            SectionTitle("Overview")
                            Text(
                                text = cleanHtml(currentMovie.summary ?: "No summary available."),
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            SectionTitle("Cast")
                            if (cast.isEmpty()) {
                                Text("No cast information available.")
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 8.dp)
                                ) {
                                    items(cast.take(15)) { castItem ->
                                        CastItem(castItem)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            SectionTitle("Seasons")
                            if (seasons.isEmpty()) {
                                Text("No seasons available.")
                            } else {
                                seasons.forEach { season ->
                                    SeasonCard(season)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun CastItem(castItem: CastResponse) {
    Column(
        modifier = Modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = castItem.person.image?.medium,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = castItem.person.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = castItem.character.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SeasonCard(season: SeasonDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Season ${season.number ?: "-"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${season.episodeOrder ?: 0} episodes",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = season.premiereDate?.take(4) ?: "",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun cleanHtml(text: String): String {
    return text
        .replace(Regex("<[^>]*>"), "")
}
