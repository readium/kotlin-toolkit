/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.datasource

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
import org.readium.r2.shared.util.xml.ElementNode
import org.readium.r2.shared.util.xml.XmlParser

internal sealed class DecoderError<E : Error>(
    override val message: String
) : Error {

    class DataSourceError<E : Error>(
        override val cause: E
    ) : DecoderError<E>("Data source error")

    class DecodingError<E : Error>(
        override val cause: Error?
    ) : DecoderError<E>("Decoding Error")
}

internal suspend fun<R, S, E : Error> Try<S, E>.decode(
    block: (value: S) -> R,
    wrapException: (Exception) -> Error
): Try<R, DecoderError<E>> =
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
            Try.failure(DecoderError.DataSourceError(value))
    }

internal suspend fun<R, S, E : Error> Try<S, DecoderError<E>>.decodeMap(
    block: (value: S) -> R,
    wrapException: (Exception) -> Error
): Try<R, DecoderError<E>> =
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
internal suspend fun<E : Error> DataSource<E>.readAsString(
    charset: Charset = Charsets.UTF_8
): Try<String, DecoderError<E>> =
    read().decode(
        { String(it, charset = charset) },
        { MessageError("Content is not a valid $charset string.", ThrowableError(it)) }
    )

/** Content as an XML document. */
internal suspend fun<E : Error> DataSource<E>.readAsXml(): Try<ElementNode, DecoderError<E>> =
    read().decode(
        { XmlParser().parse(ByteArrayInputStream(it)) },
        { MessageError("Content is not a valid XML document.", ThrowableError(it)) }
    )

/**
 * Content parsed from JSON.
 */
internal suspend fun<E : Error> DataSource<E>.readAsJson(): Try<JSONObject, DecoderError<E>> =
    readAsString().decodeMap(
        { JSONObject(it) },
        { MessageError("Content is not valid JSON.", ThrowableError(it)) }
    )

/** Readium Web Publication Manifest parsed from the content. */
internal suspend fun<E : Error> DataSource<E>.readAsRwpm(): Try<Manifest, DecoderError<E>> =
    readAsJson().flatMap { json ->
        Manifest.fromJSON(json)
            ?.let { Try.success(it) }
            ?: Try.failure(
                DecoderError.DecodingError(
                    MessageError("Content is not a valid RPWM.")
                )
            )
    }

/**
 * Reads the full content as a [Bitmap].
 */
internal suspend fun<E : Error> DataSource<E>.readAsBitmap(): Try<Bitmap, DecoderError<E>> =
    read()
        .mapFailure { DecoderError.DataSourceError(it) }
        .flatMap { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?.let { Try.success(it) }
                ?: Try.failure(
                    DecoderError.DecodingError(
                        MessageError("Could not decode resource as a bitmap.")
                    )
                )
        }
