/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi

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

/**
 * [SettingActivator] which checks that the setting with given [key] is [value] and sets it up in
 * [Preferences] when it is asked to.
 */
@InternalReadiumApi
@OptIn(ExperimentalReadiumApi::class)
class ForcePreferenceSettingActivator<V>(
    val key: Setting.Key<V>,
    val value: V,
    val valueFromPreferences: (Preferences) -> V
) : SettingActivator {

    override fun isActiveWithPreferences(preferences: Preferences): Boolean =
        valueFromPreferences(preferences) == value

    override fun activateInPreferences(preferences: MutablePreferences) {
        preferences[key] = value
    }
}

/**
 * [SettingActivator] which checks that the setting with given [key] is [value].
 */
@InternalReadiumApi
@OptIn(ExperimentalReadiumApi::class)
class RequirePreferenceSettingActivator<V>(
    val value: V,
    val valueFromPreferences: (Preferences) -> V
) : SettingActivator {

    override fun isActiveWithPreferences(preferences: Preferences): Boolean =
        valueFromPreferences(preferences) == value

    override fun activateInPreferences(preferences: MutablePreferences) {
        // Nothing
    }
}


/**
 * A [SettingActivator] combining two activators.
 */
@ExperimentalReadiumApi
class CombinedSettingActivator(
    val outer: SettingActivator,
    val inner: SettingActivator
) : SettingActivator {

    override fun isActiveWithPreferences(preferences: Preferences): Boolean =
        inner.isActiveWithPreferences(preferences) && outer.isActiveWithPreferences(preferences)

    override fun activateInPreferences(preferences: MutablePreferences) {
        inner.activateInPreferences(preferences)
        outer.activateInPreferences(preferences)
    }
}

/**
 * Combines the [SettingActivator] receiver with the given [other] activator.
 */
@ExperimentalReadiumApi
operator fun SettingActivator.plus(other: SettingActivator): SettingActivator =
    CombinedSettingActivator(this, other)
