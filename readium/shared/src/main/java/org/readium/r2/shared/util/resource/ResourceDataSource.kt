/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import android.graphics.Bitmap
import java.nio.charset.Charset
import org.json.JSONObject
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.datasource.DataSource
import org.readium.r2.shared.util.datasource.DecoderError
import org.readium.r2.shared.util.datasource.decode
import org.readium.r2.shared.util.datasource.readAsBitmap
import org.readium.r2.shared.util.datasource.readAsJson
import org.readium.r2.shared.util.datasource.readAsString
import org.readium.r2.shared.util.datasource.readAsXml
import org.readium.r2.shared.util.xml.ElementNode

private fun DecoderError<ResourceError>.toResourceError() =
    when (this) {
        is DecoderError.DataSourceError ->
            cause
        is DecoderError.DecodingError ->
            ResourceError.InvalidContent(cause)
    }

public suspend fun<R> Resource.decode(
    block: (value: ByteArray) -> R,
    wrapException: (Exception) -> Error
): ResourceTry<R> =
    read()
        .decode(block, wrapException)
        .mapFailure { it.toResourceError() }

/**
 * Reads the full content as a [String].
 *
 * If [charset] is null, then it falls back on UTF-8.
 */
public suspend fun Resource.readAsString(charset: Charset = Charsets.UTF_8): ResourceTry<String> =
    asDataSource().readAsString(charset).mapFailure { it.toResourceError() }

/**
 * Reads the full content as a JSON object.
 */
public suspend fun Resource.readAsJson(): ResourceTry<JSONObject> =
    asDataSource().readAsJson().mapFailure { it.toResourceError() }

/**
 * Reads the full content as an XML document.
 */
public suspend fun Resource.readAsXml(): ResourceTry<ElementNode> =
    asDataSource().readAsXml().mapFailure { it.toResourceError() }

/**
 * Reads the full content as a [Bitmap].
 */
public suspend fun Resource.readAsBitmap(): ResourceTry<Bitmap> =
    asDataSource().readAsBitmap().mapFailure { it.toResourceError() }

internal class ResourceDataSource(
    private val resource: Resource
) : DataSource<ResourceError> {

    override suspend fun length(): Try<Long, ResourceError> =
        resource.length()

    override suspend fun read(range: LongRange?): Try<ByteArray, ResourceError> =
        resource.read()

    override suspend fun close() {
        resource.close()
    }
}

internal fun Resource.asDataSource() =
    ResourceDataSource(this)
