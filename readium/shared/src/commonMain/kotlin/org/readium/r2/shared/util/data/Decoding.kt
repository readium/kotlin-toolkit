/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.OutOfMemoryError
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.json.LenientJson
import org.readium.r2.shared.util.tryRecover
import org.readium.r2.shared.util.xml.ElementNode
import org.readium.r2.shared.util.xml.XmlParser

/**
 * Errors produced when trying to decode content.
 */
public sealed class DecodeError(
    override val message: String,
    override val cause: Error,
) : Error {

    /**
     * Content could not be successfully decoded because there is not enough memory available.
     */
    public class OutOfMemory(override val cause: ThrowableError<OutOfMemoryError>) :
        DecodeError("The resource is too large to be read on this device.", cause) {

        public constructor(error: OutOfMemoryError) : this(ThrowableError(error))
    }

    /**
     * Content could not be successfully decoded because it doesn't match what was expected.
     */
    public class Decoding(cause: Error) :
        DecodeError("Decoding Error", cause)
}

/**
 * Decodes receiver properly wrapping exceptions into [DecodeError]s.
 */
@InternalReadiumApi
public suspend fun <R, S> S.decode(
    block: (value: S) -> R,
    wrapError: (Exception) -> Error,
): Try<R, DecodeError> =
    withContext(Dispatchers.Default) {
        try {
            Try.success(block(this@decode))
        } catch (e: Exception) {
            Try.failure(DecodeError.Decoding(wrapError(e)))
        } catch (e: OutOfMemoryError) {
            Try.failure(DecodeError.OutOfMemory(e))
        }
    }

/**
 * Content as a UTF-8 string.
 */
public suspend fun ByteArray.decodeString(): Try<String, DecodeError> =
    decode(
        { it.decodeToString() },
        { DebugError("Content is not a valid UTF-8 string.", ThrowableError(it)) }
    )

/** Content as an XML document. */
public suspend fun ByteArray.decodeXml(): Try<ElementNode, DecodeError> =
    decode(
        { XmlParser().parse(it) },
        { DebugError("Content is not a valid XML document.", ThrowableError(it)) }
    )

/**
 * Content parsed from JSON.
 */
public suspend fun ByteArray.decodeJson(): Try<JsonObject, DecodeError> =
    decodeString().flatMap { string ->
        decode(
            { LenientJson.parseToJsonElement(string) as JsonObject },
            { DebugError("Content is not valid JSON.", ThrowableError(it)) }
        )
    }

@Suppress("RedundantSuspendModifier")
@InternalReadiumApi
public suspend inline fun <R> Try<ByteArray, ReadError>.decodeOrElse(
    decode: (value: ByteArray) -> Try<R, DecodeError>,
    recover: (DecodeError.Decoding) -> R,
): Try<R, ReadError> =
    flatMap {
        decode(it)
            .tryRecover { error ->
                when (error) {
                    is DecodeError.OutOfMemory ->
                        Try.failure(ReadError.OutOfMemory(error.cause))
                    is DecodeError.Decoding ->
                        Try.success(recover(error))
                }
            }
    }

@Suppress("RedundantSuspendModifier")
@InternalReadiumApi
public suspend inline fun <R> Try<ByteArray, ReadError>.decodeOrNull(
    decode: (value: ByteArray) -> Try<R, DecodeError>,
): R? =
    flatMap { decode(it) }.getOrNull()

@InternalReadiumApi
public suspend inline fun <R> Readable.readDecodeOrElse(
    decode: (value: ByteArray) -> Try<R, DecodeError>,
    recoverRead: (ReadError) -> R,
    recoverDecode: (DecodeError.Decoding) -> R,
): R =
    read().decodeOrElse(decode, recoverDecode).getOrElse(recoverRead)

@InternalReadiumApi
public suspend inline fun <R> Readable.readDecodeOrElse(
    decode: (value: ByteArray) -> Try<R, DecodeError>,
    recover: (ReadError) -> R,
): R =
    readDecodeOrElse(decode, recover) { recover(ReadError.Decoding(it)) }

@InternalReadiumApi
public suspend inline fun <R> Readable.readDecodeOrNull(
    decode: (value: ByteArray) -> Try<R, DecodeError>,
): R? =
    read().decodeOrNull(decode)
