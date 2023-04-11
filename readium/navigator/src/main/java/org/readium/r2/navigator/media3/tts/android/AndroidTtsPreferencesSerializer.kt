/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts.android

import kotlinx.serialization.json.Json
import org.readium.r2.navigator.preferences.PreferencesSerializer
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * JSON serializer of [AndroidTtsPreferences].
 */
@ExperimentalReadiumApi
class AndroidTtsPreferencesSerializer : PreferencesSerializer<AndroidTtsPreferences> {

    override fun serialize(preferences: AndroidTtsPreferences): String =
        Json.encodeToString(AndroidTtsPreferences.serializer(), preferences)

    override fun deserialize(preferences: String): AndroidTtsPreferences =
        Json.decodeFromString(AndroidTtsPreferences.serializer(), preferences)
}
