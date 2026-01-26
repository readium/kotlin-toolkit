/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.readaloud.preferences

import java.util.Locale
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.guided.GuidedNavigationRole
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.ASIDE
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.BIBLIOGRAPHY
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.CELL
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.ENDNOTES
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.FIGURE
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.FOOTNOTE
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.LANDMARKS
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.LIST
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.LIST_ITEM
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.LOA
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.LOI
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.LOT
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.LOV
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.NOTEREF
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.PAGEBREAK
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.PULLQUOTE
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.ROW
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.TABLE
import org.readium.r2.shared.guided.GuidedNavigationRole.Companion.TOC
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.util.Language

@OptIn(ExperimentalReadiumApi::class)
internal class ReadAloudSettingsResolver(
    private val metadata: Metadata,
    private val defaults: ReadAloudDefaults,
) {

    fun settings(preferences: ReadAloudPreferences): ReadAloudSettings {
        val language = preferences.language
            ?: metadata.language
            ?: defaults.language
            ?: Language(Locale.getDefault())

        val skippableRoles: Set<GuidedNavigationRole> =
            preferences.skippableRoles
                ?: defaults.skippableRoles
                ?: setOf(
                    ASIDE, BIBLIOGRAPHY, ENDNOTES, FOOTNOTE, NOTEREF, PULLQUOTE,
                    LANDMARKS, LOA, LOI, LOT, LOV, PAGEBREAK, TOC
                )

        val escapableRoles: Set<GuidedNavigationRole> =
            preferences.escapableRoles
                ?: defaults.escapableRoles
                ?: setOf(ASIDE, FIGURE, LIST, LIST_ITEM, TABLE, ROW, CELL)

        val languagesWithPreferredVoice =
            preferences.voices.orEmpty().keys.map { it.removeRegion() }

        val filteredDefaultVoices = defaults.voices.orEmpty()
            .filter { it.key.removeRegion() !in languagesWithPreferredVoice }

        val voices = filteredDefaultVoices + preferences.voices.orEmpty()

        return ReadAloudSettings(
            language = language,
            voices = voices,
            pitch = preferences.pitch ?: defaults.pitch ?: 1.0,
            speed = preferences.speed ?: defaults.speed ?: 1.0,
            overrideContentLanguage = preferences.language != null,
            escapableRoles = escapableRoles,
            skippableRoles = skippableRoles,
            readContinuously = preferences.readContinuously ?: defaults.readContinuously ?: true
        )
    }
}
