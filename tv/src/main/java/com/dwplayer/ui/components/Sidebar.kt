@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.tv.material3.*
import com.dwplayer.ui.theme.*

enum class NavDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    DOWNLOADS("Downloads", Icons.Default.Download),
    PLAYLISTS("Playlists", Icons.Default.PlaylistPlay),
    ARCHIVE("Archive", Icons.Default.VideoLibrary),
    SMB("Network Shares", Icons.Default.CloudQueue),
    ADD("Add Link", Icons.Default.AddLink)
}

@Composable
fun TvSidebar(
    currentDestination: NavDestination,
    onNavigate: (NavDestination) -> Unit,
    onAddClicked: () -> Unit,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null
) {
    Column(
        modifier = modifier
            .width(220.dp)
            .fillMaxHeight()
            .padding(16.dp)
            .background(SurfaceDark.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(vertical = 20.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Header: Logo + App Name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AccentPrimary, Color(0xFF4F46E5))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = "dwPlayer",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "TV Cinema Hub",
                    color = AccentSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                )
            }
        }

        // Navigation Menu List
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            NavDestination.values().forEachIndexed { index, destination ->
                val isSelected = currentDestination == destination
                val itemModifier = if (index == 0 && firstItemFocusRequester != null) {
                    Modifier.focusRequester(firstItemFocusRequester)
                } else {
                    Modifier
                }

                SidebarItem(
                    destination = destination,
                    isSelected = isSelected,
                    modifier = itemModifier,
                    onClick = {
                        if (destination == NavDestination.ADD) {
                            onAddClicked()
                        } else {
                            onNavigate(destination)
                        }
                    }
                )
            }
        }

        // Bottom Info Tag
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "v1.0 • Android TV",
                color = TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun SidebarItem(
    destination: NavDestination,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) AccentPrimary.copy(alpha = 0.22f) else Color.Transparent,
            focusedContainerColor = AccentPrimary,
            contentColor = if (isSelected) Color.White else TextSecondary,
            focusedContentColor = Color.White
        ),
        border = ClickableSurfaceDefaults.border(
            border = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) AccentPrimary.copy(alpha = 0.4f) else Color.Transparent
                )
            ),
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, AccentSecondary)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                modifier = Modifier.size(20.dp),
                tint = if (isFocused) Color.White else if (isSelected) AccentSecondary else TextSecondary
            )

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = destination.label,
                fontSize = 13.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                color = if (isFocused || isSelected) Color.White else TextSecondary,
                maxLines = 1
            )
        }
    }
}
