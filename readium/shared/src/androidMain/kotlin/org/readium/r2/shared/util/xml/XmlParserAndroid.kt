/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.xml

import java.io.IOException
import java.io.InputStream
import org.readium.r2.shared.InternalReadiumApi

/**
 * Parses the XML document read from the given [stream] into an [ElementNode] tree.
 *
 * The [stream] is fully read and closed.
 */
@InternalReadiumApi
@Throws(XmlParserException::class, IOException::class)
public fun XmlParser.parse(stream: InputStream): ElementNode =
    parse(stream.use { it.readBytes() })
