package com.dwplayer.phone.core.media

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("dwshare_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FOLDER_URI = "key_folder_uri"
        private const val KEY_FOLDER_NAME = "key_folder_name"
    }

    fun saveFolder(uri: Uri, displayName: String) {
        prefs.edit()
            .putString(KEY_FOLDER_URI, uri.toString())
            .putString(KEY_FOLDER_NAME, displayName)
            .apply()
    }

    fun getFolderUri(): Uri? {
        val uriStr = prefs.getString(KEY_FOLDER_URI, null) ?: return null
        return try {
            Uri.parse(uriStr)
        } catch (e: Exception) {
            null
        }
    }

    fun getFolderName(): String {
        return prefs.getString(KEY_FOLDER_NAME, "No folder selected") ?: "No folder selected"
    }

    fun clearFolder() {
        prefs.edit().remove(KEY_FOLDER_URI).remove(KEY_FOLDER_NAME).apply()
    }

    fun hasSelectedFolder(): Boolean {
        return getFolderUri() != null
    }
}
