/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.ReadingProgression

@OptIn(ExperimentalReadiumApi::class)
class PdfiumPreferencesEditor(
    private val edit: ((MutablePreferences).() -> Unit) -> Unit,
    private val settings: PdfiumSettings,
    private val preferences: Preferences
) : FixedLayoutPreferencesEditor {

    override fun setReadingProgression(readingProgression: ReadingProgression?) = edit {
        set(settings.readingProgression, readingProgression)
    }

    override fun setScrollAxis(axis: Axis) = edit {
        set(settings.scrollAxis, axis)
    }

    override fun setFit(fit: Fit) = edit {
        set(settings.fit, fit)
    }

    private val <V> Setting<V>.prefOrValue: V get() =
        preferences[this] ?: value
}
