package io.github.xuzhike123.libretvtv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun libreColorScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = LibreOnAccent,
    background = LibreBackground,
    onBackground = LibreTextPrimary,
    surface = LibreSurface,
    onSurface = LibreTextPrimary,
    surfaceVariant = LibreSurfaceVariant,
    onSurfaceVariant = LibreTextSecondary,
    outline = LibreOutline
)

@Composable
fun LibreTvTheme(
    accent: Color = AccentTeal,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = libreColorScheme(accent),
        typography = Typography(),
        content = content
    )
}
