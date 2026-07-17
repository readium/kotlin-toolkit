/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import kotlinx.serialization.json.JsonObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.xml.ElementNode
import org.readium.r2.shared.util.xml.XmlParser

/**
 * Content as plain text, decoded with the given [charset].
 */
public suspend fun ByteArray.decodeString(
    charset: Charset,
): Try<String, DecodeError> =
    decode(
        { String(it, charset = charset) },
        { DebugError("Content is not a valid $charset string.", ThrowableError(it)) }
    )

// TODO(kmp phase-06): move to commonMain once XmlParser is backed by xmlutil.

/** Content as an XML document. */
public suspend fun ByteArray.decodeXml(): Try<ElementNode, DecodeError> =
    decode(
        { XmlParser().parse(ByteArrayInputStream(it)) },
        { DebugError("Content is not a valid XML document.", ThrowableError(it)) }
    )

// TODO(kmp phase-07): move to commonMain once Manifest is in commonMain.

/**
 * Readium Web Publication Manifest parsed from the content.
 */
public suspend fun ByteArray.decodeRwpm(): Try<Manifest, DecodeError> =
    decodeJson().flatMap { it.decodeRwpm() }

// TODO(kmp phase-07): move to commonMain once Manifest is in commonMain.

/**
 * Readium Web Publication Manifest parsed from JSON.
 */
public suspend fun JsonObject.decodeRwpm(): Try<Manifest, DecodeError> =
    decode(
        {
            Manifest.fromJSON(this)
                ?: throw Exception("Manifest.fromJSON returned null")
        },
        { DebugError("Content is not a valid RWPM.") }
    )

// TODO(kmp phase-07): the KMP image type will provide a common `decodeBitmap`.

/**
 * Reads the full content as a [Bitmap].
 */
public suspend fun ByteArray.decodeBitmap(): Try<Bitmap, DecodeError> =
    decode(
        {
            BitmapFactory.decodeByteArray(this, 0, size)
                ?: throw Exception("BitmapFactory returned null.")
        },
        { DebugError("Could not decode content as a bitmap.") }
    )
