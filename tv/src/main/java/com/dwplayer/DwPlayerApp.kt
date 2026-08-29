package com.dwplayer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DwPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
