/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.PublicationCollection
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.util.Url

/**
 * Creates a [Manifest] model from an EPUB package's document.
 *
 * @param displayOptions iBooks Display Options XML file to use as a fallback for the metadata.
 *        See https://github.com/readium/architecture/blob/master/streamer/parser/metadata.md#epub-2x-9
 */
internal class ManifestAdapter(
    private val packageDocument: PackageDocument,
    private val navigationData: Map<String, List<Link>> = emptyMap(),
    private val encryptionData: Map<Url, Encryption> = emptyMap(),
    private val displayOptions: Map<String, String> = emptyMap(),
) {

    data class Result(
        val manifest: Manifest,
        val mediaOverlays: List<Link>,
    )
    private val epubVersion = packageDocument.epubVersion
    private val spine = packageDocument.spine

    fun adapt(): Result {
        // Compute metadata
        val metadata = MetadataAdapter(
            epubVersion,
            packageDocument.uniqueIdentifierId,
            spine.direction,
            displayOptions
        ).adapt(packageDocument.metadata)

        // Compute links
        val (readingOrder, resources, mediaOverlays) = ResourceAdapter(
            packageDocument.spine,
            packageDocument.manifest,
            encryptionData,
            metadata.coverId,
            metadata.durationById
        ).adapt()

        // Compute toc and otherCollections
        val toc = navigationData["toc"].orEmpty()
        val subcollections = navigationData
            .minus("toc")
            .mapKeys {
                when (it.key) {
                    // RWPM uses camel case for the roles
                    // https://github.com/readium/webpub-manifest/issues/53
                    "page-list" -> "pageList"
                    else -> it.key
                }
            }
            .mapValues { listOf(PublicationCollection(links = it.value)) }
            .toMutableMap()

        // EPUB 3 Reading Systems must ignore the guide element when provided in EPUB 3 Publications
        // whose EPUB Navigation Document includes the landmarks feature.
        // https://idpf.org/epub/30/spec/epub30-publications.html#sec-guide-elem
        if (!subcollections.contains("landmarks") && packageDocument.guide.isNotEmpty()) {
            // EPUB 2.0 doesn't have a landmarks collection, so we use the guide as a fallback
            // If an EPUB 3.0+ file does not have landmarks, it will use guide instead.
            subcollections["landmarks"] = listOf(PublicationCollection(links = packageDocument.guide))
        }

        // Build Publication object
        val manifest = Manifest(
            metadata = metadata.metadata,
            links = emptyList(),
            readingOrder = readingOrder,
            resources = resources,
            tableOfContents = toc,
            subcollections = subcollections.toMap()
        )

        return Result(manifest = manifest, mediaOverlays = mediaOverlays)
    }
}
