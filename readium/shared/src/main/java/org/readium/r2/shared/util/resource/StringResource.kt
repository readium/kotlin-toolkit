/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable

/** Creates a Resource serving a [String]. */
public class StringResource(
    private val readable: Readable,
    private val properties: Resource.Properties
) : Resource, Readable by readable {

    public constructor(
        source: AbsoluteUrl? = null,
        properties: Resource.Properties = Resource.Properties(),
        string: suspend () -> Try<String, ReadError>
    ) : this(InMemoryResource(source, properties) { string().map { it.toByteArray() } }, properties)

    public constructor(
        string: String,
        source: AbsoluteUrl? = null,
        properties: Resource.Properties = Resource.Properties()
    ) : this(source, properties, { Try.success(string) })

    override val source: AbsoluteUrl? =
        null

    override suspend fun properties(): Try<Resource.Properties, ReadError> =
        Try.success(properties)

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { read().map { it.decodeToString() } } }})"
}
