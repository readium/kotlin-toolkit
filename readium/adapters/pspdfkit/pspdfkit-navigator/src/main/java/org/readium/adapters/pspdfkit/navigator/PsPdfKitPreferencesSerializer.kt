/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.readium.r2.navigator.preferences.PreferencesSerializer
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * JSON serializer of [PsPdfKitPreferences].
 *
 * Serialization and deserialization can be configured through the [json] argument.
 */
@ExperimentalReadiumApi
class PsPdfKitPreferencesSerializer(
    private val json: Json = Json
) : PreferencesSerializer<PsPdfKitPreferences> {

    /**
     * Serialize [PsPdfKitPreferences] into a JSON string.
     */
    override fun serialize(preferences: PsPdfKitPreferences): String =
        json.encodeToString(serializer(), preferences)

    /**
     * Deserialize [PsPdfKitPreferences] from a JSON string.
     */
    override fun deserialize(preferences: String): PsPdfKitPreferences =
        json.decodeFromString(preferences)
}
