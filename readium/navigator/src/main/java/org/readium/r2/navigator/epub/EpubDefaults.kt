/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
data class EpubDefaults(
    val readingProgression: ReadingProgression? = null,
    val language: Language? = null,
    val scroll: Boolean? = null,
    val spread: Spread? = null,
    val columnCount: ColumnCount? = null,
    val fontSize: Double? = null,
    val lineHeight: Double? = null
)
