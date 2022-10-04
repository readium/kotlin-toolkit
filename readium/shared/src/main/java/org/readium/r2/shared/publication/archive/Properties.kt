/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.archive

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.optNullableBoolean
import org.readium.r2.shared.extensions.optNullableLong
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log

// Archive Link Properties Extension

/**
 * Holds information about how the resource is stored in the publication archive.
 *
 * @param entryLength The length of the entry stored in the archive. It might be a compressed length
 *        if the entry is deflated.
 * @param isEntryCompressed Indicates whether the entry was compressed before being stored in the
 *        archive.
 */
@Parcelize
data class ArchiveProperties(
    val entryLength: Long,
    val isEntryCompressed: Boolean
) : JSONable, Parcelable {

    override fun toJSON(): JSONObject = JSONObject().apply {
        put("entryLength", entryLength)
        put("isEntryCompressed", isEntryCompressed)
    }

    companion object {
        fun fromJSON(json: JSONObject?, warnings: WarningLogger? = null): ArchiveProperties? {
            json ?: return null

            val entryLength = json.optNullableLong("entryLength")
            val isEntryCompressed = json.optNullableBoolean("isEntryCompressed")
            if (entryLength == null || isEntryCompressed == null) {
                warnings?.log(ArchiveProperties::class.java, "[entryLength] and [isEntryCompressed] are required", json)
                return null
            }

            return ArchiveProperties(entryLength = entryLength, isEntryCompressed = isEntryCompressed)
        }

    }
}

/**
 * Provides information about how the resource is stored in the publication archive.
 */
val Properties.archive: ArchiveProperties?
    get() = (this["archive"] as? Map<*, *>)
        ?.let { ArchiveProperties.fromJSON(JSONObject(it)) }
