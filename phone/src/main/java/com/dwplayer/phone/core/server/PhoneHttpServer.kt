package com.dwplayer.phone.core.server

import android.content.Context
import android.util.Log
import com.dwplayer.phone.core.media.PhoneMediaScanner
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneHttpServer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaScanner: PhoneMediaScanner
) {
    private val TAG = "PhoneHttpServer"
    private var engine: NettyApplicationEngine? = null
    var isRunning = false
        private set

    fun start(port: Int = 8085) {
        if (isRunning) return

        try {
            engine = embeddedServer(Netty, port = port) {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
                install(CORS) {
                    anyHost()
                    allowHeader(HttpHeaders.ContentType)
                    allowHeader(HttpHeaders.Authorization)
                    allowHeader(HttpHeaders.Range)
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Post)
                    allowMethod(HttpMethod.Options)
                }
                install(PartialContent)
                install(AutoHeadResponse)
                install(StatusPages) {
                    exception<Throwable> { call, cause ->
                        Log.e("PhoneHttpServer", "Unhandled error", cause)
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "Unknown error")))
                    }
                }

                routing {
                    // WebDAV PROPFIND support for TV Player integration
                    route("{...}") {
                        handle {
                            if (call.request.httpMethod.value.equals("PROPFIND", ignoreCase = true)) {
                                val videos = mediaScanner.getVideos()
                                val sb = StringBuilder()
                                sb.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n")
                                sb.append("<D:multistatus xmlns:D=\"DAV:\">\n")

                                // Root collection
                                sb.append("  <D:response>\n")
                                sb.append("    <D:href>/</D:href>\n")
                                sb.append("    <D:propstat>\n")
                                sb.append("      <D:prop>\n")
                                sb.append("        <D:displayname>dwShare Videos</D:displayname>\n")
                                sb.append("        <D:resourcetype><D:collection/></D:resourcetype>\n")
                                sb.append("      </D:prop>\n")
                                sb.append("      <D:status>HTTP/1.1 200 OK</D:status>\n")
                                sb.append("    </D:propstat>\n")
                                sb.append("  </D:response>\n")

                                for (v in videos) {
                                    val safeHref = "/api/stream/${v.id}"
                                    sb.append("  <D:response>\n")
                                    sb.append("    <D:href>$safeHref</D:href>\n")
                                    sb.append("    <D:propstat>\n")
                                    sb.append("      <D:prop>\n")
                                    sb.append("        <D:displayname>${escapeXml(v.title)}</D:displayname>\n")
                                    sb.append("        <D:getcontentlength>${v.size}</D:getcontentlength>\n")
                                    sb.append("        <D:getcontenttype>${v.mimeType}</D:getcontenttype>\n")
                                    sb.append("        <D:resourcetype/>\n")
                                    sb.append("      </D:prop>\n")
                                    sb.append("      <D:status>HTTP/1.1 200 OK</D:status>\n")
                                    sb.append("    </D:propstat>\n")
                                    sb.append("  </D:response>\n")
                                }
                                sb.append("</D:multistatus>")

                                call.respondText(
                                    text = sb.toString(),
                                    contentType = ContentType.parse("application/xml; charset=utf-8"),
                                    status = HttpStatusCode(207, "Multi-Status")
                                )
                            }
                        }
                    }

                    // REST Media API
                    get("/api/media") {
                        val videos = mediaScanner.getVideos()
                        val result = videos.map { v ->
                            mapOf(
                                "id" to v.id.toString(),
                                "title" to v.title,
                                "size" to v.size,
                                "durationMs" to v.durationMs,
                                "streamUrl" to "/api/stream/${v.id}",
                                "mimeType" to v.mimeType
                            )
                        }
                        call.respond(result)
                    }

                    // Direct Stream with HTTP 206 Partial Content
                    get("/api/stream/{id}") {
                        val idStr = call.parameters["id"]
                        val id = idStr?.toLongOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, "Invalid media ID")
                            return@get
                        }

                        val video = mediaScanner.getVideos().find { it.id == id }
                        if (video != null && video.path.isNotEmpty()) {
                            val file = File(video.path)
                            if (file.exists() && file.canRead()) {
                                call.respondFile(file)
                                return@get
                            }
                        }

                        // Fallback open via ContentResolver stream
                        call.respond(HttpStatusCode.NotFound, "File not found or inaccessible")
                    }

                    // Web Browser Companion Dashboard
                    get("/") {
                        val videos = mediaScanner.getVideos()
                        val html = buildWebDashboardHtml(videos)
                        call.respondText(html, ContentType.Text.Html)
                    }
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    engine?.start(wait = true)
                } catch (e: Exception) {
                    Log.e(TAG, "Server run exception", e)
                }
            }
            isRunning = true
            Log.i(TAG, "dwShare HTTP Server started on port $port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start HTTP server", e)
            isRunning = false
        }
    }

    fun stop() {
        if (!isRunning) return
        try {
            engine?.stop(1000, 2000)
            engine = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        } finally {
            isRunning = false
        }
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun buildWebDashboardHtml(videos: List<com.dwplayer.phone.core.media.MediaItem>): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>dwShare - Phone Media Server</title>
              <script src="https://cdn.tailwindcss.com"></script>
            </head>
            <body class="bg-slate-950 text-slate-100 min-h-screen p-6 font-sans">
              <div class="max-w-4xl mx-auto space-y-6">
                <header class="flex items-center justify-between border-b border-slate-800 pb-4">
                  <div>
                    <h1 class="text-2xl font-black text-blue-400">dwShare Media Server</h1>
                    <p class="text-xs text-slate-400">Streaming videos directly from Android Phone to Android TV & Web</p>
                  </div>
                  <span class="px-3 py-1 bg-emerald-950 text-emerald-300 border border-emerald-800 rounded-full text-xs font-bold">
                    ● Server Active
                  </span>
                </header>

                <div class="space-y-3">
                  <h2 class="text-sm font-bold text-slate-300">Available Videos (${videos.size})</h2>
                  ${if (videos.isEmpty()) """
                    <div class="p-8 text-center bg-slate-900 rounded-2xl border border-slate-800 text-slate-500 text-sm">
                      No video files found on this phone yet.
                    </div>
                  """ else videos.map { v -> """
                    <div class="p-4 bg-slate-900 border border-slate-800 rounded-xl flex items-center justify-between gap-4">
                      <div>
                        <h3 class="text-sm font-bold text-white">${v.title}</h3>
                        <p class="text-xs text-slate-400 font-mono">${formatSize(v.size)}</p>
                      </div>
                      <a href="/api/stream/${v.id}" target="_blank" class="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-lg text-xs font-bold transition">
                        Stream / Download
                      </a>
                    </div>
                  """ }.joinToString("\n")}
                </div>
              </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        val gb = mb / 1024.0
        return if (gb >= 1.0) String.format("%.2f GB", gb) else String.format("%.1f MB", mb)
    }
}
