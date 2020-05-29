/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumannn, Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.fetcher

import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.HrefParameters
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.server.Resources

internal class ServingFetcher(
    val publication: Publication,
    userPropertiesPath: String?,
    customResources: Resources? = null
) : Fetcher {

    private val htmlInjector: HtmlInjector = HtmlInjector(publication, userPropertiesPath, customResources)

    override val links: List<Link> = emptyList()

    override fun get(link: Link, parameters: HrefParameters): Resource {
        val resource = publication.get(link, parameters)
        return if (publication.type == Publication.TYPE.EPUB)
            htmlInjector.transform(resource)
        else
            resource
    }

    override fun close() {}
}
