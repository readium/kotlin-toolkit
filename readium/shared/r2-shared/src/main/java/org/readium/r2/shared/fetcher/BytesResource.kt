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


/** Creates a Resource serving [ByteArray] or an error if the [ByteArray] cannot be initialized. */
abstract class BytesResource : Resource {

    private lateinit var byteArray: ResourceTry<ByteArray>

    private suspend fun cachedBytes(): ResourceTry<ByteArray> {
        if(!::byteArray.isInitialized) {
            byteArray = bytes()
        }
        return byteArray
    }

    abstract suspend fun bytes(): ResourceTry<ByteArray>

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> {
        if (range == null)
            return bytes()

        @Suppress("NAME_SHADOWING")
        val range = range.coerceToPositiveIncreasing().apply { requireLengthFitInt() }
        return cachedBytes().map { it.sliceArray(range.map(Long::toInt)) }
    }

    override suspend fun length(): ResourceTry<Long> = cachedBytes().map { it.size.toLong() }
}

/** Creates a Resource serving a [String]. */
class StringResource(val link: Link, val factory: suspend () -> String) : BytesResource() {

    constructor(link: Link, string: String) : this(link, { string })

    override suspend fun bytes(): ResourceTry<ByteArray> = Try.success(factory().toByteArray())

    override suspend fun link(): Link = link()

    override suspend fun close() {}
}