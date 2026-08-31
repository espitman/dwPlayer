@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import androidx.tv.material3.LocalContentColor
import com.dwplayer.R
import com.dwplayer.core.player.*
import com.dwplayer.ui.components.FocusableCard
import com.dwplayer.ui.theme.*

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private enum class SubtitleSettingTab(val title: String, val icon: ImageVector) {
    TRACKS("Tracks", Icons.Default.Subtitles),
    FONT("Font", Icons.Default.TextFields),
    SIZE("Size", Icons.Default.FormatSize),
    COLOR("Color", Icons.Default.Palette),
    BACKGROUND("Background & Edge", Icons.Default.BorderStyle),
    POSITION("Position", Icons.Default.VerticalAlignBottom)
}

@Composable
fun SubtitleSettingsDialog(
    subtitleTracks: List<TrackInfo>,
    selectedTrackIndex: Int,
    settings: SubtitleSettings,
    onSelectTrack: (TrackInfo?) -> Unit,
    onUpdateSettings: (SubtitleSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf(SubtitleSettingTab.TRACKS) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header: Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Subtitles,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Subtitle Settings",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    FocusableCard(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp),
                        containerColor = Color.White.copy(alpha = 0.08f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Subtitle Live Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF090D16))
                        .border(1.dp, AccentPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val previewFontFamily = when (settings.font) {
                        SubtitleFont.VAZIRMATN -> VazirmatnFontFamily
                        SubtitleFont.VAZIRMATN_BOLD -> VazirmatnFontFamily
                        SubtitleFont.SYSTEM_DEFAULT -> FontFamily.Default
                    }
                    val previewFontWeight = when (settings.font) {
                        SubtitleFont.VAZIRMATN_BOLD -> FontWeight.Bold
                        else -> FontWeight.Medium
                    }
                    val previewTextColor = Color(settings.color.composeColor)
                    val previewBoxBg = when (settings.backgroundStyle) {
                        SubtitleBackgroundStyle.TRANSLUCENT_BOX -> Color.Black.copy(alpha = 0.65f)
                        SubtitleBackgroundStyle.SOLID_BOX -> Color.Black.copy(alpha = 0.95f)
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(previewBoxBg)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Subtitle Sample Preview • نمونه پیش‌نمایش زیرنویس",
                            color = previewTextColor,
                            fontSize = (settings.size.spSize * 0.75f).sp,
                            fontWeight = previewFontWeight,
                            fontFamily = previewFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Main Content: Left Tabs + Right Options List
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Column: Setting Categories / Tabs
                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SubtitleSettingTab.values().forEachIndexed { index, tab ->
                            val isSelected = tab == activeTab
                            FocusableCard(
                                onClick = { activeTab = tab },
                                containerColor = if (isSelected) AccentPrimary else Color.White.copy(alpha = 0.05f),
                                focusedContainerColor = if (isSelected) AccentSecondary else AccentPrimary.copy(alpha = 0.8f),
                                contentColor = if (isSelected) BgDark else TextSecondary,
                                focusedContentColor = BgDark,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        tab.icon,
                                        contentDescription = null,
                                        tint = LocalContentColor.current,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = tab.title,
                                        color = LocalContentColor.current,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Right Column: Options for Active Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardDark.copy(alpha = 0.4f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        when (activeTab) {
                            SubtitleSettingTab.TRACKS -> {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    item {
                                        val isNone = selectedTrackIndex == -1
                                        FocusableCard(
                                            onClick = { onSelectTrack(null) },
                                            containerColor = if (isNone) AccentPrimary else Color.White.copy(alpha = 0.05f),
                                            focusedContainerColor = if (isNone) AccentSecondary else CardDark,
                                            contentColor = if (isNone) BgDark else TextPrimary,
                                            focusedContentColor = if (isNone) BgDark else TextPrimary,
                                            modifier = Modifier.fillMaxWidth().height(44.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Off / None", color = LocalContentColor.current, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                if (isNone) Icon(Icons.Default.Check, null, tint = LocalContentColor.current, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }

                                    items(subtitleTracks) { track ->
                                        val isSelected = track.isSelected
                                        FocusableCard(
                                            onClick = { onSelectTrack(track) },
                                            containerColor = if (isSelected) AccentPrimary else Color.White.copy(alpha = 0.05f),
                                            focusedContainerColor = if (isSelected) AccentSecondary else CardDark,
                                            contentColor = if (isSelected) BgDark else TextPrimary,
                                            focusedContentColor = if (isSelected) BgDark else TextPrimary,
                                            modifier = Modifier.fillMaxWidth().height(44.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(track.label, color = LocalContentColor.current, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                if (isSelected) Icon(Icons.Default.Check, null, tint = LocalContentColor.current, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            SubtitleSettingTab.FONT -> {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(SubtitleFont.values()) { font ->
                                        val isSelected = font == settings.font
                                        FocusableCard(
                                            onClick = { onUpdateSettings(settings.copy(font = font)) },
                                            containerColor = if (isSelected) AccentPrimary else Color.White.copy(alpha = 0.05f),
                                            focusedContainerColor = if (isSelected) AccentSecondary else CardDark,
                                            contentColor = if (isSelected) BgDark else TextPrimary,
                                            focusedContentColor = if (isSelected) BgDark else TextPrimary,
                                            modifier = Modifier.fillMaxWidth().height(44.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(font.displayName, color = LocalContentColor.current, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                if (isSelected) Icon(Icons.Default.Check, null, tint = LocalContentColor.current, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            SubtitleSettingTab.SIZE -> {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(SubtitleSize.values()) { size ->
                                        val isSelected = size == settings.size
                                        FocusableCard(
                                            onClick = { onUpdateSettings(settings.copy(size = size)) },
                                            containerColor = if (isSelected) AccentPrimary else Color.White.copy(alpha = 0.05f),
                                            focusedContainerColor = if (isSelected) AccentSecondary else CardDark,
                                            contentColor = if (isSelected) BgDark else TextPrimary,
                                            focusedContentColor = if (isSelected) BgDark else TextPrimary,
                                            modifier = Modifier.fillMaxWidth().height(44.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(size.displayName, color = LocalContentColor.current, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                if (isSelected) Icon(Icons.Default.Check, null, tint = LocalContentColor.current, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            SubtitleSettingTab.COLOR -> {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(SubtitleColor.values()) { col ->
                                        val isSelected = col == settings.color
                                        FocusableCard(
                                            onClick = { onUpdateSettings(settings.copy(color = col)) },
                                            containerColor = if (isSelected) AccentPrimary else Color.White.copy(alpha = 0.05f),
                                            focusedContainerColor = if (isSelected) AccentSecondary else CardDark,
                                            contentColor = if (isSelected) BgDark else TextPrimary,
                                            focusedContentColor = if (isSelected) BgDark else TextPrimary,
                                            modifier = Modifier.fillMaxWidth().height(44.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(Color(col.composeColor)))
                                                    Text(col.displayName, color = LocalContentColor.current, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                }
                                                if (isSelected) Icon(Icons.Default.Check, null, tint = LocalContentColor.current, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            SubtitleSettingTab.BACKGROUND -> {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(SubtitleBackgroundStyle.values()) { bg ->
                                        val isSelected = bg == settings.backgroundStyle
                                        FocusableCard(
                                            onClick = { onUpdateSettings(settings.copy(backgroundStyle = bg)) },
                                            containerColor = if (isSelected) AccentPrimary else Color.White.copy(alpha = 0.05f),
                                            focusedContainerColor = if (isSelected) AccentSecondary else CardDark,
                                            contentColor = if (isSelected) BgDark else TextPrimary,
                                            focusedContentColor = if (isSelected) BgDark else TextPrimary,
                                            modifier = Modifier.fillMaxWidth().height(44.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(bg.displayName, color = LocalContentColor.current, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                if (isSelected) Icon(Icons.Default.Check, null, tint = LocalContentColor.current, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            SubtitleSettingTab.POSITION -> {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(SubtitlePosition.values()) { pos ->
                                        val isSelected = pos == settings.position
                                        FocusableCard(
                                            onClick = { onUpdateSettings(settings.copy(position = pos)) },
                                            containerColor = if (isSelected) AccentPrimary else Color.White.copy(alpha = 0.05f),
                                            focusedContainerColor = if (isSelected) AccentSecondary else CardDark,
                                            contentColor = if (isSelected) BgDark else TextPrimary,
                                            focusedContentColor = if (isSelected) BgDark else TextPrimary,
                                            modifier = Modifier.fillMaxWidth().height(44.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(pos.displayName, color = LocalContentColor.current, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                if (isSelected) Icon(Icons.Default.Check, null, tint = LocalContentColor.current, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
