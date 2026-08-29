package com.dwplayer.core.player

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitlePreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "dwplayer_subtitles_pref"
        private const val KEY_FONT = "subtitle_font"
        private const val KEY_SIZE = "subtitle_size"
        private const val KEY_COLOR = "subtitle_color"
        private const val KEY_BG_STYLE = "subtitle_bg_style"
        private const val KEY_POSITION = "subtitle_position"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SubtitleSettings> = _settings.asStateFlow()

    private fun loadSettings(): SubtitleSettings {
        val fontName = prefs.getString(KEY_FONT, SubtitleFont.VAZIRMATN.name) ?: SubtitleFont.VAZIRMATN.name
        val sizeName = prefs.getString(KEY_SIZE, SubtitleSize.MEDIUM.name) ?: SubtitleSize.MEDIUM.name
        val colorName = prefs.getString(KEY_COLOR, SubtitleColor.WHITE.name) ?: SubtitleColor.WHITE.name
        val bgStyleName = prefs.getString(KEY_BG_STYLE, SubtitleBackgroundStyle.OUTLINE_SHADOW.name) ?: SubtitleBackgroundStyle.OUTLINE_SHADOW.name
        val posName = prefs.getString(KEY_POSITION, SubtitlePosition.BOTTOM.name) ?: SubtitlePosition.BOTTOM.name

        val font = try { SubtitleFont.valueOf(fontName) } catch (e: Exception) { SubtitleFont.VAZIRMATN }
        val size = try { SubtitleSize.valueOf(sizeName) } catch (e: Exception) { SubtitleSize.MEDIUM }
        val color = try { SubtitleColor.valueOf(colorName) } catch (e: Exception) { SubtitleColor.WHITE }
        val bgStyle = try { SubtitleBackgroundStyle.valueOf(bgStyleName) } catch (e: Exception) { SubtitleBackgroundStyle.OUTLINE_SHADOW }
        val pos = try { SubtitlePosition.valueOf(posName) } catch (e: Exception) { SubtitlePosition.BOTTOM }

        return SubtitleSettings(
            font = font,
            size = size,
            color = color,
            backgroundStyle = bgStyle,
            position = pos
        )
    }

    fun updateSettings(newSettings: SubtitleSettings) {
        prefs.edit()
            .putString(KEY_FONT, newSettings.font.name)
            .putString(KEY_SIZE, newSettings.size.name)
            .putString(KEY_COLOR, newSettings.color.name)
            .putString(KEY_BG_STYLE, newSettings.backgroundStyle.name)
            .putString(KEY_POSITION, newSettings.position.name)
            .apply()

        _settings.value = newSettings
    }

    fun updateFont(font: SubtitleFont) {
        updateSettings(_settings.value.copy(font = font))
    }

    fun updateSize(size: SubtitleSize) {
        updateSettings(_settings.value.copy(size = size))
    }

    fun updateColor(color: SubtitleColor) {
        updateSettings(_settings.value.copy(color = color))
    }

    fun updateBackgroundStyle(style: SubtitleBackgroundStyle) {
        updateSettings(_settings.value.copy(backgroundStyle = style))
    }

    fun updatePosition(pos: SubtitlePosition) {
        updateSettings(_settings.value.copy(position = pos))
    }
}
