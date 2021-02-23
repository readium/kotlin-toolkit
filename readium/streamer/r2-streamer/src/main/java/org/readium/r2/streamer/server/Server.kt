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
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.BuildConfig.DEBUG
import org.readium.r2.streamer.container.Container
import org.readium.r2.streamer.server.handler.*
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.net.URLDecoder
import java.util.*

class Server(port: Int, context: Context) : AbstractServer(port, context.applicationContext)

abstract class AbstractServer(private var port: Int, private val context: Context) : RouterNanoHTTPD("127.0.0.1", port) {

    private val MANIFEST_HANDLE = "/manifest"
    private val JSON_MANIFEST_HANDLE = "/manifest.json"
    private val MANIFEST_ITEM_HANDLE = "/(.*)"
    private val MEDIA_OVERLAY_HANDLE = "/media-overlay"
    private val CSS_HANDLE = "/"+ Injectable.Style.rawValue +"/(.*)"
    private val JS_HANDLE = "/"+ Injectable.Script.rawValue +"/(.*)"
    private val FONT_HANDLE = "/"+ Injectable.Font.rawValue +"/(.*)"
    private val ASSETS_HANDLE = "/assets/(.*)"
    private var containsMediaOverlay = false

    private val resources = Resources()
    private val customResources = Resources()
    private val assets = Assets(context.assets, basePath = "/assets/")
    private val fonts = Files(basePath = "/${Injectable.Style}/")

    init {
        assets.add(href = "readium-css", path = "readium/readium-css")
        assets.add(href = "scripts", path = "readium/scripts")
        assets.add(href = "fonts", path = "readium/fonts")
    }

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
        fonts[name] = file
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

    fun addPublication(publication: Publication, userPropertiesFile: File?): URL? {
        return addPublication(publication, null, "/${UUID.randomUUID()}", userPropertiesFile?.path)
    }

    private fun addPublication(publication: Publication, container: Container?, filename: String, userPropertiesPath: String?): URL? {
        if (container?.rootFile?.rootFilePath?.isEmpty() == true) {
            return null
        }
        val baseUrl = URL(Publication.localBaseUrlOf(filename = filename, port = port))
        val fetcher = ServingFetcher(
            publication,
            userPropertiesPath,
            customResources
        )

        // NanoHTTPD expects percent-decoded routes.
        val basePath =
            try { URLDecoder.decode(baseUrl.path, "UTF-8") }
            catch (e: Exception) { baseUrl.path }

        if (containsMediaOverlay) {
            setRoute(basePath + MEDIA_OVERLAY_HANDLE, MediaOverlayHandler::class.java, fetcher)
        }
        setRoute(basePath + JSON_MANIFEST_HANDLE, ManifestHandler::class.java, fetcher)
        setRoute(basePath + MANIFEST_HANDLE, ManifestHandler::class.java, fetcher)
        setRoute(basePath + MANIFEST_ITEM_HANDLE, PublicationResourceHandler::class.java, fetcher)
        setRoute(ASSETS_HANDLE, AssetHandler::class.java, assets)
        setRoute(JS_HANDLE, ResourceHandler::class.java, resources)
        setRoute(CSS_HANDLE, ResourceHandler::class.java, resources)
        setRoute(FONT_HANDLE, FileHandler::class.java, fonts)

        return baseUrl
    }

    @Deprecated("Use the easier-to-use addPublication()", replaceWith = ReplaceWith("this.addPublication(publication, userPropertiesFile = File(userPropertiesPath))"), level = DeprecationLevel.ERROR)
    fun addEpub(publication: Publication, fileName: String, userPropertiesPath: String?) {
        addPublication(publication, null, filename = fileName, userPropertiesPath = userPropertiesPath)
    }

    @Deprecated("Use the easier-to-use addPublication()", replaceWith = ReplaceWith("this.addPublication(publication, userPropertiesFile = File(userPropertiesPath))"), level = DeprecationLevel.ERROR)
    fun addEpub(publication: Publication, container: Container?, fileName: String, userPropertiesPath: String?) {
        addPublication(publication, container, filename = fileName, userPropertiesPath = userPropertiesPath)
    }

    private fun setRoute(url: String, handler: Class<*>, vararg initParameter: Any) {
        try { removeRoute(url) } catch (e: Exception) {}
        addRoute(url, handler, *initParameter)
    }

    // FIXME: To review once the media-overlays will be supported in the Publication model
//    private fun addLinks(publication: Publication, filePath: String) {
//        containsMediaOverlay = false
//        for (link in publication.otherLinks) {
//            if (link.rel.contains("media-overlay")) {
//                containsMediaOverlay = true
//                link.href = link.href?.replace("port", "127.0.0.1:$listeningPort$filePath")
//            }
//        }
//    }

    private fun InputStream.toFile(path: String) {
        use { input ->
            File(path).outputStream().use { input.copyTo(it) }
        }
    }

    @Deprecated("This is not needed anymore")
    fun loadReadiumCSSResources(assets: AssetManager) {}
    @Deprecated("This is not needed anymore")
    fun loadR2ScriptResources(assets: AssetManager) {}
    @Deprecated("This is not needed anymore")
    fun loadR2FontResources(assets: AssetManager, context: Context) {}

}

