/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.assertSuccess
import org.readium.r2.shared.util.data.Blob
import org.readium.r2.shared.util.data.InMemoryBlob
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.mediatype.MediaType

/** Creates a Resource serving a [String]. */
public class StringResource(
    private val blob: Blob,
    private val mediaType: MediaType,
    private val properties: Resource.Properties
) : Resource, Blob by blob {

    public constructor(
        mediaType: MediaType,
        source: AbsoluteUrl? = null,
        properties: Resource.Properties = Resource.Properties(),
        string: suspend () -> Try<String, ReadError>
    ) : this(InMemoryBlob(source) { string().map { it.toByteArray() } }, mediaType, properties)

    public constructor(
        string: String,
        mediaType: MediaType,
        source: AbsoluteUrl? = null,
        properties: Resource.Properties = Resource.Properties()
    ) : this(mediaType, source, properties, { Try.success(string) })

    override suspend fun mediaType(): Try<MediaType, ReadError> =
        Try.success(mediaType)

    override suspend fun properties(): Try<Resource.Properties, ReadError> =
        Try.success(properties)

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { read().assertSuccess().decodeToString() } }})"
}
