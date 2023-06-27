/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.resource.ArchiveFactory
import org.readium.r2.shared.resource.BytesResource
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.ContainerFactory
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceFactory
import org.readium.r2.shared.resource.ResourceInputStream
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getOrElse
import timber.log.Timber

sealed class SnifferContext(
    mediaTypes: List<String> = emptyList(),
    fileExtensions: List<String> = emptyList()
) {
    /** Media type hints. */
    val mediaTypes: List<MediaType> = mediaTypes
        .mapNotNull { MediaType.parse(it) }

    /** File extension hints. */
    val fileExtensions: List<String> = fileExtensions
        .map { it.lowercase(Locale.ROOT) }

    /** Finds the first [Charset] declared in the media types' `charset` parameter. */
    val charset: Charset? get() =
        this.mediaTypes.firstNotNullOfOrNull { it.charset }

    /** Returns whether this context has any of the given file extensions, ignoring case. */
    fun hasFileExtension(vararg fileExtensions: String): Boolean {
        for (fileExtension in fileExtensions) {
            if (this.fileExtensions.contains(fileExtension.lowercase(Locale.ROOT))) {
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

    abstract suspend fun release()
}

class HintSnifferContext(
    mediaTypes: List<String> = emptyList(),
    fileExtensions: List<String> = emptyList()
) : SnifferContext(mediaTypes, fileExtensions) {

    override suspend fun release() {}
}

sealed class ContentAwareSnifferContext(
    mediaTypes: List<String> = emptyList(),
    fileExtensions: List<String> = emptyList()
) : SnifferContext(mediaTypes, fileExtensions)

/**
 * A companion type of [Sniffer] holding the type hints (file extensions, media types) and
 * providing an access to the file content.
 *
 * @param resource Underlying content holder.
 * @param mediaTypes Media type hints.
 * @param fileExtensions File extension hints.
 */
class ResourceSnifferContext internal constructor(
    val resource: Resource,
    mediaTypes: List<String> = emptyList(),
    fileExtensions: List<String> = emptyList()
) : ContentAwareSnifferContext(mediaTypes, fileExtensions) {

    /**
     * Content as plain text.
     *
     * It will extract the charset parameter from the media type hints to figure out an encoding.
     * Otherwise, fallback on UTF-8.
     */
    suspend fun contentAsString(): String? =
        try {
            if (!loadedContentAsString) {
                loadedContentAsString = true
                _contentAsString = resource
                    .readAsString(charset ?: Charset.defaultCharset())
                    .getOrNull()
            }
            _contentAsString
        } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
            Timber.e(e)
            null
        }

    private var loadedContentAsString: Boolean = false
    private var _contentAsString: String? = null

    /** Content as an XML document. */
    suspend fun contentAsXml(): ElementNode? {
        if (!loadedContentAsXml) {
            loadedContentAsXml = true
            _contentAsXml = withContext(Dispatchers.IO) {
                try {
                    resource.readAsXml().getOrNull()
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
    suspend fun contentAsStream(): InputStream? =
        ResourceInputStream(resource)

    /**
     * Reads all the bytes or the given [range].
     *
     * It can be used to check a file signature, aka magic number.
     * See https://en.wikipedia.org/wiki/List_of_file_signatures
     */
    suspend fun read(range: LongRange? = null): ByteArray? =
        resource.read(range).getOrNull()

    /**
     * Returns whether the content is a JSON object containing all of the given root keys.
     */
    internal suspend fun containsJsonKeys(vararg keys: String): Boolean {
        val json = contentAsJson() ?: return false
        return json.keys().asSequence().toSet().containsAll(keys.toList())
    }

    override suspend fun release() {
        resource.close()
    }
}

/**
 * A companion type of [Sniffer] holding the type hints (file extensions, media types) and
 * providing an access to the file content.
 *
 * @param resource Underlying content holder.
 * @param mediaTypes Media type hints.
 * @param fileExtensions File extension hints.
 */
class ContainerSnifferContext internal constructor(
    val container: Container,
    val isExploded: Boolean,
    mediaTypes: List<String> = emptyList(),
    fileExtensions: List<String> = emptyList()
) : ContentAwareSnifferContext(mediaTypes, fileExtensions) {

    /**
     * Returns whether an Archive entry exists in this file.
     */
    internal suspend fun containsArchiveEntryAt(path: String): Boolean =
        container.entry(path).read(0 until 16L).isSuccess

    /**
     * Returns the Archive entry data at the given [path] in this file.
     */
    internal suspend fun readArchiveEntryAt(path: String): ByteArray? {
        val archive = container

        return withContext(Dispatchers.IO) {
            val entry = archive.entry(path)
            val bytes = entry.read().getOrNull()
            entry.close()
            bytes
        }
    }

    /**
     * Returns whether all the Archive entry paths satisfy the given `predicate`.
     */
    internal suspend fun archiveEntriesAllSatisfy(predicate: (Container.Entry) -> Boolean): Boolean =
        container.entries()
            ?.all(predicate)
            ?: false

    override suspend fun release() {
        container.close()
    }
}

class UrlSnifferContextFactory(
    private val resourceFactory: ResourceFactory,
    private val containerFactory: ContainerFactory,
    private val archiveFactory: ArchiveFactory
) {

    suspend fun createContext(
        url: Url,
        mediaTypes: List<String> = emptyList(),
        fileExtensions: List<String> = emptyList()
    ): ContentAwareSnifferContext? {

        val resource = resourceFactory
            .create(url)
            .getOrElse {
                when (it) {
                    is ResourceFactory.Error.NotAResource ->
                        return tryCreateContainerContext(url, fileExtensions, mediaTypes)
                    else -> return null
                }
            }

        return archiveFactory.create(resource, password = null)
            .fold(
                { ContainerSnifferContext(it, false, fileExtensions) },
                { ResourceSnifferContext(resource, mediaTypes, fileExtensions) }
            )
    }

    private suspend fun tryCreateContainerContext(
        url: Url,
        mediaTypes: List<String>,
        fileExtensions: List<String>
    ): ContentAwareSnifferContext? {
        val container = containerFactory.create(url)
            .getOrNull()
            ?: return null

        return ContainerSnifferContext(container, true, mediaTypes, fileExtensions)
    }
}

class BytesSnifferContextFactory(
    private val archiveFactory: ArchiveFactory
) {

    suspend fun createContext(
        bytes: ByteArray,
        mediaTypes: List<String> = emptyList(),
        fileExtensions: List<String> = emptyList(),
    ): ContentAwareSnifferContext? {
        val resource: Resource = BytesResource(bytes)
        return archiveFactory.create(resource, password = null)
            .fold(
                { ContainerSnifferContext(it, false, mediaTypes, fileExtensions) },
                { ResourceSnifferContext(resource, mediaTypes, fileExtensions) }
            )
    }
}
