package org.readium.navigator.web.reflowable.resource

import org.readium.navigator.common.Position
import org.readium.navigator.common.Progression
import org.readium.r2.shared.ExperimentalReadiumApi

/** Information about the visible portion of the publication. */
@ExperimentalReadiumApi
public data class ReflowableWebViewport(

    /**
     * Range of visible reading order resources.
     */
    public val readingOrder: ClosedRange<Int>,

    /**
     * Range of visible scroll progressions for each visible reading order resource.
     */
    public val progressions: List<ClosedRange<Progression>>,

    /**
     * Range of visible positions.
     */
    public val positions: ClosedRange<Position>,
)
