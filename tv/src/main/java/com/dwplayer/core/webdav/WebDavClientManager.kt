package com.dwplayer.core.webdav

import android.util.Base64
import android.util.Log
import com.dwplayer.data.models.WebDavItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

@Singleton
class WebDavClientManager @Inject constructor() {

    companion object {
        private const val TAG = "WebDavClientManager"
        private val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "webm", "ts", "m4v", "flv", "wmv", "iso", "3gp", "vob"
        )
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    suspend fun testConnection(
        serverUrl: String,
        username: String?,
        password: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(serverUrl)
            val requestBuilder = Request.Builder()
                .url(normalizedUrl)
                .method("PROPFIND", createPropfindBody())
                .addHeader("Depth", "0")
                .addHeader("Content-Type", "application/xml; charset=utf-8")

            applyAuth(requestBuilder, username, password)

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            response.use { res ->
                if (res.isSuccessful || res.code == 207) {
                    Result.success("WebDAV connection successful (${res.code})")
                } else if (res.code == 401) {
                    Result.failure(Exception("Authentication failed (401 Unauthorized)"))
                } else {
                    Result.failure(Exception("WebDAV server returned HTTP ${res.code}: ${res.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun listFiles(
        serverUrl: String,
        subPath: String,
        username: String?,
        password: String?
    ): Result<List<WebDavItem>> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = normalizeUrl(serverUrl)
            val targetUrl = if (subPath.isBlank() || subPath == "/") {
                baseUrl
            } else {
                val cleanSub = subPath.trim().removePrefix("/").removeSuffix("/")
                if (baseUrl.endsWith("/")) "$baseUrl$cleanSub/" else "$baseUrl/$cleanSub/"
            }

            val requestBuilder = Request.Builder()
                .url(targetUrl)
                .method("PROPFIND", createPropfindBody())
                .addHeader("Depth", "1")
                .addHeader("Content-Type", "application/xml; charset=utf-8")

            applyAuth(requestBuilder, username, password)

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            response.use { res ->
                if (!res.isSuccessful && res.code != 207) {
                    return@withContext Result.failure(Exception("WebDAV listing failed with code ${res.code}: ${res.message}"))
                }

                val xmlBody = res.body?.string() ?: ""
                val items = parseWebDavXml(xmlBody, targetUrl, baseUrl)
                Result.success(items)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Listing files failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun applyAuth(requestBuilder: Request.Builder, username: String?, password: String?) {
        if (!username.isNullOrBlank()) {
            val credentials = "$username:${password ?: ""}"
            val encoded = Base64.encodeToString(credentials.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
            requestBuilder.addHeader("Authorization", "Basic $encoded")
        }
    }

    private fun createPropfindBody(): RequestBody {
        val xml = """<?xml version="1.0" encoding="utf-8" ?>
<D:propfind xmlns:D="DAV:">
  <D:prop>
    <D:resourcetype/>
    <D:getcontentlength/>
    <D:getlastmodified/>
    <D:displayname/>
  </D:prop>
</D:propfind>"""
        return xml.toRequestBody("application/xml; charset=utf-8".toMediaType())
    }

    private fun normalizeUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        if (!clean.endsWith("/")) {
            clean = "$clean/"
        }
        return clean
    }

    private fun parseWebDavXml(xml: String, currentTargetUrl: String, baseUrl: String): List<WebDavItem> {
        val result = mutableListOf<WebDavItem>()
        if (xml.isBlank()) return result

        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)))
            doc.documentElement.normalize()

            val responses = doc.getElementsByTagNameNS("*", "response")
            val targetPathNorm = normalizePath(currentTargetUrl)

            for (i in 0 until responses.length) {
                val node = responses.item(i)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    val elem = node as Element

                    val hrefRaw = getTagValueNS(elem, "href") ?: continue
                    val hrefDecoded = try {
                        URLDecoder.decode(hrefRaw, "UTF-8")
                    } catch (e: Exception) {
                        hrefRaw
                    }

                    val normalizedHref = normalizePath(hrefDecoded)

                    // Skip the current queried directory itself
                    if (normalizedHref == targetPathNorm) {
                        continue
                    }

                    // Check if directory / collection
                    val resourceTypeNodes = elem.getElementsByTagNameNS("*", "resourcetype")
                    var isDirectory = false
                    if (resourceTypeNodes.length > 0) {
                        val resTypeElem = resourceTypeNodes.item(0) as Element
                        val collectionNodes = resTypeElem.getElementsByTagNameNS("*", "collection")
                        isDirectory = collectionNodes.length > 0 || hrefDecoded.endsWith("/")
                    }

                    val contentLengthStr = getTagValueNS(elem, "getcontentlength")
                    val size = contentLengthStr?.toLongOrNull() ?: 0L

                    val lastModified = getTagValueNS(elem, "getlastmodified") ?: ""
                    val displayName = getTagValueNS(elem, "displayname")

                    val rawName = if (!displayName.isNullOrBlank()) {
                        displayName
                    } else {
                        hrefDecoded.trimEnd('/').substringAfterLast('/')
                    }

                    // Filter out empty or hidden files/folders (starting with .)
                    if (rawName.isBlank() || rawName.startsWith(".")) continue

                    val ext = rawName.substringAfterLast('.', "").lowercase()
                    val isVideo = !isDirectory && VIDEO_EXTENSIONS.contains(ext)

                    // Calculate relative path for subfolder navigation
                    val basePathComponent = try {
                        val uri = java.net.URI(baseUrl)
                        uri.path?.trim('/') ?: ""
                    } catch (e: Exception) {
                        ""
                    }

                    var cleanRel = hrefDecoded.trim()
                    if (cleanRel.startsWith("http://") || cleanRel.startsWith("https://")) {
                        cleanRel = try { java.net.URI(cleanRel).path?.trim('/') ?: "" } catch (e: Exception) { cleanRel.substringAfter("://").substringAfter("/", "").trim('/') }
                    } else {
                        cleanRel = cleanRel.trim('/')
                    }

                    if (basePathComponent.isNotEmpty() && cleanRel.startsWith(basePathComponent)) {
                        cleanRel = cleanRel.removePrefix(basePathComponent).trim('/')
                    }

                    val navPath = if (isDirectory) cleanRel else hrefDecoded

                    // Construct full video stream URL
                    val fullUrl = if (hrefRaw.startsWith("http://") || hrefRaw.startsWith("https://")) {
                        hrefRaw
                    } else {
                        val baseWithoutSlash = baseUrl.trimEnd('/')
                        val hostRoot = if (baseUrl.contains("://")) {
                            val scheme = baseUrl.substringBefore("://")
                            val hostPort = baseUrl.substringAfter("://").substringBefore("/")
                            "$scheme://$hostPort"
                        } else baseUrl

                        if (hrefRaw.startsWith("/")) {
                            "$hostRoot$hrefRaw"
                        } else {
                            "$baseWithoutSlash/$hrefRaw"
                        }
                    }

                    result.add(
                        WebDavItem(
                            name = rawName,
                            path = navPath,
                            fullUrl = fullUrl,
                            isDirectory = isDirectory,
                            size = size,
                            formattedSize = if (isDirectory) "Folder" else formatSize(size),
                            lastModified = lastModified,
                            isVideo = isVideo
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "XML parse error: ${e.message}", e)
        }

        // Sort folders first, then files alphabetically
        return result.sortedWith(
            compareByDescending<WebDavItem> { it.isDirectory }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun getTagValueNS(element: Element, tagName: String): String? {
        val list = element.getElementsByTagNameNS("*", tagName)
        if (list.length > 0) {
            val node = list.item(0)
            return node.textContent?.trim()
        }
        return null
    }

    private fun normalizePath(p: String): String {
        var clean = p.trim()
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            clean = clean.substringAfter("://").substringAfter("/", "")
        }
        return clean.trim('/')
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val formatted = DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble()))
        return "$formatted ${units[digitGroups]}"
    }
}
