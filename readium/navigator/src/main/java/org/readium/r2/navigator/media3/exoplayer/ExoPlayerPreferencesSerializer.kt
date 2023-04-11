/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.exoplayer

import kotlinx.serialization.json.Json
import org.readium.r2.navigator.preferences.PreferencesSerializer
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * JSON serializer of [ExoPlayerPreferences].
 */
@ExperimentalReadiumApi
class ExoPlayerPreferencesSerializer : PreferencesSerializer<ExoPlayerPreferences> {

    override fun serialize(preferences: ExoPlayerPreferences): String =
        Json.encodeToString(ExoPlayerPreferences.serializer(), preferences)

    override fun deserialize(preferences: String): ExoPlayerPreferences =
        Json.decodeFromString(ExoPlayerPreferences.serializer(), preferences)
}
