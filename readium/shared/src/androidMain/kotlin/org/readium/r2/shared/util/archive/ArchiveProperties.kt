/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.archive

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.util.json.optNullableBoolean
import org.readium.r2.shared.util.json.optNullableLong
import org.readium.r2.shared.util.json.toMap
import org.readium.r2.shared.util.json.toJsonObject
import org.readium.r2.shared.util.resource.Resource

/**
 * Holds information about how the resource is stored in the archive.
 *
 * @param entryLength The length of the entry stored in the archive. It might be a compressed length
 *        if the entry is deflated.
 * @param isEntryCompressed Indicates whether the entry was compressed before being stored in the
 *        archive.
 */
public data class ArchiveProperties(
    val entryLength: Long,
    val isEntryCompressed: Boolean,
) : JSONable {

    override fun toJSON(): JsonObject = buildJsonObject {
        put("entryLength", entryLength)
        put("isEntryCompressed", isEntryCompressed)
    }

    public companion object {
        public fun fromJSON(json: JsonObject?): ArchiveProperties? {
            json ?: return null

            val entryLength = json.optNullableLong("entryLength")
            val isEntryCompressed = json.optNullableBoolean("isEntryCompressed")
            if (entryLength == null || isEntryCompressed == null) {
                return null
            }
            return ArchiveProperties(
                entryLength = entryLength,
                isEntryCompressed = isEntryCompressed
            )
        }
    }
}

private const val ARCHIVE_KEY = "https://readium.org/webpub-manifest/properties#archive"

public val Resource.Properties.archive: ArchiveProperties?
    get() = (this[ARCHIVE_KEY] as? Map<*, *>)
        ?.let { ArchiveProperties.fromJSON(it.toJsonObject()) }

public var Resource.Properties.Builder.archive: ArchiveProperties?
    get() = (this[ARCHIVE_KEY] as? Map<*, *>)
        ?.let { ArchiveProperties.fromJSON(it.toJsonObject()) }
    set(value) {
        if (value == null) {
            remove(ARCHIVE_KEY)
        } else {
            put(ARCHIVE_KEY, value.toJSON().toMap())
        }
    }
