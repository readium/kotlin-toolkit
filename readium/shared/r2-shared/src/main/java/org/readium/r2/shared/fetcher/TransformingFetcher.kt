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

class TransformingFetcher(val fetcher: Fetcher, val transformers: List<ResourceTransformer>) : Fetcher {

    constructor(fetcher: Fetcher, transformer: ResourceTransformer)
            : this(fetcher, listOf(transformer))

    override fun get(link: Link): Resource {
        val resource = fetcher.get(link)
        return transformers.fold(resource) { acc, transformer -> transformer(acc) }
    }

    override fun close() {
        fetcher.close()
    }
}
