/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.fetcher

import java.io.InputStream
import org.readium.r2.shared.publication.Publication

@Suppress("UNUSED_PARAMETER", "unused")
@Deprecated("Use [publication.get(link)] to access publication content.", level = DeprecationLevel.ERROR)
public class Fetcher(
    public var publication: Publication,
    private val userPropertiesPath: String?,
) {

    public fun data(path: String): ByteArray? = throw NotImplementedError()

    public fun dataStream(path: String): InputStream = throw NotImplementedError()

    public fun dataLength(path: String): Long = throw NotImplementedError()
}
