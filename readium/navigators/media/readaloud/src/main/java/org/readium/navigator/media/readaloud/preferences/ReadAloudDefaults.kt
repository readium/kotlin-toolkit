/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.readaloud.preferences

import org.readium.navigator.media.readaloud.TtsVoice
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.guided.GuidedNavigationRole
import org.readium.r2.shared.util.Language

/**
 * Default values for the ReadAloudNavigator.
 *
 * These values will be used as a last resort when no user preference takes precedence.
 *
 * @see ReadAloudPreferences
 */
@ExperimentalReadiumApi
public data class ReadAloudDefaults(
    val language: Language? = null,
    val pitch: Double? = null,
    val speed: Double? = null,
    val voices: Map<Language, TtsVoice.Id>? = null,
    val escapableRoles: Set<GuidedNavigationRole>? = null,
    val skippableRoles: Set<GuidedNavigationRole>? = null,
    val readContinuously: Boolean? = null,
) {
    init {
        require(pitch == null || pitch > 0)
        require(speed == null || speed > 0)
    }
}
