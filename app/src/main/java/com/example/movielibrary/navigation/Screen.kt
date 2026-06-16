package com.example.movielibrary.navigation

sealed class Screen(val route: String) {

    object Login : Screen("login")

    object Register : Screen("register")

    object Home : Screen("home")

    object Details : Screen("details/{movieId}") {
        fun createRoute(movieId: Int): String {
            return "details/$movieId"
        }
    }
}