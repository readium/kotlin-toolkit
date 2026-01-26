/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.media.readaloud

import org.readium.navigator.common.CssSelector
import org.readium.navigator.common.Location
import org.readium.navigator.common.TextAnchor
import org.readium.navigator.common.TextQuote
import org.readium.navigator.common.TimeOffset
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
public sealed interface ReadAloudLocation : Location

@ExperimentalReadiumApi
public data class ReadAloudTextLocation(
    override val href: Url,
    public val textAnchor: TextAnchor?,
    public val cssSelector: CssSelector?,
) : ReadAloudLocation

@ExperimentalReadiumApi
public data class ReadAloudAudioLocation(
    override val href: Url,
    public val timeOffset: TimeOffset?,
) : ReadAloudLocation

@ExperimentalReadiumApi
public sealed interface ReadAloudHighlightLocation : Location

@ExperimentalReadiumApi
public data class ReadAloudTextHighlightLocation(
    override val href: Url,
    public val textQuote: TextQuote?,
    public val cssSelector: CssSelector?,
) : ReadAloudHighlightLocation
