@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.tv.material3.Text
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.PlaylistAdd
import com.dwplayer.ui.components.CreatePlaylistDialog
import com.dwplayer.ui.components.FocusableCard
import com.dwplayer.ui.components.QrCodeView
import com.dwplayer.ui.theme.*

@Composable
fun AddDownloadDialog(
    companionUrl: String,
    playlists: List<com.dwplayer.data.entities.PlaylistWithItems> = emptyList(),
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
                .fillMaxWidth(0.80f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(28.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Add Movie or Series Download",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Scan QR code with phone or type link below",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    FocusableCard(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp),
                        containerColor = Color.White.copy(alpha = 0.05f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Left: QR Code companion
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(CardDark.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Easy Phone Remote",
                            color = AccentSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        QrCodeView(data = companionUrl, size = 150.dp)

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = companionUrl,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Open on same Wi-Fi",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Right: Manual Input
                    Column(
                        modifier = Modifier.weight(1.6f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Or enter URL manually:",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // URL Input Box
                        Column {
                            Text("Download URL *", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(BgDark, RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = inputUrl,
                                    onValueChange = { inputUrl = it },
                                    singleLine = true,
                                    cursorBrush = SolidColor(AccentPrimary),
                                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (inputUrl.isEmpty()) {
                                    Text("https://.../movie.mp4", color = TextTertiary, fontSize = 13.sp)
                                }
                            }
                        }

                        // File Name Box
                        Column {
                            Text("Custom Name (Optional)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(BgDark, RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
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
                                    "Assign to Playlist / Series (Optional)",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (onCreatePlaylist != null) {
                                    FocusableCard(
                                        onClick = { showCreatePlaylistDialog = true },
                                        containerColor = Color.Transparent
                                    ) {
                                        Text(
                                            "+ New Series",
                                            color = AccentCyan,
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
                                    FocusableCard(
                                        onClick = { selectedPlaylistId = null },
                                        containerColor = if (selectedPlaylistId == null) AccentPrimary else Color.White.copy(alpha = 0.06f),
                                        focusedContainerColor = AccentPrimary.copy(alpha = 0.8f),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "None",
                                                color = Color.White,
                                                fontSize = 12.sp,
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
                                        focusedContainerColor = AccentPrimary.copy(alpha = 0.8f),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                                .fillMaxHeight(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PlaylistPlay,
                                                null,
                                                tint = if (isSel) Color.White else AccentCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                p.playlist.name,
                                                color = Color.White,
                                                fontSize = 12.sp,
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
                            focusedContainerColor = AccentSecondary
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Download", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
