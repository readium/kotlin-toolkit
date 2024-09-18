/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.tts.android

import kotlinx.serialization.json.Json
import org.readium.r2.navigator.preferences.PreferencesSerializer

/**
 * JSON serializer of [AndroidTtsPreferences].
 */
public class AndroidTtsPreferencesSerializer : PreferencesSerializer<AndroidTtsPreferences> {

    override fun serialize(preferences: AndroidTtsPreferences): String =
        Json.encodeToString(AndroidTtsPreferences.serializer(), preferences)

    override fun deserialize(preferences: String): AndroidTtsPreferences =
        Json.decodeFromString(AndroidTtsPreferences.serializer(), preferences)
}
