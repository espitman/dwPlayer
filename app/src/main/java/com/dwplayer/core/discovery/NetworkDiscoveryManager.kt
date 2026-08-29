package com.dwplayer.core.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.dwplayer.data.models.DiscoveredServerDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkDiscoveryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NetworkDiscoveryManager"
        private val SERVICE_TYPES = listOf(
            "_dwshare._tcp.",
            "_webdav._tcp.",
            "_http._tcp."
        )
    }

    private val nsdManager: NsdManager? by lazy {
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _discoveredServers = MutableStateFlow<List<DiscoveredServerDto>>(emptyList())
    val discoveredServers: StateFlow<List<DiscoveredServerDto>> = _discoveredServers.asStateFlow()

    private val activeListeners = mutableMapOf<String, NsdManager.DiscoveryListener>()
    private var isScanning = false

    fun startDiscovery() {
        if (isScanning) return
        isScanning = true

        SERVICE_TYPES.forEach { serviceType ->
            try {
                val listener = createDiscoveryListener(serviceType)
                activeListeners[serviceType] = listener
                nsdManager?.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
                Log.d(TAG, "Started discovery for: $serviceType")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start discovery for $serviceType: ${e.message}")
            }
        }
    }

    fun stopDiscovery() {
        if (!isScanning) return
        isScanning = false

        activeListeners.forEach { (type, listener) ->
            try {
                nsdManager?.stopServiceDiscovery(listener)
                Log.d(TAG, "Stopped discovery for: $type")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop discovery for $type: ${e.message}")
            }
        }
        activeListeners.clear()
    }

    private fun createDiscoveryListener(serviceType: String): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "onStartDiscoveryFailed: $serviceType, code: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "onStopDiscoveryFailed: $serviceType, code: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d(TAG, "Discovery started: $serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d(TAG, "Discovery stopped: $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) return
                Log.d(TAG, "Service found: ${serviceInfo.serviceName} (${serviceInfo.serviceType})")

                try {
                    nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                            Log.w(TAG, "Resolve failed for ${serviceInfo?.serviceName}, code: $errorCode")
                        }

                        override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                            if (resolvedInfo == null) return
                            val hostAddress = resolvedInfo.host?.hostAddress ?: return
                            val port = resolvedInfo.port
                            val name = resolvedInfo.serviceName ?: "Unknown Server"
                            val type = resolvedInfo.serviceType ?: serviceType

                            val deviceType = when {
                                type.contains("dwshare", ignoreCase = true) -> "dwShare Phone"
                                type.contains("webdav", ignoreCase = true) -> "WebDAV Server / NAS"
                                else -> "Local Media Server"
                            }

                            val serverUrl = "http://$hostAddress:$port/"
                            val dto = DiscoveredServerDto(
                                serviceName = name,
                                serviceType = type,
                                host = hostAddress,
                                port = port,
                                url = serverUrl,
                                deviceType = deviceType
                            )

                            _discoveredServers.update { current ->
                                val filtered = current.filterNot { it.host == hostAddress && it.port == port }
                                filtered + dto
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Error resolving service: ${e.message}")
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) return
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                _discoveredServers.update { current ->
                    current.filterNot { it.serviceName == serviceInfo.serviceName }
                }
            }
        }
    }
}
