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

@Composable
fun DetailsScreen(
    movieId: Int,
    onBackClick: () -> Unit
) {
    var movie by remember { mutableStateOf<ShowDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(movieId) {
        try {
            isLoading = true
            movie = RetrofitClient.api.getMovieDetails(movieId)
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "Could not load movie details."
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = onBackClick) {
            Text("Back")
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