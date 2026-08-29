package com.dwplayer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.dwplayer.ui.theme.BgDark

@Composable
fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val animatedOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "anim1"
    )
    val animatedOffset2 by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "anim2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Base dark
        drawRect(color = BgDark)

        // Radial glow 1 (Deep blue top right)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1E3A8A).copy(alpha = 0.35f), Color.Transparent),
                center = Offset(size.width * 0.85f + animatedOffset1, size.height * 0.15f),
                radius = size.width * 0.6f
            )
        )

        // Radial glow 2 (Indigo bottom left)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF312E81).copy(alpha = 0.25f), Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.85f + animatedOffset2),
                radius = size.width * 0.5f
            )
        )
    }
}
