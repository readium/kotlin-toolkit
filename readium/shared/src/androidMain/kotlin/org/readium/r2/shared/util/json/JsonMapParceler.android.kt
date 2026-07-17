/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.json

import android.os.Parcel
import kotlinx.serialization.json.JsonObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.Parceler
import org.readium.r2.shared.util.logging.ReadiumLog

@InternalReadiumApi
public actual object JsonMapParceler : Parceler<Map<String, Any>> {

    override fun create(parcel: Parcel): Map<String, Any> =
        try {
            parcel.readString()
                ?.toJsonObjectOrNull()
                ?.toMap()
                ?: emptyMap()
        } catch (e: Exception) {
            ReadiumLog.e(e, "Failed to read a JSON map from a Parcel")
            emptyMap()
        }

    override fun Map<String, Any>.write(parcel: Parcel, flags: Int) {
        try {
            parcel.writeString((toJsonObject() ?: JsonObject(emptyMap())).toString())
        } catch (e: Exception) {
            ReadiumLog.e(e, "Failed to write a JSON map into a Parcel")
        }
    }
}
