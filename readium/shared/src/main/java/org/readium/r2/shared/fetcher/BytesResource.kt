/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */
package org.readium.r2.shared.fetcher

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceTry

@InternalReadiumApi
public sealed class BaseBytesResource(
    private val link: Link,
    protected val resource: org.readium.r2.shared.resource.BytesResource
) : Resource by resource, Fetcher.Resource {

    protected constructor(link: Link, bytes: suspend () -> ResourceTry<ByteArray>) :
        this(link, org.readium.r2.shared.resource.BytesResource(bytes))

    protected constructor(link: Link, bytes: ByteArray) :
        this(link, { Try.success(bytes) })

    override suspend fun link(): Link =
        link

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { length().getOrNull() }} bytes)"
}

/** Creates a Resource serving [ByteArray]. */
public class BytesResource(
    link: Link,
    bytes: suspend () -> ByteArray
) : BaseBytesResource(link, { Try.success(bytes()) }) {

    public constructor(link: Link, bytes: ByteArray) : this(link, { bytes })
}

/** Creates a Resource serving a [String]. */
public class StringResource(
    link: Link,
    string: suspend () -> String
) : BaseBytesResource(link, { Try.success(string()).map { it.toByteArray() } }) {

    public constructor(link: Link, string: String) : this(link, { string })

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { resource.bytes().map { it.toString() } } })"
}
