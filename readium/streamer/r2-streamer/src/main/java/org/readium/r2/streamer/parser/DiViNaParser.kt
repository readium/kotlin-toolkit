// TODO WIP

/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser

import android.util.Log
import android.webkit.MimeTypeMap
import com.mcxiaoke.koi.ext.close
import org.json.JSONObject
import org.readium.r2.shared.*
import java.io.File
import org.readium.r2.streamer.container.ContainerCbz
import org.readium.r2.streamer.container.ContainerDiViNa
import org.zeroturnaround.zip.ZipUtil
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

// Some constants useful to parse an DiViNa document
const val mimetypeDiViNa = "application/divina+zip"

//const val mimetypeJPEG = "image/jpeg"
//const val mimetypePNG = "image/png"

/**
 *      DiViNaParser : Handle any DiViNa file. Opening, listing files
 *                  get name of the resource, creating the Publication
 *                  for rendering
 */

class DiViNaParser : PublicationParser {

    /**
     * Check if path exist, generate a container for CBZ file
     *                   then check if creation was a success
     */
    private fun generateContainerFrom(path: String): ContainerDiViNa {
        val container: ContainerDiViNa?

        if (!File(path).exists())
            throw Exception("Missing File")
        container = ContainerDiViNa(path)
        if (!container.successCreated)
            throw Exception("Missing File")
        return container
    }

    /**
     *
     */
    override fun parse(fileAtPath: String, title: String): PubBox? {
        val container = try {
            generateContainerFrom(fileAtPath)
        } catch (e: Exception) {
            Log.e("Error", "Could not generate container", e)
            return null
        }
//        val listFiles = try {
//            container.getFilesList()
//        } catch (e: Exception) {
//            Log.e("Error", "Missing File : META-INF/container.xml", e)
//            return null
//        }


//        if (listFiles.contains("publication.json")) {
//
//            val wpm = listFiles.get(listFiles.indexOf("publication.json"))
//            val link = Link()
//            link.typeLink = getMimeType(wpm)
//            link.href = wpm
//            var publication:Publication? = null
//
////            task {
//
//                val json = getPublicationURL(link.href!!, fileAtPath)
//
////            } then { json ->
//
//                json?.let {
//                    publication = parsePublication(json)
//                }
////            }
//            publication?.type = Publication.TYPE.DiViNa
//
//            return PubBox(publication!!, container)
//
//        } else {
            val publication = Publication()
//            listFiles.forEach {
//                val link = Link()
//
//                link.typeLink = getMimeType(it)
//                link.href = it
//
//                if (getMimeType(it) == mimetypeJPEG || getMimeType(it) == mimetypePNG) {
//                    publication.readingOrder.add(link)
//                }
//
//            }
            publication.metadata.identifier = fileAtPath
            publication.type = Publication.TYPE.DiViNa
            return PubBox(publication, container)
//        }

    }
    private fun getPublicationURL(src: String, fileAtPath: String): JSONObject? {
        return try {

            val blob = ZipUtil.unpackEntry(File(fileAtPath), src)
            blob?.let { jsonManifest ->

                val stringManifest = jsonManifest.toString(Charset.defaultCharset())
                val json = JSONObject(stringManifest)
                json
            }

//            val url = URL(src)
//            val connection = url.openConnection() as HttpURLConnection
//            connection.instanceFollowRedirects = false
//            connection.doInput = true
//            connection.connect()
//
//            val jsonManifestURL = URL(connection.getHeaderField("Location") ?: src).openConnection()
//            jsonManifestURL.connect()
//
//            val jsonManifest = jsonManifestURL.getInputStream().readBytes()
//            val stringManifest = jsonManifest.toString(Charset.defaultCharset())
//            val json = JSONObject(stringManifest)
//
//            jsonManifestURL.close()
//            connection.disconnect()
//            connection.close()
//
//            json
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }


    private fun getMimeType(fileName: String): String? {
        return try {
            val name = fileName.replace(" ", "").replace("'", "").replace(",", "")
            val extension = MimeTypeMap.getFileExtensionFromUrl(name)
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } catch (e: Exception) {
            Log.e("Error", "Something went wrong while getMimeType() : ${e.message}")
            null
        }
    }

}