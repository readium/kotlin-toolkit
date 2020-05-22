/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.server

import android.content.Context
import android.content.res.AssetManager
import org.nanohttpd.router.RouterNanoHTTPD
import org.readium.r2.shared.Injectable
import org.readium.r2.shared.publication.ContentLayout
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.BuildConfig.DEBUG
import org.readium.r2.streamer.container.Container
import org.readium.r2.streamer.fetcher.Fetcher
import org.readium.r2.streamer.server.handler.*
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.net.URLDecoder
import java.util.*

class Server(port: Int) : AbstractServer(port)

abstract class AbstractServer(private var port: Int) : RouterNanoHTTPD("127.0.0.1", port) {

    //    private val SEARCH_QUERY_HANDLE = "/search"
    private val MANIFEST_HANDLE = "/manifest"
    private val JSON_MANIFEST_HANDLE = "/manifest.json"
    private val MANIFEST_ITEM_HANDLE = "/(.*)"
    private val MEDIA_OVERLAY_HANDLE = "/media-overlay"
    private val CSS_HANDLE = "/"+ Injectable.Style.rawValue +"/(.*)"
    private val JS_HANDLE = "/"+ Injectable.Script.rawValue +"/(.*)"
    private val FONT_HANDLE = "/"+ Injectable.Font.rawValue +"/(.*)"
    private var containsMediaOverlay = false

    private val resources = Resources()
    private val customResources = Resources()

    private val fonts = Fonts()

    private fun addResource(name: String, body: String, custom: Boolean = false, injectable: Injectable? = null) {
        if (custom) {
            customResources.add(name, body, injectable)
        }
        resources.add(name, body)
    }

    private fun addFont(name: String, inputStream: InputStream, context: Context) {
        val dir = File(context.filesDir.path + "/" + Injectable.Font.rawValue + "/")
        dir.mkdirs()
        inputStream.toFile(context.filesDir.path + "/" + Injectable.Font.rawValue + "/" + name)
        val file = File(context.filesDir.path + "/" + Injectable.Font.rawValue + "/" + name)
        fonts.add(name, file)
    }

    fun loadReadiumCSSResources(assets: AssetManager) {
        fun load(name: String, layout: ContentLayout) {
            try {
                addResource("${layout.cssId}-${name}.css", Scanner(assets.open("static/readium-css/${layout.readiumCSSPath}ReadiumCSS-$name.css"), "utf-8")
                    .useDelimiter("\\A").next())
            } catch (e: IOException) {
                if (DEBUG) Timber.d(e)
            }
        }

        for (layout in ContentLayout.values()) {
            load("before", layout)
            load("default", layout)
            load("after", layout)
        }
    }

    private val ContentLayout.readiumCSSPath: String get() = when(this)  {
        ContentLayout.LTR -> ""
        ContentLayout.RTL -> "rtl/"
        ContentLayout.CJK_VERTICAL -> "cjk-vertical/"
        ContentLayout.CJK_HORIZONTAL -> "cjk-horizontal/"
    }

    fun loadR2ScriptResources(assets: AssetManager) {
        try {
            addResource("touchHandling.js", Scanner(assets.open(Injectable.Script.rawValue + "/touchHandling.js"), "utf-8")
                    .useDelimiter("\\A").next())
        } catch (e: IOException) {
            if (DEBUG) Timber.d(e)
        }
        try {
            addResource("utils.js", Scanner(assets.open(Injectable.Script.rawValue + "/utils.js"), "utf-8")
                    .useDelimiter("\\A").next())
        } catch (e: IOException) {
            if (DEBUG) Timber.d(e)
        }
    }
    fun loadR2FontResources(assets: AssetManager, context: Context) {
        try {
            addFont("OpenDyslexic-Regular.otf", assets.open("static/"+ Injectable.Font.rawValue +"/OpenDyslexic-Regular.otf"), context)
        } catch (e: IOException) {
            if (DEBUG) Timber.d(e)
        }
    }

    fun loadCustomResource(inputStream: InputStream, fileName: String, injectable: Injectable) {
        try {
            addResource(fileName, Scanner(inputStream, "utf-8").useDelimiter("\\A").next(), true, injectable)
        } catch (e: IOException) {
            if (DEBUG) Timber.d(e)
        }
    }

    fun loadCustomFont(inputStream: InputStream, context: Context, fileName: String) {
        try {
            addFont(fileName, inputStream, context)
        } catch (e: IOException) {
            if (DEBUG) Timber.d(e)
        }
    }

    fun addEpub(publication: Publication, container: Container, fileName: String, userPropertiesPath: String?) {
        if (container.rootFile.rootFilePath.isEmpty()) {
            return
        }
        val baseUrl = URL(Publication.localBaseUrlOf(filename = fileName, port = port))
        val fetcher = Fetcher(publication, container, userPropertiesPath, customResources)

        publication.setSelfLink("$baseUrl/manifest.json")

        // NanoHTTPD expects percent-decoded routes.
        val basePath =
            try { URLDecoder.decode(baseUrl.path, "UTF-8") }
            catch (e: Exception) { baseUrl.path }

        if (containsMediaOverlay) {
            addRoute(basePath + MEDIA_OVERLAY_HANDLE, MediaOverlayHandler::class.java, fetcher)
        }
        addRoute(basePath + JSON_MANIFEST_HANDLE, ManifestHandler::class.java, fetcher)
        addRoute(basePath + MANIFEST_HANDLE, ManifestHandler::class.java, fetcher)
        addRoute(basePath + MANIFEST_ITEM_HANDLE, ResourceHandler::class.java, fetcher)
        addRoute(JS_HANDLE, JSHandler::class.java, resources)
        addRoute(CSS_HANDLE, CSSHandler::class.java, resources)
        addRoute(FONT_HANDLE, FontHandler::class.java, fonts)
    }

    /* FIXME: To review once the media-overlays will be supported in the Publication model
         private fun addLinks(publication: Publication, filePath: String) {
        containsMediaOverlay = false
        for (link in publication.otherLinks) {
            if (link.rel.contains("media-overlay")) {
                containsMediaOverlay = true
                link.href = link.href?.replace("port", "127.0.0.1:$listeningPort$filePath")
            }
        }
    } */

    private fun InputStream.toFile(path: String) {
        use { input ->
            File(path).outputStream().use { input.copyTo(it) }
        }
    }

}

