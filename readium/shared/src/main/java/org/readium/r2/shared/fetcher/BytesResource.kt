/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.extensions.coerceIn
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Try

sealed class BaseBytesResource(val link: Link, val bytes: suspend () -> ByteArray) : Resource {

    private lateinit var _bytes: ByteArray

    override suspend fun link(): Link = link

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> {
        if (!::_bytes.isInitialized)
            _bytes = bytes()

        if (range == null)
            return Try.success(_bytes)

        @Suppress("NAME_SHADOWING")
        val range = range
            .coerceIn(0L until _bytes.size)
            .requireLengthFitInt()

        return Try.success(_bytes.sliceArray(range.map(Long::toInt)))
    }

    override suspend fun length(): ResourceTry<Long> =
        read().map { it.size.toLong() }

    override suspend fun close() {}
}

/** Creates a Resource serving [ByteArray]. */
class BytesResource(link: Link, bytes: suspend () -> ByteArray) : BaseBytesResource(link, bytes) {

    constructor(link: Link, bytes: ByteArray) : this(link, { bytes })

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { bytes().size }} bytes)"

}

/** Creates a Resource serving a [String]. */
class StringResource(link: Link, string: suspend () -> String) : BaseBytesResource(link, { string().toByteArray() }) {

    constructor(link: Link, string: String) : this(link, { string })

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { bytes().toString() }})"

}