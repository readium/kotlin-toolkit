/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import androidx.compose.runtime.Stable
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * The state of the rendition, giving access to a [NavigationController] after the first composition.
 */
@ExperimentalReadiumApi
@Stable
public interface RenditionState<N : NavigationController<*, *>> {

    /**
     * The rendition controller if available. This property must be observable.
     */
    public val controller: N?
}
