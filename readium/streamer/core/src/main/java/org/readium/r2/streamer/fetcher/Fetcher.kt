/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.fetcher

import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.container.Container
import org.readium.r2.streamer.server.Resources
import java.io.InputStream


@Suppress("UNUSED_PARAMETER", "unused")
@Deprecated("Use [publication.get(link)] to access publication content.", level = DeprecationLevel.ERROR)
class Fetcher(var publication: Publication, var container: Container, private val userPropertiesPath: String?, customResources: Resources? = null) {

    fun data(path: String): ByteArray? = throw NotImplementedError()

    fun dataStream(path: String): InputStream = throw NotImplementedError()

    fun dataLength(path: String): Long = throw NotImplementedError()

}