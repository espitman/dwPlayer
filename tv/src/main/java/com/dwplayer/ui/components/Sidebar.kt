@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.dwplayer.ui.theme.*

enum class NavDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    PLAYLISTS("Playlists", Icons.Outlined.CalendarMonth),
    SMB("Network", Icons.Outlined.CloudQueue),
    DOWNLOADS("Downloads", Icons.Outlined.Download),
    ARCHIVE("Library", Icons.Outlined.Folder),
    SETTINGS("Settings", Icons.Outlined.Tune),
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
            NavDestination.PLAYLISTS,
            NavDestination.SMB,
            NavDestination.DOWNLOADS,
            NavDestination.ARCHIVE,
            NavDestination.SETTINGS
        )
    }

    Column(
        modifier = modifier
            .width(96.dp)
            .fillMaxHeight()
            .background(Color(0xFF090C0A))
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Top Logo Mark (matching prototype)
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF161A17))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Logo",
                tint = AccentPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        // 2. Navigation Rail Icons (Generous spacing)
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
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
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.Transparent,
            focusedContainerColor = CardDark,
            focusedBorderColor = AccentPrimary
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add URL",
                    tint = TextSecondary,
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
            .size(54.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(16.dp),
        scale = 1.05f,
        containerColor = if (isSelected) Color.White else Color.Transparent,
        focusedContainerColor = if (isSelected) Color.White else Color(0xFF181D1A),
        borderColor = Color.Transparent,
        focusedBorderColor = AccentPrimary
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) Color.Black else if (isFocused) Color.White else Color(0xFF7A857E)
            )
        }
    }
}
