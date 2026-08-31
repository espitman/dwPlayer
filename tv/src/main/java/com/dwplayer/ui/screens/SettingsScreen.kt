@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.dwplayer.data.models.StorageInfo
import com.dwplayer.core.player.SubtitleSettings
import com.dwplayer.ui.components.FocusableCard
import com.dwplayer.ui.components.QrCodeView
import com.dwplayer.ui.theme.*

@Composable
fun SettingsScreen(
    companionUrl: String,
    storageInfo: StorageInfo,
    subtitleSettings: SubtitleSettings,
    appVersion: String,
    deviceName: String,
    androidVersion: String,
    modifier: Modifier = Modifier
) {
    var showQrDialog by remember { mutableStateOf(false) }
    var activeDetails by remember { mutableStateOf<SettingsDetails?>(null) }

    activeDetails?.let { details ->
        SettingsDetailsDialog(details = details, onDismiss = { activeDetails = null })
    }

    if (showQrDialog) {
        Dialog(
            onDismissRequest = { showQrDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .width(440.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceDark)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                    .padding(26.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Web Remote Companion",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Scan with your phone to control playback & send links",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    QrCodeView(data = companionUrl, size = 180.dp)

                    Text(
                        text = companionUrl,
                        color = AccentPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    FocusableCard(
                        onClick = { showQrDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        containerColor = Color.White.copy(alpha = 0.08f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Close", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0x142A493B), Color.Transparent),
                    radius = 680f
                )
            )
            .padding(start = 42.dp, end = 42.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "DWPLAYER TV",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp
            )
            Text(
                text = "Connection, playback and storage details for this device.",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }

        // 2x2 Settings Grid
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Web Remote
                SettingTile(
                    title = "Web remote",
                    description = "Control playback and send links from any device on this network.",
                    value = if (companionUrl.isNotBlank()) "${companionUrl.removePrefix("http://").removePrefix("https://").substringBefore("/")} • READY" else "OFFLINE",
                    onClick = { showQrDialog = true },
                    modifier = Modifier.weight(1f)
                )

                // 2. Playback
                SettingTile(
                    title = "Playback",
                    description = "Current subtitle defaults and resume behavior used by the player.",
                    value = "${subtitleSettings.font.displayName.uppercase()} • ${subtitleSettings.size.displayName.uppercase()}",
                    onClick = {
                        activeDetails = SettingsDetails(
                            title = "Playback",
                            rows = listOf(
                                "Decoder" to "Media3 automatic selection",
                                "Resume" to "Saved every 5 seconds",
                                "Subtitle font" to subtitleSettings.font.displayName,
                                "Subtitle size" to subtitleSettings.size.displayName,
                                "Subtitle color" to subtitleSettings.color.displayName,
                                "Subtitle style" to subtitleSettings.backgroundStyle.displayName,
                                "Subtitle position" to subtitleSettings.position.displayName
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 3. Storage
                SettingTile(
                    title = "Storage",
                    description = "Manage downloaded and archived media on this TV.",
                    value = "${storageInfo.freeSpace} FREE",
                    onClick = {
                        activeDetails = SettingsDetails(
                            title = "Storage",
                            rows = listOf(
                                "Location" to storageInfo.path,
                                "Free" to storageInfo.freeSpace,
                                "Total" to storageInfo.totalSpace,
                                "Used" to "${storageInfo.usedPercent}%"
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                // 4. About
                SettingTile(
                    title = "About dwPlayer",
                    description = "Build information and Android TV permissions.",
                    value = "VERSION $appVersion • ANDROID $androidVersion",
                    onClick = {
                        activeDetails = SettingsDetails(
                            title = "About dwPlayer",
                            rows = listOf(
                                "Version" to appVersion,
                                "Device" to deviceName,
                                "Android" to androidVersion,
                                "Platform" to "Android TV"
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private data class SettingsDetails(
    val title: String,
    val rows: List<Pair<String, String>>
)

@Composable
private fun SettingsDetailsDialog(
    details: SettingsDetails,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .width(540.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(26.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(details.title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                details.rows.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(120.dp))
                        Text(
                            value,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                FocusableCard(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    containerColor = AccentPrimary,
                    focusedContainerColor = AccentSecondary,
                    contentColor = BgDark,
                    focusedContentColor = BgDark
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Done", color = BgDark, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingTile(
    title: String,
    description: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FocusableCard(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(21.dp),
        containerColor = SurfaceDark.copy(alpha = 0.58f),
        focusedContainerColor = CardDark,
        borderColor = Color.White.copy(alpha = 0.10f),
        focusedBorderColor = AccentPrimary,
        scale = 1.018f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 21.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}
