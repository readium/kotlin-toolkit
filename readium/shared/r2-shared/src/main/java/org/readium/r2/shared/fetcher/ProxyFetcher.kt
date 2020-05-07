/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import org.readium.r2.shared.publication.Link

/** Delegates the creation of a [Resource] to a [closure]. */
internal class ProxyFetcher(val closure: (Link, HrefParameters) -> Resource) : Fetcher {

    override fun get(link: Link, parameters: HrefParameters): Resource = closure(link, parameters)

    override fun close() {}
}

