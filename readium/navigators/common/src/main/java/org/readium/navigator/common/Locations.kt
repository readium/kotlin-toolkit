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
 *  Location of the navigator.
 */
@ExperimentalReadiumApi
public interface Location {

    public val href: Url
}

@ExperimentalReadiumApi
public interface ExportableLocation : Location {

    public fun toLocator(): Locator
}

@ExperimentalReadiumApi
public interface TextAnchorLocation : Location {

    public val textAnchor: TextAnchor
}

@ExperimentalReadiumApi
public interface TextQuoteLocation : Location {

    public val textQuote: TextQuote
}

@ExperimentalReadiumApi
public interface CssLocation {

    public val cssSelector: CssSelector?
}

@ExperimentalReadiumApi
public interface ProgressionLocation : Location {

    public val progression: Progression
}

@ExperimentalReadiumApi
public interface PositionLocation : Location {

    public val position: Position
}
