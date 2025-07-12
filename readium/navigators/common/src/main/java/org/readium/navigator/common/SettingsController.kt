/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * A controller for rendition settings.
 */
@ExperimentalReadiumApi
public interface SettingsController<S : Settings> {

    /**
     * Sets the current rendition settings. This property must be observable.
     */
    public var settings: S
}

/**
 * Marker interface for the [Settings] properties holder.
 */
@ExperimentalReadiumApi
public interface Settings
