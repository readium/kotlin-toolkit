/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * A [SettingActivator] ensures that the condition required for a setting to be active are met in
 * a set of [Preferences].
 *
 * For example, the EPUB navigator requires the [Setting.PUBLISHER_STYLES] to be disabled to
 * render the [Setting.WORD_SPACING] setting.
 */
@ExperimentalReadiumApi
interface SettingActivator {

    /**
     * Indicates whether the setting is active in the given set of [preferences].
     */
    fun isActiveWithPreferences(preferences: Preferences): Boolean

    /**
     * Updates the given [preferences] to make sure the setting is active.
     */
    fun activateInPreferences(preferences: MutablePreferences)
}

/**
 * Default implementation of [SettingActivator] for a setting that is always considered active.
 */
@ExperimentalReadiumApi
object NullSettingActivator : SettingActivator {
    override fun isActiveWithPreferences(preferences: Preferences): Boolean = true
    override fun activateInPreferences(preferences: MutablePreferences) {}
}
