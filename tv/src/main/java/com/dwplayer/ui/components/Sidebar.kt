@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.dwplayer.ui.theme.*

enum class NavDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    DOWNLOADS("Downloads", Icons.Default.Download),
    PLAYLISTS("Playlists", Icons.Default.PlaylistPlay),
    ARCHIVE("Library", Icons.Default.Folder),
    SMB("Network", Icons.Default.CloudQueue),
    SETTINGS("Settings", Icons.Default.Settings),
    ADD("Add URL", Icons.Default.Add)
}

@Composable
fun TvSidebar(
    currentDestination: NavDestination,
    onNavigate: (NavDestination) -> Unit,
    onAddClicked: () -> Unit,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null
) {
    val navItems = remember {
        listOf(
            NavDestination.HOME,
            NavDestination.DOWNLOADS,
            NavDestination.PLAYLISTS,
            NavDestination.ARCHIVE,
            NavDestination.SMB,
            NavDestination.SETTINGS
        )
    }

    Column(
        modifier = modifier
            .width(84.dp)
            .fillMaxHeight()
            .background(SurfaceDark.copy(alpha = 0.96f))
            .border(
                width = 1.dp,
                color = BorderDark,
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            )
            .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            .padding(vertical = 24.dp, horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Top Logo Mark
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Logo",
                tint = AccentPrimary,
                modifier = Modifier.size(26.dp)
            )
        }

        // 2. Navigation Rail Icons
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            navItems.forEachIndexed { index, destination ->
                val isSelected = currentDestination == destination
                val itemModifier = if (index == 0 && firstItemFocusRequester != null) {
                    Modifier.focusRequester(firstItemFocusRequester)
                } else {
                    Modifier
                }

                RailItem(
                    destination = destination,
                    isSelected = isSelected,
                    modifier = itemModifier,
                    onClick = { onNavigate(destination) }
                )
            }
        }

        // 3. Bottom Quick Add Button
        FocusableCard(
            onClick = onAddClicked,
            modifier = Modifier.size(50.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White.copy(alpha = 0.06f),
            focusedContainerColor = AccentPrimary,
            focusedBorderColor = Color.White
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add URL",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun RailItem(
    destination: NavDestination,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    FocusableCard(
        onClick = onClick,
        modifier = modifier
            .size(50.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(16.dp),
        scale = 1.08f,
        containerColor = if (isSelected) Color.White else Color.Transparent,
        focusedContainerColor = if (isSelected) Color.White else CardDark,
        borderColor = if (isSelected) Color.White else Color.Transparent,
        focusedBorderColor = AccentPrimary
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                modifier = Modifier.size(22.dp),
                tint = if (isSelected) Color.Black else if (isFocused) Color.White else TextSecondary
            )
        }
    }
}
