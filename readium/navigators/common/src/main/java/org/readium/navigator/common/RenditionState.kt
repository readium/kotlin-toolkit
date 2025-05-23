/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * The state of the rendition, giving access to a [NavigationController] after the first composition.
 */
@ExperimentalReadiumApi
public interface RenditionState<N : NavigationController<*, *>> {

    public val controller: N?
}
