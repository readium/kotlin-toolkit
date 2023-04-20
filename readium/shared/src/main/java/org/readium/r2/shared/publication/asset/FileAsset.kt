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
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.archive.ExplodedArchiveFactory
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.toUrl

/**
 * A [PublicationAsset] built from a [File].
 */
data class FileAsset(
    val file: File,
    override val mediaType: MediaType,
    override val fetcher: Fetcher
) : PublicationAsset {

    override val name: String =
        file.name

    internal class Factory(
        private val archiveFactory: ArchiveFactory,
        private val httpClient: HttpClient
    ) {

        suspend fun createAssetForPackagedPublication(
            file: File,
            mediaType: MediaType
        ): Try<PublicationAsset, Publication.OpeningException> {
            return ArchiveFetcher.create(file.toUrl(), archiveFactory)
                .map { fetcher ->
                    FileAsset(
                        file = file,
                        mediaType = mediaType,
                        fetcher = fetcher
                    )
                }.mapFailure { error ->
                    when (error) {

                    }
                }
        }

        suspend fun createAssetForExplodedPublication(
            file: File,
            mediaType: MediaType
        ): Try<PublicationAsset, Publication.OpeningException> {


        }

        suspend fun createAssetForWebpubManifest(
            file: File,
            mediaType: MediaType
        ): Try<PublicationAsset, Publication.OpeningException> {
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

            val asset =
                FileAsset(
                    file = file,
                    mediaType = mediaType,
                    fetcher = fetcher
                )
            return Try.success(asset)
        }

        private suspend fun createFetcher(file: File, mediaType: MediaType): Try<Fetcher, Publication.OpeningException> {
            return try {
                when {
                    file.isDirectory -> Try.success(FileFetcher(href = "/", file = file))
                    file.exists() -> createFetcherForFile(file, mediaType)
                    else -> throw FileNotFoundException(file.path)
                }
            } catch (e: SecurityException) {
                Try.failure(Publication.OpeningException.Forbidden(e))
            } catch (e: FileNotFoundException) {
                Try.failure(Publication.OpeningException.NotFound(e))
            }
        }

        private suspend fun createFetcherForFile(file: File, mediaType: MediaType): Try<Fetcher, Publication.OpeningException> {
            ArchiveFetcher.create(file.toUrl(), archiveFactory)
                ?.let { return Try.success(it) }

            if (mediaType.isRwpm) {
                val manifest = file.readAsRwpm(packaged = false)
                    .mapFailure { Publication.OpeningException.ParsingFailed(it) }
                    .getOrElse { return Try.failure(it) }

                val fileFetcher = FileFetcher(href = "/manifest.json", file = file)
                val baseUrl = manifest.linkWithRel("self")?.let { File(it.href).parent }
                val httpFetcher = HttpFetcher(httpClient, baseUrl)
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
    }
}
