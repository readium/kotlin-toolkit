/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.readium

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.fetcher.*
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.services.PerResourcePositionsService
import org.readium.r2.shared.publication.services.locatorServiceFactory
import org.readium.r2.shared.publication.services.positionsServiceFactory
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.streamer.PublicationParser
import org.readium.r2.streamer.container.ContainerError
import org.readium.r2.streamer.container.PublicationContainer
import org.readium.r2.streamer.extensions.readAsJsonOrNull
import org.readium.r2.streamer.fetcher.LcpDecryptor
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.audio.AudioLocatorService
import org.readium.r2.streamer.toPublicationType
import java.io.File
import java.io.FileNotFoundException

/**
 * Parses any Readium Web Publication package or manifest, e.g. WebPub, Audiobook, DiViNa, LCPDF...
 */
@OptIn(PdfSupport::class)
class ReadiumWebPubParser(
    private val pdfFactory: PdfDocumentFactory?,
    private val httpClient: HttpClient,
) : PublicationParser, org.readium.r2.streamer.parser.PublicationParser {

    override suspend fun parse(
        asset: PublicationAsset,
        fetcher: Fetcher,
        warnings: WarningLogger?
    ): Publication.Builder? {
        val mediaType = asset.mediaType()

        if (!mediaType.isReadiumWebPubProfile)
            return null

        val isPackage = !mediaType.isRwpm

        val manifestJson =
            if (isPackage) {
                fetcher.readAsJsonOrNull("/manifest.json")
            } else {
                // For a single manifest file, reads the first (and only) file in the fetcher.
                fetcher.links().firstOrNull()
                    ?.let { fetcher.readAsJsonOrNull(it.href) }
            }
                ?: throw Exception("Manifest not found")

        val manifest = Manifest.fromJSON(manifestJson, packaged = isPackage)
            ?: throw Exception("Failed to parse the RWPM Manifest")

        @Suppress("NAME_SHADOWING")
        var fetcher = fetcher

        // For a manifest, we discard the [fetcher] provided by the Streamer, because it was only
        // used to read the manifest file. We use an [HttpFetcher] instead to serve the remote
        // resources.
        if (!isPackage) {
            val baseUrl = manifest.linkWithRel("self")?.let { File(it.href).parent }
            fetcher = HttpFetcher(httpClient, baseUrl)
        }

        // Checks the requirements from the LCPDF specification.
        // https://readium.org/lcp-specs/notes/lcp-for-pdf.html
        val readingOrder = manifest.readingOrder
        if (asset.mediaType() == MediaType.LCP_PROTECTED_PDF && (readingOrder.isEmpty() || !readingOrder.all { it.mediaType.matches(MediaType.PDF) })) {
            throw Exception("Invalid LCP Protected PDF.")
        }

        val servicesBuilder = Publication.ServicesBuilder().apply {
            when (asset.mediaType()) {
                MediaType.LCP_PROTECTED_PDF ->
                    positionsServiceFactory = pdfFactory?.let { LcpdfPositionsService.create(it) }

                MediaType.DIVINA_MANIFEST, MediaType.DIVINA ->
                    positionsServiceFactory = PerResourcePositionsService.createFactory("image/*")

                MediaType.READIUM_AUDIOBOOK, MediaType.READIUM_AUDIOBOOK_MANIFEST, MediaType.LCP_PROTECTED_AUDIOBOOK ->
                    locatorServiceFactory = AudioLocatorService.createFactory()
            }
        }

        return Publication.Builder(manifest, fetcher, servicesBuilder)
    }

    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? = runBlocking {

        val file = File(fileAtPath)
        val asset = FileAsset(file)
        val mediaType = asset.mediaType()
        var baseFetcher = try {
            ArchiveFetcher.fromPath(file.path) ?: FileFetcher(href = "/${file.name}", file = file)
        } catch (e: SecurityException) {
            return@runBlocking null
        } catch (e: FileNotFoundException) {
            throw ContainerError.missingFile(fileAtPath)
        }

        val drm = if (baseFetcher.isProtectedWithLcp()) DRM(DRM.Brand.lcp) else null
        if (drm?.brand == DRM.Brand.lcp) {
            baseFetcher = TransformingFetcher(baseFetcher, LcpDecryptor(drm)::transform)
        }

        val builder = try {
            parse(asset, baseFetcher)
        } catch (e: Exception) {
            return@runBlocking null
        } ?: return@runBlocking null

        val publication = builder.build()
            .apply { type = mediaType.toPublicationType() }

        val container = PublicationContainer(
            publication = publication,
            path = file.canonicalPath,
            mediaType = mediaType,
            drm = drm
        ).apply {
            if (!mediaType.isRwpm) {
                rootFile.rootFilePath = "manifest.json"
            }
        }

        PubBox(publication, container)
    }
}

private suspend fun Fetcher.isProtectedWithLcp(): Boolean =
    get("license.lcpl").use { it.length().isSuccess }

/** Returns whether this media type is of a Readium Web Publication profile. */
private val MediaType.isReadiumWebPubProfile: Boolean get() =  matchesAny(
    MediaType.READIUM_WEBPUB, MediaType.READIUM_WEBPUB_MANIFEST,
    MediaType.READIUM_AUDIOBOOK, MediaType.READIUM_AUDIOBOOK_MANIFEST, MediaType.LCP_PROTECTED_AUDIOBOOK,
    MediaType.DIVINA, MediaType.DIVINA_MANIFEST, MediaType.LCP_PROTECTED_PDF
)
