package com.dwplayer.ui.player

import com.dwplayer.data.models.RemotePlayerStatus

/** Process-local bridge used by the companion HTTP service while PlayerActivity is active. */
object PlayerRemoteBridge {
    private var commandHandler: ((String) -> Boolean)? = null
    private var statusProvider: (() -> RemotePlayerStatus)? = null

    @Synchronized
    fun attach(handler: (String) -> Boolean, status: () -> RemotePlayerStatus) {
        commandHandler = handler
        statusProvider = status
    }

    @Synchronized
    fun detach(handler: (String) -> Boolean) {
        if (commandHandler === handler) {
            commandHandler = null
            statusProvider = null
        }
    }

    @Synchronized fun send(command: String): Boolean = commandHandler?.invoke(command) ?: false
    @Synchronized fun status(): RemotePlayerStatus = statusProvider?.invoke() ?: RemotePlayerStatus()
}
