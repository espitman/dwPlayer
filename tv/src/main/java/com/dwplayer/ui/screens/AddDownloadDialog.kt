@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.dwplayer.ui.components.FocusableCard
import com.dwplayer.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AddUrlDrawer(
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    var inputUrl by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var drawerVisible by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val inputFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrimAlpha by animateFloatAsState(
        targetValue = if (drawerVisible) 0.72f else 0f,
        animationSpec = tween(260),
        label = "urlDrawerScrim"
    )

    fun closeDrawer(afterClose: (() -> Unit)? = null) {
        if (isClosing) return
        isClosing = true
        keyboardController?.hide()
        drawerVisible = false
        scope.launch {
            delay(260)
            onDismiss()
            afterClose?.invoke()
        }
    }

    fun submitUrl() {
        val value = inputUrl.trim()
        if (!value.matches(Regex("^https?://.+", RegexOption.IGNORE_CASE))) {
            validationMessage = "Enter a valid HTTP or HTTPS URL"
            scope.launch { inputFocusRequester.requestFocus() }
            return
        }

        validationMessage = null
        closeDrawer { onOpenUrl(value) }
    }

    LaunchedEffect(Unit) {
        drawerVisible = true
        delay(300)
        // Keep the initial TV view unobstructed. The URL field receives focus
        // through D-pad navigation and opens the system keyboard on demand.
        closeFocusRequester.requestFocus()
        keyboardController?.hide()
    }

    Dialog(
        onDismissRequest = { closeDrawer() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark.copy(alpha = scrimAlpha))
        ) {
            AnimatedVisibility(
                visible = drawerVisible,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.43f)
                    .fillMaxHeight(),
                enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300)
                ) + fadeIn(tween(180)),
                exit = slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(260)
                ) + fadeOut(tween(180))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceDark.copy(alpha = 0.98f))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(BorderDark)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(54.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Open a URL",
                                color = Color.White,
                                fontSize = 38.sp,
                                lineHeight = 42.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1.2).sp
                            )

                            FocusableCard(
                                onClick = { closeDrawer() },
                                modifier = Modifier
                                    .size(52.dp)
                                    .focusRequester(closeFocusRequester),
                                shape = RoundedCornerShape(16.dp),
                                containerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                borderColor = BorderDark,
                                focusedBorderColor = BorderDark,
                                scale = 1f
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White,
                                        modifier = Modifier.size(25.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(54.dp))

                        Text(
                            text = "Direct media or stream URL",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(66.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(BgDark)
                                .border(
                                    width = 3.dp,
                                    color = when {
                                        validationMessage != null -> AccentRose
                                        else -> AccentPrimary
                                    },
                                    shape = RoundedCornerShape(15.dp)
                                )
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = inputUrl,
                                onValueChange = {
                                    inputUrl = it
                                    validationMessage = null
                                },
                                singleLine = true,
                                cursorBrush = SolidColor(Color.White),
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.SansSerif
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { submitUrl() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(inputFocusRequester)
                            )

                            if (inputUrl.isEmpty()) {
                                Text(
                                    text = "https://example.com/video.mp4",
                                    color = TextTertiary,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = validationMessage
                                ?: "HTTP, HTTPS, HLS and direct video files are supported.",
                            color = if (validationMessage != null) AccentRose else TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 19.sp
                        )

                        Spacer(Modifier.weight(1f))

                        FocusableCard(
                            onClick = { submitUrl() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(15.dp),
                            containerColor = AccentPrimary,
                            focusedContainerColor = AccentSecondary,
                            contentColor = BgDark,
                            focusedContentColor = BgDark,
                            borderColor = Color.Transparent,
                            focusedBorderColor = Color.White,
                            scale = 1.025f
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Check and open",
                                    color = BgDark,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
