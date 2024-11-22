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
    private val epubVersion = packageDocument.epubVersion
    private val spine = packageDocument.spine

    fun adapt(): Manifest {
        // Compute metadata
        val metadata = MetadataAdapter(
            epubVersion,
            packageDocument.uniqueIdentifierId,
            spine.direction,
            displayOptions
        ).adapt(packageDocument.metadata)

        // Compute links
        val (readingOrder, resources) = ResourceAdapter(
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

        // Build Publication object
        return Manifest(
            metadata = metadata.metadata,
            links = emptyList(),
            readingOrder = readingOrder,
            resources = resources,
            tableOfContents = toc,
            subcollections = subcollections
        )
    }
}
