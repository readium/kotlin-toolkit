package org.readium.r2.streamer.parser

import org.json.JSONObject
import org.readium.r2.shared.parsePublication
import org.readium.r2.streamer.container.ContainerAudioBook
import timber.log.Timber
import java.io.File
import java.net.URI
import java.nio.charset.Charset


/**
 *      AudiobookParser : Handle any Audiobook Package file. Opening, listing files
 *                  get name of the resource, creating the Publication
 *                  for rendering
 */

class AudioBookParser : PublicationParser {

    companion object {
        // Some constants useful to parse an DiViNa document
        const val mimetypeAudiobook = "application/audiobook+zip"
        const val manifestPath = "manifest.json"
    }

    /**
     * Check if path exist, generate a container for CBZ file
     *                   then check if creation was a success
     */
    private fun generateContainerFrom(path: String): ContainerAudioBook {
        val container: ContainerAudioBook?

        if (!File(path).exists())
            throw Exception("Missing File")
        container = ContainerAudioBook(path)
        if (!container.successCreated)
            throw Exception("Missing File")
        return container
    }

    /**
     * This functions parse a manifest.json and build PubBox object from it
     */
    override fun parse(fileAtPath: String, title: String): PubBox? {

        //Building container
        val container = try {
            generateContainerFrom(fileAtPath)
        } catch (e: Exception) {
            Timber.e(e, "Could not generate container")
            return null
        }
        val data = try {
            container.data(manifestPath)
        } catch (e: Exception) {
            Timber.e(e, "Missing File : $manifestPath")
            return null
        }

        //Building publication object from manifest.json
        //Getting manifest.json
        val stringManifest = data.toString(Charset.defaultCharset())
        val json = JSONObject(stringManifest)

        //Parsing manifest.json & building publication object
        val publication = parsePublication(json)

        //Modifying path of links
        for ((index, link) in publication.readingOrder.withIndex()) {
            val uri: String = if (URI(link.href).isAbsolute) {
                link.href!!
            } else {
                fileAtPath + "/" + link.href
            }
            publication.readingOrder[index].href = uri
        }

        return PubBox(publication, container)

    }

}