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

public sealed class DecodeError(
    override val message: String
) : Error {

    public class Reading(
        override val cause: ReadError
    ) : DecodeError("Reading error")

    public class Decoding(
        override val cause: Error?
    ) : DecodeError("Decoding Error")
}

internal suspend fun<R, S> Try<S, ReadError>.decode(
    block: (value: S) -> R,
    wrapError: (Exception) -> Error
): Try<R, DecodeError> =
    when (this) {
        is Try.Success ->
            try {
                withContext(Dispatchers.Default) {
                    Try.success(block(value))
                }
            } catch (e: Exception) {
                Try.failure(DecodeError.Decoding(wrapError(e)))
            } catch (e: OutOfMemoryError) {
                Try.failure(DecodeError.Reading(ReadError.OutOfMemory(e)))
            }
        is Try.Failure ->
            Try.failure(DecodeError.Reading(value))
    }

internal suspend fun<R, S> Try<S, DecodeError>.decodeMap(
    block: (value: S) -> R,
    wrapError: (Exception) -> Error
): Try<R, DecodeError> =
    when (this) {
        is Try.Success ->
            try {
                withContext(Dispatchers.Default) {
                    Try.success(block(value))
                }
            } catch (e: Exception) {
                Try.failure(DecodeError.Decoding(wrapError(e)))
            } catch (e: OutOfMemoryError) {
                Try.failure(DecodeError.Reading(ReadError.OutOfMemory(e)))
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
public suspend fun Readable.readAsString(
    charset: Charset = Charsets.UTF_8
): Try<String, DecodeError> =
    read().decode(
        { String(it, charset = charset) },
        { MessageError("Content is not a valid $charset string.", ThrowableError(it)) }
    )

/** Content as an XML document. */
public suspend fun Readable.readAsXml(): Try<ElementNode, DecodeError> =
    read().decode(
        { XmlParser().parse(ByteArrayInputStream(it)) },
        { MessageError("Content is not a valid XML document.", ThrowableError(it)) }
    )

/**
 * Content parsed from JSON.
 */
public suspend fun Readable.readAsJson(): Try<JSONObject, DecodeError> =
    readAsString().decodeMap(
        { JSONObject(it) },
        { MessageError("Content is not valid JSON.", ThrowableError(it)) }
    )

/** Readium Web Publication Manifest parsed from the content. */
public suspend fun Readable.readAsRwpm(): Try<Manifest, DecodeError> =
    readAsJson().flatMap { json ->
        Manifest.fromJSON(json)
            ?.let { Try.success(it) }
            ?: Try.failure(
                DecodeError.Decoding(
                    MessageError("Content is not a valid RWPM.")
                )
            )
    }

/**
 * Reads the full content as a [Bitmap].
 */
public suspend fun Readable.readAsBitmap(): Try<Bitmap, DecodeError> =
    read()
        .mapFailure { DecodeError.Reading(it) }
        .flatMap { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?.let { Try.success(it) }
                ?: Try.failure(
                    DecodeError.Decoding(
                        MessageError("Could not decode resource as a bitmap.")
                    )
                )
        }

/**
 * Returns whether the content is a JSON object containing all of the given root keys.
 */
public suspend fun Readable.containsJsonKeys(
    vararg keys: String
): Try<Boolean, DecodeError> {
    val json = readAsJson()
        .getOrElse { return Try.failure(it) }
    return Try.success(json.keys().asSequence().toSet().containsAll(keys.toList()))
}