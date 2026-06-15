/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */
@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.common

import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Interactive editor of settings.
 *
 * This can be used as a helper for a user preferences screen.
 */
@ExperimentalReadiumApi
public interface PreferencesEditor<P : Preferences<P>, S : Settings> {

    /**
     * The current preferences.
     */
    public val preferences: P

    /**
     * The current computed settings.
     */
    public val settings: S

    /**
     * Unset all preferences.
     */
    public fun clear()
}
