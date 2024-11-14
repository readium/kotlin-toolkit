/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import androidx.compose.runtime.State
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
public interface NavigationController<L : Location, G : GoLocation> {

    public val location: State<L>

    public suspend fun goTo(location: G)

    public suspend fun goTo(location: L)

    public suspend fun goTo(location: HyperlinkLocation)
}

/**
 *  Location of the navigator.
 */
@ExperimentalReadiumApi
public interface Location {

    public val href: Url
}

/**
 * Location the navigator can go to.
 */
@ExperimentalReadiumApi
public interface GoLocation
