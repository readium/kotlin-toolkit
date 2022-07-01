package org.readium.r2.navigator.util

import android.os.Parcel
import kotlinx.parcelize.Parceler
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.readium.r2.shared.extensions.tryOrLog

/**
 * Implementation of a [Parceler] to be used with [@Parcelize] to serialize JSON objects.
 */
internal object JsonParceler : Parceler<JsonObject> {

    override fun create(parcel: Parcel): JsonObject =
        tryOrLog {
            parcel.readString()
                ?.let { Json.decodeFromString(it) }
        } ?: JsonObject(emptyMap())

    override fun JsonObject.write(parcel: Parcel, flags: Int) {
        tryOrLog {
            parcel.writeString(toString())
        }
    }
}
