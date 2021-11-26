/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumannn, Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.server

import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.fetcher.HtmlInjector

internal class ServingFetcher(
    val publication: Publication,
    private val enableReadiumNavigatorSupport: Boolean,
    userPropertiesPath: String?,
    customResources: Resources? = null,
) : Fetcher {

    private val htmlInjector: HtmlInjector by lazy {
        HtmlInjector(
            publication,
            userPropertiesPath,
            customResources
        )
    }

    override suspend fun links(): List<Link> = emptyList()

    override fun get(link: Link): Resource {
        val resource = publication.get(link)
        return if (enableReadiumNavigatorSupport)
            transformResourceForReadiumNavigator(resource)
        else
            resource
    }

    private fun transformResourceForReadiumNavigator(resource: Resource): Resource {
        return if (publication.conformsTo(Publication.Profile.EPUB))
            htmlInjector.transform(resource)
        else
            resource
    }

    override fun get(href: String): Resource {
        val link = publication.linkWithHref(href)
            ?.copy(href = href) // query parameters must be kept
            ?: Link(href = href)

        return get(link)
    }

    override suspend fun close() {}
}
