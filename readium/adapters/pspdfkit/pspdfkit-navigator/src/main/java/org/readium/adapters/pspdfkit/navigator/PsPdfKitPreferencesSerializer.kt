/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import kotlinx.serialization.json.Json
import org.readium.r2.navigator.preferences.PreferencesSerializer
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * JSON serializer of [PsPdfKitPreferences].
 */
@ExperimentalReadiumApi
class PsPdfKitPreferencesSerializer : PreferencesSerializer<PsPdfKitPreferences> {

    override fun serialize(preferences: PsPdfKitPreferences): String =
        Json.encodeToString(PsPdfKitPreferences.serializer(), preferences)

    override fun deserialize(preferences: String): PsPdfKitPreferences =
        Json.decodeFromString(PsPdfKitPreferences.serializer(), preferences)
}
