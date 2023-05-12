/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.asset

import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.Charset
import org.json.JSONObject
import org.readium.r2.shared.fetcher.*
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.archive.ExplodedArchive
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * A [PublicationAsset] built from a [File].
 */
data class FileAsset(
    val file: File,
    override val mediaType: MediaType,
    override val assetType: PublicationAsset.Type
) : PublicationAsset {

    override val name: String =
        file.name
}

class FileFetcherFactory(
    private val archiveFactory: ArchiveFactory,
    private val httpClient: HttpClient
) {

    suspend fun createFetcher(
        asset: FileAsset
    ): Try<Fetcher, Publication.OpeningException> =
        createFetcher(asset.file, asset.mediaType, asset.assetType)

    private suspend fun createFetcher(
        file: File,
        mediaType: MediaType,
        type: PublicationAsset.Type
    ): Try<Fetcher, Publication.OpeningException> =
        when (type) {
            PublicationAsset.Type.Manifest ->
                createFetcherForManifest(file)
            PublicationAsset.Type.PackagedPublication ->
                createFetcherForPackagedPublication(file, mediaType)
            PublicationAsset.Type.ExplodedPublication ->
                createFetcherForExplodedPublication(file)
            PublicationAsset.Type.Content ->
                createFetcherForContentFile(file)
        }

    private suspend fun createFetcherForPackagedPublication(
        file: File,
        mediaType: MediaType
    ): Try<Fetcher, Publication.OpeningException> {
        val resource = FileFetcher.FileResource(
            Link(href = file.path, type = mediaType.toString()),
            file
        )
        return archiveFactory.open(resource, password = null)
            .mapFailure { Publication.OpeningException.ParsingFailed(it)  }
            .map { archive -> ArchiveFetcher(archive)  }
    }

    private fun createFetcherForExplodedPublication(
        file: File
    ): Try<Fetcher, Publication.OpeningException> {
        val archive = ExplodedArchive(file)
        val fetcher = ArchiveFetcher(archive)
        return Try.success(fetcher)
    }

    private fun createFetcherForManifest(
        file: File
    ): Try<Fetcher, Publication.OpeningException> {
        val manifest = file.readAsRwpm(packaged = false)
            .mapFailure { Publication.OpeningException.ParsingFailed(it) }
            .getOrElse { return Try.failure(it) }

        val baseUrl =
            manifest.linkWithRel("self")?.let { File(it.href).parent }

        val fetcher =
            RoutingFetcher(
                local = FileFetcher(href = "/manifest.json", file = file),
                remote = HttpFetcher(httpClient, baseUrl)
            )

        return Try.success(fetcher)
    }

    private fun createFetcherForContentFile(
        file: File
    ): Try<Fetcher, Publication.OpeningException> {
        try {
            if (!file.exists()) {
                val exception = FileNotFoundException(file.path)
             return Try.failure(Publication.OpeningException.NotFound(exception))
            }
        } catch (e: SecurityException) {
            return Try.failure(Publication.OpeningException.Forbidden(e))
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
}