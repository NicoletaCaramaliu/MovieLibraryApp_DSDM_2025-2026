package com.example.movielibrary.navigation

sealed class Screen(val route: String) {

    object Login : Screen("login")

    object Register : Screen("register")

    object Welcome : Screen("welcome")

    object Home : Screen("home?query={query}") {
        fun createRoute(query: String = "off campus"): String {
            return "home?query=${android.net.Uri.encode(query)}"
        }
    }

    object Details : Screen("details/{movieId}") {
        fun createRoute(movieId: Int): String {
            return "details/$movieId"
        }
    }
}