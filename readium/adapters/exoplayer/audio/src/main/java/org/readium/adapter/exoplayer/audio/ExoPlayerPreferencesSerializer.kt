/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.exoplayer.audio

import kotlinx.serialization.json.Json
import org.readium.r2.navigator.preferences.PreferencesSerializer

/**
 * JSON serializer of [ExoPlayerPreferences].
 */
public class ExoPlayerPreferencesSerializer : PreferencesSerializer<ExoPlayerPreferences> {

    override fun serialize(preferences: ExoPlayerPreferences): String =
        Json.encodeToString(ExoPlayerPreferences.serializer(), preferences)

    override fun deserialize(preferences: String): ExoPlayerPreferences =
        Json.decodeFromString(ExoPlayerPreferences.serializer(), preferences)
}
