/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

/**
 * Default values for the EPUB navigator.
 *
 * These values will be used when no publication metadata or user preference takes precedence.
 *
 * @see EpubPreferences
 */
public data class EpubDefaults @ExperimentalReadiumApi constructor(
    val columnCount: ColumnCount? = null,
    val fontSize: Double? = null,
    val fontWeight: Double? = null,
    val hyphens: Boolean? = null,
    val imageFilter: ImageFilter? = null,
    val language: Language? = null,
    val letterSpacing: Double? = null,
    val ligatures: Boolean? = null,
    val lineHeight: Double? = null,
    val pageMargins: Double? = null,
    val paragraphIndent: Double? = null,
    val paragraphSpacing: Double? = null,
    val publisherStyles: Boolean? = null,
    val readingProgression: ReadingProgression? = null,
    val scroll: Boolean? = null,
    val spread: Spread? = null,
    val textAlign: TextAlign? = null,
    val textNormalization: Boolean? = null,
    val typeScale: Double? = null,
    val wordSpacing: Double? = null,
)
