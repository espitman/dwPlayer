@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.dwplayer.ui.theme.*

@Composable
fun FocusableCard(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    containerColor: Color = CardDark.copy(alpha = 0.6f),
    focusedContainerColor: Color = CardDark.copy(alpha = 0.95f),
    contentColor: Color = TextPrimary,
    focusedContentColor: Color = TextPrimary,
    borderColor: Color = Color.White.copy(alpha = 0.08f),
    focusedBorderColor: Color = AccentPrimary,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    scale: Float = 1.0f,
    contentScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var lastLongClickTime by remember { mutableLongStateOf(0L) }

    val safeOnClick = remember(onClick, onLongClick) {
        {
            val now = System.currentTimeMillis()
            if (onLongClick == null || (now - lastLongClickTime > 700L)) {
                onClick()
            }
        }
    }

    val safeOnLongClick = remember(onLongClick) {
        if (onLongClick != null) {
            {
                lastLongClickTime = System.currentTimeMillis()
                onLongClick()
            }
        } else null
    }

    val animatedContentScale by animateFloatAsState(
        targetValue = if (isFocused) contentScale else 1.0f,
        animationSpec = tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing
        ),
        label = "contentScale"
    )

    Surface(
        onClick = safeOnClick,
        onLongClick = safeOnLongClick,
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape),
        border = ClickableSurfaceDefaults.border(
            border = androidx.tv.material3.Border(BorderStroke(1.dp, borderColor)),
            focusedBorder = androidx.tv.material3.Border(BorderStroke(2.dp, focusedBorderColor))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = scale),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            focusedContainerColor = focusedContainerColor,
            contentColor = contentColor,
            focusedContentColor = focusedContentColor
        )
    ) {
        if (contentScale != 1.0f) {
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = animatedContentScale
                    scaleY = animatedContentScale
                }
            ) {
                content()
            }
        } else {
            content()
        }
    }
}
