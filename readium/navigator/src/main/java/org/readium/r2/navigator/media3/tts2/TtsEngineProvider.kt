/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
interface TtsEngineProvider<S : TtsSettings, P : TtsPreferences<P>, E : PreferencesEditor<P>> {

    suspend fun createEngine(publication: Publication, initialPreferences: P): TtsEngine<S, P>?

    fun createPreferencesEditor(publication: Publication, initialPreferences: P): E

    fun createEmptyPreferences(): P
}
