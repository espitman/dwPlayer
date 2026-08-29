package com.dwplayer.core.player

import android.graphics.Color
import androidx.media3.ui.CaptionStyleCompat
import com.dwplayer.R

enum class SubtitleFont(val displayName: String, val fontResId: Int?) {
    VAZIRMATN("Vazirmatn (Default)", R.font.vazirmatn_medium),
    VAZIRMATN_BOLD("Vazirmatn Bold", R.font.vazirmatn_bold),
    SYSTEM_DEFAULT("System Default", null)
}

enum class SubtitleSize(val displayName: String, val spSize: Float) {
    SMALL("Small (18sp)", 18f),
    MEDIUM("Medium (24sp)", 24f),
    LARGE("Large (30sp)", 30f),
    EXTRA_LARGE("Extra Large (38sp)", 38f)
}

enum class SubtitleColor(val displayName: String, val colorInt: Int, val composeColor: Long) {
    WHITE("White", Color.WHITE, 0xFFFFFFFF),
    YELLOW("Yellow", 0xFFFDE047.toInt(), 0xFFFDE047),
    CYAN("Cyan", 0xFF38BDF8.toInt(), 0xFF38BDF8),
    GREEN("Green", 0xFF4ADE80.toInt(), 0xFF4ADE80)
}

enum class SubtitleBackgroundStyle(
    val displayName: String,
    val bgColorInt: Int,
    val edgeType: Int,
    val edgeColorInt: Int
) {
    OUTLINE_SHADOW(
        "Outline & Shadow",
        Color.TRANSPARENT,
        CaptionStyleCompat.EDGE_TYPE_OUTLINE,
        Color.BLACK
    ),
    TRANSLUCENT_BOX(
        "Translucent Box",
        0x99000000.toInt(),
        CaptionStyleCompat.EDGE_TYPE_NONE,
        Color.TRANSPARENT
    ),
    SOLID_BOX(
        "Solid Box",
        0xDD000000.toInt(),
        CaptionStyleCompat.EDGE_TYPE_NONE,
        Color.TRANSPARENT
    ),
    NONE(
        "None",
        Color.TRANSPARENT,
        CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
        Color.BLACK
    )
}

enum class SubtitlePosition(val displayName: String, val bottomFraction: Float) {
    BOTTOM("Bottom (Standard)", 0.06f),
    HIGHER("Higher", 0.14f),
    MID_BOTTOM("Mid-Bottom", 0.22f)
}

data class SubtitleSettings(
    val font: SubtitleFont = SubtitleFont.VAZIRMATN,
    val size: SubtitleSize = SubtitleSize.MEDIUM,
    val color: SubtitleColor = SubtitleColor.WHITE,
    val backgroundStyle: SubtitleBackgroundStyle = SubtitleBackgroundStyle.OUTLINE_SHADOW,
    val position: SubtitlePosition = SubtitlePosition.BOTTOM,
    val timeOffsetMs: Long = 0L
)
