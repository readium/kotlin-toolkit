/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.readaloud.preferences

import org.readium.navigator.common.Settings
import org.readium.navigator.media.readaloud.TtsVoice
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.guided.GuidedNavigationRole
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
public data class ReadAloudSettings(
    val language: Language,
    val overrideContentLanguage: Boolean,
    val pitch: Double,
    val speed: Double,
    val voices: Map<Language, TtsVoice.Id>,
    val escapableRoles: Set<GuidedNavigationRole>,
    val skippableRoles: Set<GuidedNavigationRole>,
    val readContinuously: Boolean,
) : Settings
