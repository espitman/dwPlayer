@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

private val DwTvColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = BgDark,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondary,
    border = BorderDark
)

val DwDisplayFont = FontFamily.SansSerif
val DwBodyFont = FontFamily.SansSerif
val DwMonoFont = FontFamily.Monospace

private val DwTvTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DwDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 58.sp,
        lineHeight = 58.sp,
        letterSpacing = (-2.2).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = DwDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1.8).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DwDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = DwDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DwBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DwBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DwBodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DwMonoFont,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.1.sp
    )
)

@Composable
fun DwPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DwTvColorScheme,
        typography = DwTvTypography,
        content = content
    )
}
