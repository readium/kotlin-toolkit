/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.readaloud.preferences

import kotlinx.serialization.Serializable
import org.readium.navigator.common.Preferences
import org.readium.navigator.media.readaloud.TtsVoice
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.guided.GuidedNavigationRole
import org.readium.r2.shared.util.Language

/**
 * Preferences for the the ReadAloudNavigator.
 *
 *  @param language Language of the publication content.
 *  @param pitch Playback pitch rate.
 *  @param speed Playback speed rate.
 *  @param voices Map of preferred voices for specific languages.
 *  @param escapableRoles Roles that will be considered as escapable.
 *  @param skippableRoles Roles that will be considered as skippable.
 *  @param readContinuously Do not pause after reading each content item.
 */
@ExperimentalReadiumApi
@Serializable
public data class ReadAloudPreferences(
    val language: Language? = null,
    val pitch: Double? = null,
    val speed: Double? = null,
    val voices: Map<Language, TtsVoice.Id>? = null,
    val escapableRoles: Set<GuidedNavigationRole>? = null,
    val skippableRoles: Set<GuidedNavigationRole>? = null,
    val readContinuously: Boolean? = true,
) : Preferences<ReadAloudPreferences> {

    init {
        require(pitch == null || pitch > 0)
        require(speed == null || speed > 0)
    }

    public override fun plus(other: ReadAloudPreferences): ReadAloudPreferences =
        ReadAloudPreferences(
            language = other.language ?: language,
            pitch = other.pitch ?: pitch,
            speed = other.speed ?: speed,
            voices = other.voices ?: voices,
            escapableRoles = other.escapableRoles ?: escapableRoles,
            skippableRoles = other.skippableRoles ?: skippableRoles,
            readContinuously = other.readContinuously ?: readContinuously
        )
}
