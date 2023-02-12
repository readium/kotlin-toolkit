/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.asset

import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.Charset
import org.json.JSONObject
import org.readium.r2.shared.fetcher.*
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Represents a publication stored as a file on the local file system.
 *
 * @param file File on the file system.
 */
class FileAsset private constructor(
    val file: File,
    private val knownMediaType: MediaType?,
    private val mediaTypeHint: String?
) : PublicationAsset {

    /**
     * Creates a [FileAsset] from a [File] and an optional media type, when known.
     */
    constructor(file: File, mediaType: MediaType? = null) :
        this(file, knownMediaType = mediaType, mediaTypeHint = null)

    /**
     * Creates a [FileAsset] from a [File] and an optional media type hint.
     *
     * Providing a media type hint will improve performances when sniffing the media type.
     */
    constructor(file: File, mediaTypeHint: String?) :
        this(file, knownMediaType = null, mediaTypeHint = mediaTypeHint)

    override val name: String
        get() = file.name

    override suspend fun mediaType(): MediaType {
        if (!::_mediaType.isInitialized) {
            _mediaType = knownMediaType
                ?: MediaType.ofFile(file, mediaType = mediaTypeHint)
                ?: MediaType.BINARY
        }

        return _mediaType
    }

    private lateinit var _mediaType: MediaType

    override suspend fun createFetcher(
        dependencies: PublicationAsset.Dependencies,
        credentials: String?
    ): Try<Fetcher, Publication.OpeningException> {
        return try {
            when {
                file.isDirectory -> Try.success(FileFetcher(href = "/", file = file))
                file.exists() -> createFetcherForFile(file, dependencies)
                else -> throw FileNotFoundException(file.path)
            }
        } catch (e: SecurityException) {
            Try.failure(Publication.OpeningException.Forbidden(e))
        } catch (e: FileNotFoundException) {
            Try.failure(Publication.OpeningException.NotFound(e))
        }
    }

    private suspend fun createFetcherForFile(
        file: File,
        dependencies: PublicationAsset.Dependencies,
    ): Try<Fetcher, Publication.OpeningException> {
        ArchiveFetcher.fromPath(file.path, dependencies.archiveFactory)
            ?.let { return Try.success(it) }

        if (mediaType().isRwpm) {
            val manifest = file.readAsRwpm(packaged = false)
                .mapFailure { Publication.OpeningException.ParsingFailed(it) }
                .getOrElse { return Try.failure(it) }

            val fileFetcher = FileFetcher(href = "/manifest.json", file = file)
            val baseUrl = manifest.linkWithRel("self")?.let { File(it.href).parent }
            val httpFetcher = HttpFetcher(dependencies.httpClient, baseUrl)
            return Try.success(RoutingFetcher(fileFetcher, httpFetcher))
        }

        return Try.success(FileFetcher(href = "/${file.name}", file = file))
    }

    private fun File.readAsRwpm(packaged: Boolean): Try<Manifest, Exception> =
        try {
            val bytes = readBytes()
            val string = String(bytes, Charset.defaultCharset())
            val json = JSONObject(string)
            val manifest = Manifest.fromJSON(json, packaged = packaged)
                ?: throw Exception("Failed to parse the RWPM Manifest")
            Try.success(manifest)
        } catch (e: Exception) {
            Try.failure(e)
        }

    override fun toString(): String = "FileAsset(${file.path})"
}
