/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * A controller for rendition settings that can be updated using preferences.
 */
@ExperimentalReadiumApi
public interface PreferencesController<P : Preferences<P>, S : Settings> {

    /**
     * The current preferences.
     */
    public var preferences: P

    /**
     * The current resolved settings.
     */
    public val settings: S
}
