@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val DwTvColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = TextPrimary,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary
)

@Composable
fun DwPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DwTvColorScheme,
        content = content
    )
}
