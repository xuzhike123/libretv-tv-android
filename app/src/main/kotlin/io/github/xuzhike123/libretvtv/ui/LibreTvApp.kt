package io.github.xuzhike123.libretvtv.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import io.github.xuzhike123.libretvtv.data.UiSettings
import io.github.xuzhike123.libretvtv.data.favoriteRows
import io.github.xuzhike123.libretvtv.data.historyRows
import io.github.xuzhike123.libretvtv.data.homeRows
import io.github.xuzhike123.libretvtv.ui.components.LibreTvNavRail
import io.github.xuzhike123.libretvtv.ui.screens.CatalogScreen
import io.github.xuzhike123.libretvtv.ui.screens.DetailScreen
import io.github.xuzhike123.libretvtv.ui.screens.LiveScreen
import io.github.xuzhike123.libretvtv.ui.screens.PlayerScreen
import io.github.xuzhike123.libretvtv.ui.screens.SearchScreen
import io.github.xuzhike123.libretvtv.ui.screens.SettingsScreen
import io.github.xuzhike123.libretvtv.ui.screens.SourcesScreen
import io.github.xuzhike123.libretvtv.ui.util.onTvKey

/**
 * 应用根布局：左侧 SmartTube 风格导航栏 + 右侧页面容器。
 */
@Composable
fun LibreTvApp() {
    val navController = rememberNavController()
    var settings by remember { mutableStateOf(UiSettings()) }
    var currentRoute by remember { mutableStateOf(Screen.Home.route) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .onTvKey { key ->
                if (key == Key.Back || key == Key.Escape) {
                    navController.popBackStack()
                } else {
                    false
                }
            }
    ) {
        LibreTvNavRail(
            selectedRoute = currentRoute,
            onSelect = { screen ->
                currentRoute = screen.route
                navController.navigate(screen.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.weight(1f)
        ) {
            composable(Screen.Home.route) {
                CatalogScreen(rows = homeRows(), settings = settings) { video ->
                    navController.navigate(Screen.Detail.createRoute(video.id))
                }
            }
            composable(Screen.Search.route) {
                SearchScreen()
            }
            composable(Screen.Favorites.route) {
                CatalogScreen(rows = favoriteRows(), settings = settings) { video ->
                    navController.navigate(Screen.Detail.createRoute(video.id))
                }
            }
            composable(Screen.History.route) {
                CatalogScreen(rows = historyRows(), settings = settings) { video ->
                    navController.navigate(Screen.Detail.createRoute(video.id))
                }
            }
            composable(Screen.Live.route) {
                LiveScreen()
            }
            composable(Screen.Sources.route) {
                SourcesScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settings = settings,
                    onSettingsChange = { next -> settings = next }
                )
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId").orEmpty()
                DetailScreen(
                    videoId = videoId,
                    onPlay = { id -> navController.navigate(Screen.Player.createRoute(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.Player.route,
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId").orEmpty()
                PlayerScreen(
                    videoId = videoId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
