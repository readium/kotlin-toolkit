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
import org.readium.r2.shared.resource.ResourceTry
import org.readium.r2.shared.util.Try

sealed class BaseBytesResource(
    private val link: Link,
    protected val resource: org.readium.r2.shared.resource.BytesResource
) : Resource by resource, Fetcher.Resource {

    constructor(link: Link, bytes: suspend () -> ResourceTry<ByteArray>)
        : this(link, org.readium.r2.shared.resource.BytesResource(bytes))

    constructor(link: Link, bytes: ByteArray)
        : this(link, { Try.success(bytes) })

    override suspend fun link(): Link =
        link

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { length().getOrNull() }} bytes)"
}

/** Creates a Resource serving [ByteArray]. */
class BytesResource(
    link: Link,
    bytes: suspend () -> ByteArray
) : BaseBytesResource(link, { Try.success(bytes()) }) {

    constructor(link: Link, bytes: ByteArray) : this(link, { bytes })

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { resource.length() } } })"
}

/** Creates a Resource serving a [String]. */
class StringResource(
    link: Link,
    string: suspend () -> String
) : BaseBytesResource(link, { Try.success(string()).map { it.toByteArray() } }) {

    constructor(link: Link, string: String) : this(link, { string })

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { resource.bytes().map { it.toString() } } })"
}
