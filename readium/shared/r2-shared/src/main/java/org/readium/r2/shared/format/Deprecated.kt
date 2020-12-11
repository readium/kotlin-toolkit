/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.format

import com.github.kittinunf.fuel.core.Response
import org.readium.r2.shared.util.mediatype.Sniffer
import org.readium.r2.shared.util.mediatype.SnifferContext
import org.readium.r2.shared.util.mediatype.Sniffers
import org.readium.r2.shared.util.mediatype.sniffMediaType
import java.net.HttpURLConnection
import org.readium.r2.shared.util.mediatype.MediaType as NewMediaType

@Deprecated("Moved to another package", replaceWith = ReplaceWith("org.readium.r2.shared.util.mediatype.MediaType"), level = DeprecationLevel.ERROR)
typealias MediaType = NewMediaType
@Deprecated("Format and MediaType got merged together", replaceWith = ReplaceWith("org.readium.r2.shared.util.mediatype.MediaType"), level = DeprecationLevel.ERROR)
typealias Format = NewMediaType
@Deprecated("Renamed Sniffer", replaceWith = ReplaceWith("org.readium.r2.shared.util.mediatype.Sniffer"), level = DeprecationLevel.ERROR)
typealias FormatSniffer = Sniffer
@Deprecated("Renamed Sniffers", replaceWith = ReplaceWith("org.readium.r2.shared.util.mediatype.Sniffers"), level = DeprecationLevel.ERROR)
typealias FormatSniffers = Sniffers
@Deprecated("Renamed SnifferContext", replaceWith = ReplaceWith("org.readium.r2.shared.util.mediatype.SnifferContext"), level = DeprecationLevel.ERROR)
typealias FormatSnifferContext = SnifferContext

@Deprecated("Renamed to another package", ReplaceWith("org.readium.r2.shared.util.mediatype.sniffMediaType"), level = DeprecationLevel.ERROR)
suspend fun Response.sniffFormat(mediaTypes: List<String> = emptyList(), fileExtensions: List<String> = emptyList(), sniffers: List<Sniffer> = NewMediaType.sniffers): NewMediaType? =
    sniffMediaType(mediaTypes, fileExtensions, sniffers)

@Deprecated("Renamed to another package", ReplaceWith("org.readium.r2.shared.util.mediatype.sniffMediaType"), level = DeprecationLevel.ERROR)
suspend fun HttpURLConnection.sniffFormat(bytes: (() -> ByteArray)? = null, mediaTypes: List<String> = emptyList(), fileExtensions: List<String> = emptyList(), sniffers: List<Sniffer> = NewMediaType.sniffers): NewMediaType? =
    sniffMediaType(bytes, mediaTypes, fileExtensions, sniffers)
