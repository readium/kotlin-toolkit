/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.readium.r2.navigator.preferences.PreferencesSerializer
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * JSON serializer of [EpubPreferences].
 *
 * Serialization and deserialization can be configured through the [json] argument.
 */
@ExperimentalReadiumApi
class EpubPreferencesSerializer(
    private val json: Json = Json
) : PreferencesSerializer<EpubPreferences> {

    override fun serialize(preferences: EpubPreferences): String =
        json.encodeToString(serializer(), preferences)

    override fun deserialize(preferences: String): EpubPreferences =
        json.decodeFromString(preferences)
}
