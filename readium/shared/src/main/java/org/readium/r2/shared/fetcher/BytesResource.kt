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
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.resource.Resource

sealed class BaseBytesResource(
    private val link: Link,
    protected val resource: org.readium.r2.shared.resource.BytesResource
) : Resource by resource, Fetcher.Resource {

    constructor(link: Link, bytes: suspend () -> ByteArray) : this(link, org.readium.r2.shared.resource.BytesResource(bytes))

    constructor(link: Link, bytes: ByteArray) : this(link, { bytes })

    override suspend fun link(): Link =
        link

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { resource.bytes().size }} bytes)"
}

/** Creates a Resource serving [ByteArray]. */
class BytesResource(
    link: Link,
    bytes: suspend () -> ByteArray
) : BaseBytesResource(link, bytes) {

    constructor(link: Link, bytes: ByteArray) : this(link, { bytes })
}

/** Creates a Resource serving a [String]. */
class StringResource(link: Link, string: suspend () -> String) :
    BaseBytesResource(link, { string().toByteArray() }) {

    constructor(link: Link, string: String) : this(link, { string })

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { resource.bytes().toString() }})"
}
