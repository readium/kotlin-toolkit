/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.coerceToPositiveIncreasing
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.extensions.readFully
import org.readium.r2.shared.extensions.readRange
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.util.Try
import timber.log.Timber
import java.io.InputStream
import java.lang.Exception

internal abstract class StreamResource : Resource {

    abstract fun stream(): ResourceTry<InputStream>

    /** An estimate of data length from metadata */
    protected abstract val metadataLength: Long?

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray>  {
        val stream = stream()
        val result = stream.mapCatching {
            if (range == null)
                it.readFully()
            else
                it.readRange(range)
        }

        // We don't want to return a failure if an exception is raised only when closing the InputStream
        stream.onSuccess {
            try {
                it.close()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        return result
    }

    override suspend fun length(): ResourceTry<Long> =
        metadataLength?.let { Try.success(it) }
            ?: read().map { it.size.toLong() }
}
