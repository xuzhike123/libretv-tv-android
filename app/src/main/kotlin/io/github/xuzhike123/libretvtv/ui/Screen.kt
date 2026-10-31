package io.github.xuzhike123.libretvtv.ui

sealed class Screen(val route: String) {

    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Favorites : Screen("favorites")
    data object History : Screen("history")
    data object Live : Screen("live")
    data object Sources : Screen("sources")
    data object Settings : Screen("settings")
    data object Detail : Screen("detail/{videoId}") {
        fun createRoute(videoId: String) = "detail/$videoId"
    }
    data object Player : Screen("player")
}
