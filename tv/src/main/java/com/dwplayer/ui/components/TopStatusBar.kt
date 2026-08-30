@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.dwplayer.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TopStatusBar(
    companionUrl: String,
    modifier: Modifier = Modifier
) {
    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30000L)
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
    }

    val isConnected = companionUrl.isNotBlank() && companionUrl.contains(":")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(start = 36.dp, end = 42.dp, top = 18.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Wordmark (dwPlayer TV)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "dwPlayer",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = DwDisplayFont,
                letterSpacing = (-0.8).sp
            )
            Text(
                text = "TV",
                color = Color(0xFF7A857E),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DwMonoFont,
                letterSpacing = 1.sp
            )
        }

        // Right: Remote Status & Clock (● Web remote connected 20:07)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color.White else Color(0xFF5A635E))
                )
                Text(
                    text = if (isConnected) "Web remote connected" else "Web remote offline",
                    color = Color(0xFFA8B2AB),
                    fontSize = 13.sp,
                    fontFamily = DwBodyFont,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = currentTime,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DwMonoFont
            )
        }
    }
}
