/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import kotlinx.serialization.json.Json
import org.readium.r2.navigator.preferences.PreferencesSerializer
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * JSON serializer of [EpubPreferences].
 */
@ExperimentalReadiumApi
class EpubPreferencesSerializer : PreferencesSerializer<EpubPreferences> {

    override fun serialize(preferences: EpubPreferences): String =
        Json.encodeToString(EpubPreferences.serializer(), preferences)

    override fun deserialize(preferences: String): EpubPreferences =
        Json.decodeFromString(EpubPreferences.serializer(), preferences)
}
