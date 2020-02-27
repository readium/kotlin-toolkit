/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import androidx.core.content.MimeTypeFilter
import org.readium.r2.shared.publication.Link
import java.io.InputStream

interface ContentFilter {

    val priority: Int

    val accepts: Collection<String>

    fun filter(input: ByteArray, link: Link): ByteArray

    fun filter(input: InputStream, link: Link): InputStream =
        filter(input.readBytes(), link).inputStream()

    fun acceptsLink(link: Link): Boolean =
        accepts.map { MimeTypeFilter.matches(link.type, it) }.any { it }
}