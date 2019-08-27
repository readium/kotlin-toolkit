package org.readium.r2.streamer.parser

import android.util.Log
import org.json.JSONObject
import org.readium.r2.shared.*
import org.readium.r2.streamer.container.ContainerAudioBook
import java.io.File

import java.nio.charset.Charset

// Some constants useful to parse an DiViNa document
const val mimetypeAudiobook = "application/audiobook+zip"

/**
 *      DiViNaParser : Handle any DiViNa file. Opening, listing files
 *                  get name of the resource, creating the Publication
 *                  for rendering
 */

class AudioBookParser : PublicationParser {

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
     * This functions parse a maninest.json and build PubBox object from it
     */
    override fun parse(fileAtPath: String, title: String): PubBox? {

        //Building container
        val container = try {
            generateContainerFrom(fileAtPath)
        } catch (e: Exception) {
            Log.e("Error", "Could not generate container", e)
            return null
        }

        //Builing publication object from manifest.json
        var json: JSONObject?

        //Getting manifest.json
        var manifestByte = container?.data(manifestDotJSONPath)
        val stringManifest = manifestByte?.toString(Charset.defaultCharset())
        json = JSONObject(stringManifest)

        //Parsing manifest.json & building publication object
        val externalPub = parsePublication(json)
        var publication = externalPub

        publication.metadata.identifier = fileAtPath
        publication.type = Publication.TYPE.AUDIO

        //Modifying path of links
        for (counter in 0..(publication.readingOrder.size - 1)) {
            var tmp = fileAtPath + "/audiobook/" + publication.readingOrder[counter].href
            publication.readingOrder[counter].href = tmp
        }

        return PubBox(publication, container)

    }

}