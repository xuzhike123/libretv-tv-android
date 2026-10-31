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
import io.github.xuzhike123.libretvtv.data.ContentRow
import io.github.xuzhike123.libretvtv.data.LocalStore
import io.github.xuzhike123.libretvtv.data.PendingPlay
import io.github.xuzhike123.libretvtv.data.UiSettings
import io.github.xuzhike123.libretvtv.data.VideoItem
import io.github.xuzhike123.libretvtv.ui.components.LibreTvNavRail
import io.github.xuzhike123.libretvtv.ui.screens.CatalogScreen
import io.github.xuzhike123.libretvtv.ui.screens.DetailScreen
import io.github.xuzhike123.libretvtv.ui.screens.HomeScreen
import io.github.xuzhike123.libretvtv.ui.screens.LiveScreen
import io.github.xuzhike123.libretvtv.ui.screens.PlayerScreen
import io.github.xuzhike123.libretvtv.ui.screens.SearchScreen
import io.github.xuzhike123.libretvtv.ui.screens.SettingsScreen
import io.github.xuzhike123.libretvtv.ui.screens.SourcesScreen
import io.github.xuzhike123.libretvtv.ui.util.onTvKey
import java.net.URLEncoder

/**
 * 应用根布局：左侧 SmartTube 风格导航栏 + 右侧页面容器。
 */
@Composable
fun LibreTvApp() {
    val navController = rememberNavController()
    var settings by remember { mutableStateOf(UiSettings()) }
    var currentRoute by remember { mutableStateOf(Screen.Home.route) }

    fun goto(route: String) {
        currentRoute = route
        navController.navigate(route) { launchSingleTop = true; restoreState = true }
    }

    val onVideoClick: (VideoItem) -> Unit = { video ->
        val encoded = URLEncoder.encode(video.id, "UTF-8")
        navController.navigate(Screen.Detail.createRoute(encoded))
    }

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
            onSelect = { screen -> goto(screen.route) }
        )

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.weight(1f)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    settings = settings,
                    onSearch = { goto(Screen.Search.route) },
                    onVideoClick = onVideoClick
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(settings = settings, onVideoClick = onVideoClick)
            }
            composable(Screen.Favorites.route) {
                val items = LocalStore.getFavorites().map { (id, title) ->
                    VideoItem(id = id, title = title, source = "收藏", duration = "")
                }
                CatalogScreen(
                    rows = listOf(ContentRow(title = "我的收藏", subtitle = "仅保存在本机", items = items)),
                    settings = settings,
                    onVideoClick = onVideoClick
                )
            }
            composable(Screen.History.route) {
                val items = LocalStore.getHistory().map { h ->
                    VideoItem(
                        id = h.videoId,
                        title = h.title,
                        source = h.source,
                        duration = if (h.durationMs > 0) "看到 ${(h.positionMs * 100 / h.durationMs)}%" else "",
                        progressPercent = if (h.durationMs > 0) (h.positionMs * 100 / h.durationMs).toInt() else null
                    )
                }
                CatalogScreen(
                    rows = listOf(ContentRow(title = "观看历史", subtitle = "近 50 条", items = items)),
                    settings = settings,
                    onVideoClick = onVideoClick
                )
            }
            composable(Screen.Live.route) {
                LiveScreen(
                    onChannelClick = { ch, all, idx ->
                        PendingPlay.set(
                            url = ch.url,
                            title = ch.name,
                            source = ch.group,
                            videoId = "",
                            isLive = true,
                            liveChannels = all,
                            liveIndex = idx
                        )
                        goto(Screen.Player.route)
                    }
                )
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
                    onPlay = { url, title, source, vid ->
                        PendingPlay.set(url, title, source, vid)
                        goto(Screen.Player.route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Player.route) {
                PlayerScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
