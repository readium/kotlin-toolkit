/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.format

import org.json.JSONObject
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.shared.publication.Publication
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile


/**
 * A companion type of [Format.Sniffer] holding the type hints (file extensions, media types) and
 * providing an access to the file content.
 *
 * @param content Underlying content holder.
 * @param mediaTypes Media type hints.
 * @param fileExtensions File extension hints.
 */
class FormatSnifferContext internal constructor(
    private val content: FormatSnifferContent? = null,
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
    val contentAsString: String? by lazy {
        content?.read()?.toString(charset ?: Charset.defaultCharset())
    }

    /** Content as an XML document. */
    val contentAsXml: ElementNode? by lazy {
        try {
            stream()?.let { XmlParser().parse(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Content as a ZIP archive.
     * Warning: ZIP is only supported for a local file, for now.
     */
    val contentAsZip: ZipFile? by lazy {
        try {
            (content as? FormatSnifferFileContent)?.let {
                ZipFile(it.file)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Content parsed from JSON.
     */
    val contentAsJson: JSONObject? by lazy {
        try {
            contentAsString?.let { JSONObject(it) }
        } catch (e: Exception) {
            null
        }
    }

    /** Publication parsed from the content. */
    val contentAsRwpm: Publication? by lazy {
        Publication.fromJSON(contentAsJson)
    }

    /**
     * Raw bytes stream of the content.
     *
     * A byte stream can be useful when sniffers only need to read a few bytes at the beginning of
     * the file.
     */
    fun stream(): InputStream? = content?.stream()

    /**
     * Reads the file signature, aka magic number, at the beginning of the content, up to [length]
     * bytes.
     *
     * i.e. https://en.wikipedia.org/wiki/List_of_file_signatures
     */
    fun readFileSignature(length: Int): String? =
        try {
            stream()?.let {
                val buffer = ByteArray(length)
                it.read(buffer, 0, length)
                String(buffer)
            }
        } catch (e: Exception) {
            null
        }

    /**
     * Returns whether a ZIP entry exists in this file.
     */
    internal fun containsZipEntryAt(path: String): Boolean =
        try {
            contentAsZip?.getEntry(path) != null
        } catch (e: Exception) {
            false
        }

    /**
     * Returns the ZIP entry data at the given [path] in this file.
     */
    internal fun readZipEntryAt(path: String): ByteArray? {
        val archive = contentAsZip ?: return null
        return try {
            val entry = archive.getEntry(path)
            archive.getInputStream(entry).readBytes()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns whether all the ZIP entry paths satisfy the given `predicate`.
     */
    internal fun zipEntriesAllSatisfy(predicate: (ZipEntry) -> Boolean): Boolean =
        try {
            contentAsZip?.entries()?.asSequence()?.all(predicate) == true
        } catch (e: Exception) {
            false
        }

}
