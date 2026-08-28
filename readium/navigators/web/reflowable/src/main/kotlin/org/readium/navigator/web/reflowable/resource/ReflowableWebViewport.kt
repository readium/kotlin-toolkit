/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable.resource

import org.readium.navigator.common.Position
import org.readium.navigator.common.Progression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url

/** Information about the visible portion of the publication. */
@ExperimentalReadiumApi
public data class ReflowableWebViewport(

    /**
     * Range of visible reading order resources.
     */
    public val readingOrder: List<Url>,

    /**
     * Range of visible scroll progressions for each visible reading order resource.
     */
    public val progressions: Map<Url, ClosedRange<Progression>>,

    /**
     * Range of visible positions.
     */
    public val positions: ClosedRange<Position>,
)
