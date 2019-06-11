/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser

import android.webkit.MimeTypeMap
import org.readium.r2.shared.Link
import org.readium.r2.shared.Publication
import org.readium.r2.streamer.container.ContainerCbz
import timber.log.Timber
import java.io.File

// Some constants useful to parse an Cbz document
const val mimetypeCBZ = "application/vnd.comicbook+zip"
const val mimetypeCBR = "application/x-cbr"

const val mimetypeJPEG = "image/jpeg"
const val mimetypePNG = "image/png"

/**
 *      CbzParser : Handle any CBZ file. Opening, listing files
 *                  get name of the resource, creating the Publication
 *                  for rendering
 */

class CbzParser : PublicationParser {

    /**
     * Check if path exist, generate a container for CBZ file
     *                   then check if creation was a success
     */
    private fun generateContainerFrom(path: String): ContainerCbz {
        val container: ContainerCbz?

        if (!File(path).exists())
            throw Exception("Missing File")
        container = ContainerCbz(path)
        if (!container.successCreated)
            throw Exception("Missing File")
        return container
    }

    //TODO Comment that code
    /**
     *
     */
    override fun parse(fileAtPath: String, title: String): PubBox? {
        val container = try {
            generateContainerFrom(fileAtPath)
        } catch (e: Exception) {
            Timber.e(e,  "Could not generate container")
            return null
        }
        val listFiles = try {
            container.getFilesList()
        } catch (e: Exception) {
            Timber.e(e, "Missing File : META-INF/container.xml")
            return null
        }

        val publication = Publication()

        listFiles.forEach {
            val link = Link()

            link.typeLink = getMimeType(it)
            link.href = it

            if (getMimeType(it) == mimetypeJPEG || getMimeType(it) == mimetypePNG) {
                publication.pageList.add(link)
            } else {
                publication.resources.add(link)         //List of eventual extra files ( .nfo, ect .. )
            }
        }
        publication.pageList.first().rel.add("cover")
        publication.metadata.identifier = fileAtPath
//        publication.metadata.multilanguageTitle = MultilanguageString()
//        publication.metadata.multilanguageTitle?.singleString = container.title
//        publication.metadata.title = container.getTitle()
        publication.type = Publication.TYPE.CBZ
        return PubBox(publication, container)
    }

    private fun getMimeType(fileName: String): String? {
        return try {
            val name = fileName.replace(" ", "").replace("'", "").replace(",", "")
            val extension = MimeTypeMap.getFileExtensionFromUrl(name)
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    /**
     * List all CBZ files in a particular path
     *
     * @return listCBZ: List<String>
     */
//    fun getCbzFiles(path: String): List<String>{
//        var listCBZ = emptyList<String>()
//        try {
//            val file = File(path)
//            listCBZ = file.list().filter { it.endsWith(".cbz") }
//        } catch (e: Exception){
//            Log.e("Error", "Can't open the file (${e.message})")
//        }
//        return listCBZ
//    }

}