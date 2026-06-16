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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.movielibrary.data.remote.api.RetrofitClient
import com.example.movielibrary.data.remote.model.ShowDto
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val scope = rememberCoroutineScope()

    var movies by remember { mutableStateOf<List<ShowDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                val response = RetrofitClient.api.searchMovies("batman")
                movies = response.map { it.show }
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Nu s-au putut încărca filmele. Verifică internetul."
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
            text = "Popular Batman shows from TVMaze",
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
fun MovieCard(movie: ShowDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AsyncImage(
                model = movie.image?.medium,
                contentDescription = movie.name,
                modifier = Modifier
                    .size(width = 90.dp, height = 130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = movie.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text("Language: ${movie.language ?: "Unknown"}")
                Text("Rating: ${movie.rating?.average ?: "N/A"}")
                Text("Runtime: ${movie.runtime ?: 0} min")

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Genres: ${movie.genres?.joinToString(", ") ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}