package com.dwplayer.phone.core.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NsdServerAdvertiser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "NsdServerAdvertiser"
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isRegistered = false

    fun startAdvertising(port: Int = 8085, customDeviceName: String? = null) {
        if (isRegistered) return

        try {
            nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
            val serviceName = customDeviceName ?: "${Build.MODEL} (dwShare)"

            val serviceInfo = NsdServiceInfo().apply {
                this.serviceName = serviceName
                serviceType = "_dwshare._tcp"
                this.port = port
            }

            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                    Log.i(TAG, "dwShare service registered on mDNS: ${NsdServiceInfo.serviceName}")
                    isRegistered = true
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "Service registration failed: $errorCode")
                    isRegistered = false
                }

                override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                    Log.i(TAG, "Service unregistered")
                    isRegistered = false
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "Service unregistration failed: $errorCode")
                }
            }

            nsdManager?.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register NSD service", e)
        }
    }

    fun stopAdvertising() {
        if (!isRegistered || registrationListener == null) return
        try {
            nsdManager?.unregisterService(registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering NSD service", e)
        } finally {
            isRegistered = false
            registrationListener = null
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving IP", e)
        }
        return "127.0.0.1"
    }
}
