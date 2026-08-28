/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Transforms a manifest's components.
 */
@ExperimentalReadiumApi
public interface ManifestTransformer {
    public fun transform(manifest: Manifest): Manifest = manifest
    public fun transform(metadata: Metadata): Metadata = metadata
    public fun transform(link: Link): Link = link
    public fun transform(href: Href): Href = href
}

/**
 * Creates a copy of the receiver [Manifest], applying the given [transformer] to each component.
 */
@ExperimentalReadiumApi
public fun Manifest.copy(transformer: ManifestTransformer): Manifest =
    transformer.transform(
        copy(
            metadata = metadata.copy(transformer),
            links = links.copy(transformer),
            readingOrder = readingOrder.copy(transformer),
            resources = resources.copy(transformer),
            tableOfContents = tableOfContents.copy(transformer),
            subcollections = subcollections.copy(transformer)
        )
    )

@ExperimentalReadiumApi
public fun Metadata.copy(transformer: ManifestTransformer): Metadata =
    transformer.transform(
        copy(
            subjects = subjects.copy(transformer),
            authors = authors.copy(transformer),
            translators = translators.copy(transformer),
            editors = editors.copy(transformer),
            artists = artists.copy(transformer),
            illustrators = illustrators.copy(transformer),
            letterers = letterers.copy(transformer),
            pencilers = pencilers.copy(transformer),
            colorists = colorists.copy(transformer),
            inkers = inkers.copy(transformer),
            narrators = narrators.copy(transformer),
            contributors = contributors.copy(transformer),
            publishers = publishers.copy(transformer),
            imprints = imprints.copy(transformer),
            belongsTo = belongsTo.copy(transformer)
        )
    )

@ExperimentalReadiumApi
public fun PublicationCollection.copy(transformer: ManifestTransformer): PublicationCollection =
    copy(
        links = links.copy(transformer),
        subcollections = subcollections.copy(transformer)
    )

@ExperimentalReadiumApi
@JvmName("copyPublicationCollections")
public fun Map<String, List<PublicationCollection>>.copy(transformer: ManifestTransformer): Map<String, List<PublicationCollection>> =
    mapValues { (_, value) ->
        value.map { it.copy(transformer) }
    }

@ExperimentalReadiumApi
@JvmName("copyContributorsMap")
public fun Map<String, List<Contributor>>.copy(transformer: ManifestTransformer): Map<String, List<Contributor>> =
    mapValues { (_, value) ->
        value.map { it.copy(transformer) }
    }

@ExperimentalReadiumApi
@JvmName("copyContributors")
public fun List<Contributor>.copy(transformer: ManifestTransformer): List<Contributor> =
    map { it.copy(transformer) }

@ExperimentalReadiumApi
public fun Contributor.copy(transformer: ManifestTransformer): Contributor =
    copy(
        links = links.copy(transformer)
    )

@ExperimentalReadiumApi
@JvmName("copySubjects")
public fun List<Subject>.copy(transformer: ManifestTransformer): List<Subject> =
    map { it.copy(transformer) }

@ExperimentalReadiumApi
public fun Subject.copy(transformer: ManifestTransformer): Subject =
    copy(
        links = links.copy(transformer)
    )

@ExperimentalReadiumApi
@JvmName("copyLinks")
public fun List<Link>.copy(transformer: ManifestTransformer): List<Link> =
    map { it.copy(transformer) }

@ExperimentalReadiumApi
public fun Link.copy(transformer: ManifestTransformer): Link =
    transformer.transform(
        copy(
            href = href.copy(transformer),
            alternates = alternates.copy(transformer),
            children = children.copy(transformer)
        )
    )

@ExperimentalReadiumApi
public fun Href.copy(transformer: ManifestTransformer): Href =
    transformer.transform(this)
