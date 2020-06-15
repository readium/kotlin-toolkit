/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import org.readium.r2.shared.extensions.coerceToPositiveIncreasing
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Try


/** Creates a Resource serving [ByteArray] computed from a factory that can fail. */
open class BytesResource(private val factory: suspend () -> Pair<Link, ResourceTry<ByteArray>>) : Resource {

    constructor(link: Link, bytes: ByteArray) : this({ Pair(link, Try.success(bytes)) })

    constructor(link: Link, factory: suspend () -> ByteArray) : this({ Pair(link, Try.success(factory())) })

    private lateinit var byteArray: ResourceTry<ByteArray>
    private lateinit var computedLink: Link

    private suspend fun initDataIfNeeded() {
        if(!::byteArray.isInitialized || !::computedLink.isInitialized) {
            val res = factory()
            computedLink = res.first
            byteArray = res.second
        }
    }

    private suspend fun bytes(): ResourceTry<ByteArray> {
        initDataIfNeeded()
        return byteArray
    }

    override suspend fun link(): Link {
        initDataIfNeeded()
        return computedLink
    }

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> {
        if (range == null)
            return bytes()

        @Suppress("NAME_SHADOWING")
        val range = range.coerceToPositiveIncreasing().apply { requireLengthFitInt() }
        return bytes().map { it.sliceArray(range.map(Long::toInt)) }
    }

    override suspend fun length(): ResourceTry<Long> = byteArray.map { it.size.toLong() }

    override suspend fun close() {}
}

/** Creates a Resource serving a [String] computed from a factory that can fail. */
class StringResource(factory: suspend () -> Pair<Link, ResourceTry<String>>) : BytesResource(
    {
        val (link,res) = factory()
        Pair(link, res.mapCatching { it.toByteArray() })
    }
) {
    constructor(link: Link, string: String) : this({ Pair(link, Try.success(string)) })

    constructor(link: Link, factory: suspend () -> String) : this({ Pair(link, Try.success(factory())) })
}