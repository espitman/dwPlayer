@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.dwplayer.data.entities.PlaylistWithItems
import com.dwplayer.ui.components.CreatePlaylistDialog
import com.dwplayer.ui.components.FocusableCard
import com.dwplayer.ui.components.QrCodeView
import com.dwplayer.ui.theme.*

@Composable
fun AddDownloadDialog(
    companionUrl: String,
    playlists: List<PlaylistWithItems> = emptyList(),
    initialPlaylistId: String? = null,
    onCreatePlaylist: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onAddUrl: (url: String, name: String?, playlistId: String?) -> Unit
) {
    var inputUrl by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf("") }
    var selectedPlaylistId by remember { mutableStateOf<String?>(initialPlaylistId) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    if (showCreatePlaylistDialog && onCreatePlaylist != null) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name ->
                onCreatePlaylist(name)
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(26.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Add Media or Series Download",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Scan QR code with phone or type link below",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    FocusableCard(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp),
                        containerColor = Color.White.copy(alpha = 0.05f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left: QR Code companion
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(CardDark.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Phone Remote",
                            color = AccentPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        QrCodeView(data = companionUrl, size = 140.dp)

                        Text(
                            text = companionUrl,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Right: Manual Input
                    Column(
                        modifier = Modifier.weight(1.7f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // URL Input Box
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Direct Media / Stream URL *", color = TextSecondary, fontSize = 11.sp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardDark)
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = inputUrl,
                                    onValueChange = { inputUrl = it },
                                    singleLine = true,
                                    cursorBrush = SolidColor(AccentPrimary),
                                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (inputUrl.isEmpty()) {
                                    Text("https://.../movie.mp4", color = TextTertiary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        // File Name Box
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Custom Name (Optional)", color = TextSecondary, fontSize = 11.sp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardDark)
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = inputName,
                                    onValueChange = { inputName = it },
                                    singleLine = true,
                                    cursorBrush = SolidColor(AccentPrimary),
                                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (inputName.isEmpty()) {
                                    Text("Episode.01.mkv", color = TextTertiary, fontSize = 13.sp)
                                }
                            }
                        }

                        // Playlist Selection
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Assign to Series (Optional)",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                if (onCreatePlaylist != null) {
                                    FocusableCard(
                                        onClick = { showCreatePlaylistDialog = true },
                                        containerColor = Color.Transparent
                                    ) {
                                        Text(
                                            "+ New Series",
                                            color = AccentPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    val isNone = selectedPlaylistId == null
                                    FocusableCard(
                                        onClick = { selectedPlaylistId = null },
                                        containerColor = if (isNone) Color.White else Color.White.copy(alpha = 0.06f),
                                        focusedContainerColor = if (isNone) Color.White else CardDark,
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp).fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "None",
                                                color = if (isNone) Color.Black else TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                items(playlists, key = { it.playlist.id }) { p ->
                                    val isSel = selectedPlaylistId == p.playlist.id
                                    FocusableCard(
                                        onClick = { selectedPlaylistId = p.playlist.id },
                                        containerColor = if (isSel) AccentPrimary else Color.White.copy(alpha = 0.06f),
                                        focusedContainerColor = AccentPrimary,
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxHeight(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PlaylistPlay,
                                                null,
                                                tint = if (isSel) Color(0xFF0D0F0E) else TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                p.playlist.name,
                                                color = if (isSel) Color(0xFF0D0F0E) else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Submit Button
                        FocusableCard(
                            onClick = {
                                if (inputUrl.isNotBlank()) {
                                    onAddUrl(inputUrl.trim(), inputName.trim().ifEmpty { null }, selectedPlaylistId)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            containerColor = AccentPrimary,
                            focusedContainerColor = AccentSecondary,
                            contentColor = Color(0xFF0D0F0E),
                            focusedContentColor = Color(0xFF0D0F0E)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Download, null, tint = Color(0xFF0D0F0E), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Download", color = Color(0xFF0D0F0E), fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
