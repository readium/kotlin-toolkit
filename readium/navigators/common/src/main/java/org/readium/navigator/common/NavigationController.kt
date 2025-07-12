/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url

/**
 * This controller enables to navigate through a publication and reports the current location.
 */
@ExperimentalReadiumApi
public interface NavigationController<L : ExportableLocation, G : GoLocation> {

    public val location: L

    public suspend fun goTo(location: G)

    public suspend fun goTo(location: L)

    public suspend fun goTo(url: Url)
}

/**
 * Location the navigator can go to.
 */
@ExperimentalReadiumApi
public interface GoLocation : Location
