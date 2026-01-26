/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url

/**
 *  A location in a publication.
 */
@ExperimentalReadiumApi
public interface Location {

    public val href: Url
}

/**
 * A [Location] which can be converted to a [Locator].
 */
@ExperimentalReadiumApi
public interface ExportableLocation : Location {

    public fun toLocator(): Locator
}

/**
 * A [Location] including a [TextQuote].
 */
@ExperimentalReadiumApi
public interface TextQuoteLocation : Location {

    public val textQuote: TextQuote
}

/**
 * A [Location] including a [TextAnchor].
 */
@ExperimentalReadiumApi
public interface TextAnchorLocation : Location {

    public val textAnchor: TextAnchor
}

/**
 * A [Location] including a [CssSelector].
 */
@ExperimentalReadiumApi
public interface CssSelectorLocation : Location {

    public val cssSelector: CssSelector?
}

/**
 * A [Location] including a [Progression].
 */
@ExperimentalReadiumApi
public interface ProgressionLocation : Location {

    public val progression: Progression
}

/**
 * A [Location] including a [Position].
 */
@ExperimentalReadiumApi
public interface PositionLocation : Location {

    public val position: Position
}

@ExperimentalReadiumApi
public interface TimeOffsetLocation : Location {

    public val timeOffset: TimeOffset
}
