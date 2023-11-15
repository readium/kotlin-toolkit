/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.xml.ElementNode
import org.readium.r2.shared.util.xml.XmlParser

public sealed class DecoderError(
    override val message: String
) : Error {

    public class DataAccess(
        override val cause: ReadError
    ) : DecoderError("Data source error")

    public class DecodingError(
        override val cause: Error?
    ) : DecoderError("Decoding Error")
}

internal suspend fun<R, S> Try<S, ReadError>.decode(
    block: (value: S) -> R,
    wrapException: (Exception) -> Error
): Try<R, DecoderError> =
    when (this) {
        is Try.Success ->
            try {
                Try.success(
                    withContext(Dispatchers.Default) {
                        block(value)
                    }
                )
            } catch (e: Exception) {
                Try.failure(DecoderError.DecodingError(wrapException(e)))
            }
        is Try.Failure ->
            Try.failure(DecoderError.DataAccess(value))
    }

internal suspend fun<R, S> Try<S, DecoderError>.decodeMap(
    block: (value: S) -> R,
    wrapException: (Exception) -> Error
): Try<R, DecoderError> =
    when (this) {
        is Try.Success ->
            try {
                Try.success(
                    withContext(Dispatchers.Default) {
                        block(value)
                    }
                )
            } catch (e: Exception) {
                Try.failure(DecoderError.DecodingError(wrapException(e)))
            }
        is Try.Failure ->
            Try.failure(value)
    }

/**
 * Content as plain text.
 *
 * It will extract the charset parameter from the media type hints to figure out an encoding.
 * Otherwise, fallback on UTF-8.
 */
public suspend fun Blob.readAsString(
    charset: Charset = Charsets.UTF_8
): Try<String, DecoderError> =
    read().decode(
        { String(it, charset = charset) },
        { MessageError("Content is not a valid $charset string.", ThrowableError(it)) }
    )

/** Content as an XML document. */
public suspend fun Blob.readAsXml(): Try<ElementNode, DecoderError> =
    read().decode(
        { XmlParser().parse(ByteArrayInputStream(it)) },
        { MessageError("Content is not a valid XML document.", ThrowableError(it)) }
    )

/**
 * Content parsed from JSON.
 */
public suspend fun Blob.readAsJson(): Try<JSONObject, DecoderError> =
    readAsString().decodeMap(
        { JSONObject(it) },
        { MessageError("Content is not valid JSON.", ThrowableError(it)) }
    )

/** Readium Web Publication Manifest parsed from the content. */
public suspend fun Blob.readAsRwpm(): Try<Manifest, DecoderError> =
    readAsJson().flatMap { json ->
        Manifest.fromJSON(json)
            ?.let { Try.success(it) }
            ?: Try.failure(
                DecoderError.DecodingError(
                    MessageError("Content is not a valid RWPM.")
                )
            )
    }

/**
 * Reads the full content as a [Bitmap].
 */
public suspend fun Blob.readAsBitmap(): Try<Bitmap, DecoderError> =
    read()
        .mapFailure { DecoderError.DataAccess(it) }
        .flatMap { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?.let { Try.success(it) }
                ?: Try.failure(
                    DecoderError.DecodingError(
                        MessageError("Could not decode resource as a bitmap.")
                    )
                )
        }

/**
 * Returns whether the content is a JSON object containing all of the given root keys.
 */
public suspend fun Blob.containsJsonKeys(
    vararg keys: String
): Try<Boolean, DecoderError> {
    val json = readAsJson()
        .getOrElse { return Try.failure(it) }
    return Try.success(json.keys().asSequence().toSet().containsAll(keys.toList()))
}
