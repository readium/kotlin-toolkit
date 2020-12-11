/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.shared.extensions.readFully
import org.readium.r2.shared.extensions.readRange
import org.readium.r2.shared.extensions.tryOr
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.util.archive.Archive
import org.readium.r2.shared.util.archive.DefaultArchiveFactory
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*

/**
 * A companion type of [Sniffer] holding the type hints (file extensions, media types) and
 * providing an access to the file content.
 *
 * @param content Underlying content holder.
 * @param mediaTypes Media type hints.
 * @param fileExtensions File extension hints.
 */
class SnifferContext internal constructor(
    private val content: SnifferContent? = null,
    mediaTypes: List<String>,
    fileExtensions: List<String>
) {

    /** Media type hints. */
    val mediaTypes: List<MediaType> = mediaTypes
        .mapNotNull { MediaType.parse(it) }

    /** File extension hints. */
    val fileExtensions: List<String> = fileExtensions
        .map { it.toLowerCase(Locale.ROOT) }

    // Metadata

    /** Finds the first [Charset] declared in the media types' `charset` parameter. */
    val charset: Charset? by lazy {
        this.mediaTypes.mapNotNull { it.charset }.firstOrNull()
    }

    /** Returns whether this context has any of the given file extensions, ignoring case. */
    fun hasFileExtension(vararg fileExtensions: String): Boolean {
        for (fileExtension in fileExtensions) {
            if (this.fileExtensions.contains(fileExtension.toLowerCase(Locale.ROOT))) {
                return true
            }
        }
        return false
    }

    /**
     * Returns whether this context has any of the given media type, ignoring case and extra
     * parameters.
     *
     * Implementation note: Use [MediaType] to handle the comparison to avoid edge cases.
     */
    fun hasMediaType(vararg mediaTypes: String): Boolean {
        @Suppress("NAME_SHADOWING")
        val mediaTypes = mediaTypes.mapNotNull { MediaType.parse(it) }
        for (mediaType in mediaTypes) {
            if (this.mediaTypes.any { mediaType.contains(it) }) {
                return true
            }
        }
        return false
    }

    // Content

    /**
     * Content as plain text.
     *
     * It will extract the charset parameter from the media type hints to figure out an encoding.
     * Otherwise, fallback on UTF-8.
     */
    suspend fun contentAsString(): String? {
        if (!loadedContentAsString) {
            loadedContentAsString = true
            _contentAsString = content?.read()?.toString(charset ?: Charset.defaultCharset())
        }
        return _contentAsString
    }

    private var loadedContentAsString: Boolean = false
    private var _contentAsString: String? = null

    /** Content as an XML document. */
    suspend fun contentAsXml(): ElementNode? {
        if (!loadedContentAsXml) {
            loadedContentAsXml = true
            _contentAsXml = withContext(Dispatchers.IO) {
                try {
                    stream()?.let { XmlParser().parse(it) }
                } catch (e: Exception) {
                    null
                }
            }
        }

        return _contentAsXml
    }

    private var loadedContentAsXml: Boolean = false
    private var _contentAsXml: ElementNode? = null

    /**
     * Content as an Archive instance.
     * Warning: Archive is only supported for a local file, for now.
     */
    suspend fun contentAsArchive(): Archive? {
        if (!loadedContentAsArchive) {
            loadedContentAsArchive = true
            _contentAsArchive = withContext(Dispatchers.IO) {
                (content as? SnifferFileContent)?.let {
                    DefaultArchiveFactory().open(it.file, password = null)
                }
            }
        }

        return _contentAsArchive
    }

    private var loadedContentAsArchive: Boolean = false
    private var _contentAsArchive: Archive? = null

    /**
     * Content parsed from JSON.
     */
    suspend fun contentAsJson(): JSONObject? =
        try {
            contentAsString()?.let { JSONObject(it) }
        } catch (e: Exception) {
            null
        }

    /** Readium Web Publication Manifest parsed from the content. */
    suspend fun contentAsRwpm(): Manifest? =
        Manifest.fromJSON(contentAsJson())

    /**
     * Raw bytes stream of the content.
     *
     * A byte stream can be useful when sniffers only need to read a few bytes at the beginning of
     * the file.
     */
    suspend fun stream(): InputStream? = content?.stream()

    /**
     * Reads all the bytes or the given [range].
     *
     * It can be used to check a file signature, aka magic number.
     * See https://en.wikipedia.org/wiki/List_of_file_signatures
     */
    suspend fun read(range: LongRange? = null): ByteArray? =
        tryOrNull {
            if (range != null) stream()?.readRange(range)
            else stream()?.readFully()
        }

    /**
     * Returns whether the content is a JSON object containing all of the given root keys.
     */
    internal suspend fun containsJsonKeys(vararg keys: String): Boolean {
        val json = contentAsJson() ?: return false
        return json.keys().asSequence().toSet().containsAll(keys.toList())
    }

    /**
     * Returns whether an Archive entry exists in this file.
     */
    internal suspend fun containsArchiveEntryAt(path: String): Boolean =
        tryOrNull { contentAsArchive()?.entry(path) } != null

    /**
     * Returns the Archive entry data at the given [path] in this file.
     */
    internal suspend fun readArchiveEntryAt(path: String): ByteArray? {
        val archive = contentAsArchive() ?: return null

        return withContext(Dispatchers.IO) {
            tryOrNull { archive.entry(path).read() }
        }
    }

    /**
     * Returns whether all the Archive entry paths satisfy the given `predicate`.
     */
    internal suspend fun archiveEntriesAllSatisfy(predicate: (Archive.Entry) -> Boolean): Boolean =
        tryOr(false) { contentAsArchive()?.entries()?.all(predicate) == true }
}
