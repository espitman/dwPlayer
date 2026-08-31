@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.dwplayer.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.dwplayer.R
import com.dwplayer.data.entities.DownloadTaskEntity
import com.dwplayer.data.entities.PlaybackHistoryEntity
import com.dwplayer.data.entities.SmbShareEntity
import com.dwplayer.data.entities.WebDavServerEntity
import com.dwplayer.data.models.DownloadProgressInfo
import com.dwplayer.data.models.LocalArchiveFile
import com.dwplayer.data.models.StorageInfo
import com.dwplayer.ui.components.FocusableCard
import com.dwplayer.ui.theme.*
import java.util.Locale

@Composable
fun HomeScreen(
    historyList: List<PlaybackHistoryEntity>,
    activeTasks: List<DownloadTaskEntity>,
    liveProgress: Map<String, DownloadProgressInfo>,
    smbShares: List<SmbShareEntity>,
    webDavServers: List<WebDavServerEntity>,
    archiveFiles: List<LocalArchiveFile>,
    storageInfo: StorageInfo,
    companionUrl: String,
    onPlayMedia: (String, String, Boolean) -> Unit,
    onNavigateDownloads: () -> Unit,
    onNavigateSmb: () -> Unit,
    onNavigateArchive: () -> Unit,
    onOpenAddDialog: () -> Unit,
    onClearHistory: () -> Unit = {}
) {
    val archivePaths = archiveFiles.mapTo(hashSetOf()) { it.path.removePrefix("file://") }
    val visibleHistory = historyList.filter { item ->
        item.isSmb ||
            item.mediaUri.startsWith("http://", ignoreCase = true) ||
            item.mediaUri.startsWith("https://", ignoreCase = true) ||
            item.mediaUri.startsWith("content://", ignoreCase = true) ||
            item.mediaUri.removePrefix("file://") in archivePaths
    }
    val featured = visibleHistory.firstOrNull()
    val progress = featured?.progressFraction() ?: 0f
    val progressPercent = (progress * 100).toInt().coerceIn(0, 99)

    BoxWithConstraints(Modifier.fillMaxSize().background(BgDark)) {
        val fullCanvasHeight = maxHeight + 68.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(fullCanvasHeight)
                .offset(y = (-68).dp)
        ) {
            CinematicHero(
                featured = featured,
                progressPercent = progressPercent,
                modifier = Modifier.fillMaxWidth().height(fullCanvasHeight * 0.64f),
                onResume = {
                    if (featured != null) onPlayMedia(featured.mediaUri, featured.title, featured.isSmb)
                    else onOpenAddDialog()
                },
                onDetails = onNavigateArchive
            )

            RecentMediaRail(
                historyList = visibleHistory,
                activeTasks = activeTasks,
                liveProgress = liveProgress,
                smbShares = smbShares,
                webDavServers = webDavServers,
                archiveFiles = archiveFiles,
                storageInfo = storageInfo,
                isRemoteConnected = companionUrl.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 42.dp)
                    .offset(y = fullCanvasHeight * 0.62f),
                onPlayMedia = onPlayMedia,
                onNavigateDownloads = onNavigateDownloads,
                onNavigateSmb = onNavigateSmb,
                onOpenAddDialog = onOpenAddDialog
            )
        }
    }
}

@Composable
private fun CinematicHero(
    featured: PlaybackHistoryEntity?,
    progressPercent: Int,
    modifier: Modifier,
    onResume: () -> Unit,
    onDetails: () -> Unit
) {
    val title = featured?.title?.cinematicTitle() ?: "Your cinema, ready"
    val duration = featured?.durationMs?.takeIf { it > 0 }?.asCompactDuration() ?: "Ready to play"
    val extension = featured?.title?.substringAfterLast('.', "VIDEO")?.uppercase(Locale.US) ?: "VIDEO"
    val source = if (featured?.isSmb == true) "NETWORK" else "TV ARCHIVE"

    Box(modifier) {
        Image(
            painter = painterResource(R.drawable.cinematic_coast_hero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to BgDark,
                    0.30f to BgDark.copy(alpha = 0.94f),
                    0.70f to BgDark.copy(alpha = 0.18f),
                    1f to BgDark.copy(alpha = 0.88f)
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.50f to Color.Transparent,
                    1f to BgDark
                )
            )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 42.dp, top = 100.dp)
                .widthIn(max = 430.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (featured == null) "DWPLAYER TV" else "CONTINUE WATCHING",
                color = TextPrimary.copy(alpha = 0.76f),
                fontFamily = DwMonoFont,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Text(
                text = title,
                color = TextPrimary,
                fontFamily = DwDisplayFont,
                fontSize = 50.sp,
                lineHeight = 50.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-2.2).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroMeta(duration)
                HeroMeta(source)
                HeroMeta(extension)
            }
            Text(
                text = if (featured == null) {
                    "Open a stream or browse your library to start watching on the biggest screen in the house."
                } else {
                    "A saved film from your TV archive. Pick up exactly where you stopped, even when the network is offline."
                },
                color = TextPrimary.copy(alpha = 0.78f),
                fontFamily = DwBodyFont,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusableCard(
                    onClick = onResume,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    containerColor = AccentPrimary,
                    focusedContainerColor = AccentSecondary,
                    contentColor = BgDark,
                    focusedContentColor = BgDark,
                    focusedBorderColor = TextPrimary,
                    scale = 1.025f
                ) {
                    Row(
                        Modifier.fillMaxHeight().padding(horizontal = 21.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = BgDark, modifier = Modifier.size(21.dp))
                        Text(
                            if (featured != null && progressPercent > 0) "Resume $progressPercent%" else "Play media",
                            color = BgDark,
                            fontFamily = DwBodyFont,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                FocusableCard(
                    onClick = onDetails,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    containerColor = TextPrimary.copy(alpha = 0.10f),
                    focusedContainerColor = TextPrimary.copy(alpha = 0.20f),
                    borderColor = TextPrimary.copy(alpha = 0.22f),
                    scale = 1.025f
                ) {
                    Row(
                        Modifier.fillMaxHeight().padding(horizontal = 19.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = TextPrimary, modifier = Modifier.size(19.dp))
                        Text("Details", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMeta(text: String) = Text(
    text = text,
    color = TextPrimary.copy(alpha = 0.82f),
    fontFamily = DwMonoFont,
    fontSize = 11.sp
)

@Composable
private fun RecentMediaRail(
    historyList: List<PlaybackHistoryEntity>,
    activeTasks: List<DownloadTaskEntity>,
    liveProgress: Map<String, DownloadProgressInfo>,
    smbShares: List<SmbShareEntity>,
    webDavServers: List<WebDavServerEntity>,
    archiveFiles: List<LocalArchiveFile>,
    storageInfo: StorageInfo,
    isRemoteConnected: Boolean,
    modifier: Modifier,
    onPlayMedia: (String, String, Boolean) -> Unit,
    onNavigateDownloads: () -> Unit,
    onNavigateSmb: () -> Unit,
    onOpenAddDialog: () -> Unit
) {
    val recent = historyList.take(2)
    val historyPaths = historyList.mapTo(hashSetOf()) { it.mediaUri.removePrefix("file://") }
    val recentArchive = archiveFiles
        .filterNot { it.path in historyPaths }
        .take((2 - recent.size).coerceAtLeast(0))
    val activeTask = activeTasks.firstOrNull()
    val recentCount = recent.size + recentArchive.size
    val networkSourceCount = smbShares.size + webDavServers.size
    val sourceCount = archiveFiles.size + networkSourceCount

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                "Your recent media",
                color = TextPrimary,
                fontFamily = DwDisplayFont,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = when {
                    activeTask != null -> "${activeTasks.size} active download${if (activeTasks.size == 1) "" else "s"}"
                    sourceCount > 0 -> "$sourceCount item${if (sourceCount == 1) "" else "s"} across TV and network"
                    isRemoteConnected -> "Web remote connected · ${storageInfo.freeSpace} free"
                    else -> "TV archive and network sources"
                },
                color = TextSecondary,
                fontFamily = DwBodyFont,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            recent.forEachIndexed { index, item ->
                CinematicMediaCard(
                    title = item.title.cinematicTitle(),
                    subtitle = item.recentSubtitle(),
                    imageRes = fallbackImages[index % fallbackImages.size],
                    progress = item.progressFraction(),
                    modifier = Modifier.weight(if (index == 0) 1.25f else 1f),
                    onClick = { onPlayMedia(item.mediaUri, item.title, item.isSmb) }
                )
            }
            recentArchive.forEachIndexed { index, file ->
                CinematicMediaCard(
                    title = file.name.cinematicTitle(),
                    subtitle = "${file.extension} · ${file.sizeFormatted} · TV archive",
                    imageRes = fallbackImages[(recent.size + index) % fallbackImages.size],
                    progress = 0f,
                    modifier = Modifier.weight(if (recentCount == 1) 1.25f else 1f),
                    onClick = { onPlayMedia(file.path, file.name, false) }
                )
            }
            if (activeTask != null && recentCount < 2) {
                val downloadProgress = liveProgress[activeTask.id]?.progress ?: activeTask.progress
                AbstractMediaCard(
                    activeTask.fileName.cinematicTitle(),
                    "$downloadProgress% · Download queue",
                    Icons.Default.PlayArrow,
                    Modifier.weight(1f),
                    onNavigateDownloads
                )
            }
            AbstractMediaCard(
                if (networkSourceCount == 0) "Phone files" else "Network files",
                if (networkSourceCount == 0) "Browse available devices" else "$networkSourceCount saved source${if (networkSourceCount == 1) "" else "s"}",
                Icons.Default.CloudQueue,
                Modifier.weight(1f),
                onNavigateSmb
            )
            if (recentCount + (if (activeTask != null && recentCount < 2) 1 else 0) < 2) {
                AbstractMediaCard(
                    "Open a stream",
                    "Paste a direct URL",
                    Icons.Default.AddLink,
                    Modifier.weight(1f),
                    onOpenAddDialog
                )
            }
        }
    }
}

@Composable
private fun CinematicMediaCard(
    title: String,
    subtitle: String,
    @DrawableRes imageRes: Int,
    progress: Float,
    modifier: Modifier,
    onClick: () -> Unit
) {
    FocusableCard(
        onClick = onClick,
        modifier = modifier.height(165.dp),
        shape = RoundedCornerShape(18.dp),
        containerColor = SurfaceDark,
        focusedContainerColor = SurfaceDark,
        borderColor = TextPrimary.copy(alpha = 0.11f),
        scale = 1.035f
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(painterResource(imageRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.56f to BgDark.copy(alpha = 0.12f),
                        1f to BgDark.copy(alpha = 0.96f)
                    )
                )
            )
            MediaCardCopy(title, subtitle, progress, Modifier.align(Alignment.BottomStart))
        }
    }
}

@Composable
private fun AbstractMediaCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    FocusableCard(
        onClick = onClick,
        modifier = modifier.height(165.dp),
        shape = RoundedCornerShape(18.dp),
        containerColor = SurfaceDark,
        focusedContainerColor = CardDark,
        borderColor = TextPrimary.copy(alpha = 0.11f),
        scale = 1.035f
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(TextPrimary.copy(alpha = 0.12f), Color.Transparent))
            )
        ) {
            Icon(
                icon,
                null,
                tint = TextPrimary.copy(alpha = 0.18f),
                modifier = Modifier.align(Alignment.TopEnd).padding(15.dp).size(34.dp)
            )
            MediaCardCopy(title, subtitle, 0f, Modifier.align(Alignment.BottomStart))
        }
    }
}

@Composable
private fun MediaCardCopy(title: String, subtitle: String, progress: Float, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
        Text(
            title,
            color = TextPrimary,
            fontFamily = DwDisplayFont,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            subtitle,
            color = TextPrimary.copy(alpha = 0.66f),
            fontFamily = DwMonoFont,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (progress > 0f) {
            Spacer(Modifier.height(5.dp))
            Box(Modifier.fillMaxWidth().height(3.dp).background(TextPrimary.copy(alpha = 0.18f))) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(TextPrimary))
            }
        }
    }
}

private val fallbackImages = listOf(
    R.drawable.fallback_rain_room,
    R.drawable.fallback_coast,
    R.drawable.fallback_forest,
    R.drawable.fallback_platform,
    R.drawable.fallback_motel,
    R.drawable.fallback_lake
)

private fun PlaybackHistoryEntity.progressFraction(): Float =
    if (durationMs > 0) (lastPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

private fun PlaybackHistoryEntity.recentSubtitle(): String {
    val remainingMinutes = (durationMs - lastPositionMs).coerceAtLeast(0L) / 60_000
    val source = if (isSmb) "Network" else "TV archive"
    return if (remainingMinutes > 0) "$remainingMinutes min left · $source" else source
}

private fun String.cinematicTitle(): String {
    val withoutExtension = substringBeforeLast('.', this)
    val tokens = withoutExtension.split('.', '_', '-')
    val cleanTokens = tokens.takeWhile { token ->
        !token.matches(Regex("(13|19|20)\\d{2}")) &&
            !token.equals("DVDRip", true) &&
            !token.equals("BluRay", true) &&
            !token.equals("WEBRip", true)
    }
    return cleanTokens.joinToString(" ").trim().ifBlank { withoutExtension.replace('.', ' ') }
}

private fun Long.asCompactDuration(): String {
    val totalMinutes = this / 60_000
    if (this > 0L && totalMinutes == 0L) return "<1m"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
