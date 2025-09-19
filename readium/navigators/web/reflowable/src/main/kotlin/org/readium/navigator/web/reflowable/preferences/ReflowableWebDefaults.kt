/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable.preferences

import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.ImageFilter
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

/**
 * Default values for the Reflowable Web navigator.
 *
 * These values will be used when no publication metadata or user preference takes precedence.
 *
 * @see ReflowableWebPreferences
 */
@ExperimentalReadiumApi
public data class ReflowableWebDefaults(
    val backgroundColor: Color? = null,
    val columnCount: Int? = null,
    val fontSize: Double? = null,
    val fontWeight: Double? = null,
    val hyphens: Boolean? = null,
    val imageFilter: ImageFilter? = null,
    val language: Language? = null,
    val letterSpacing: Double? = null,
    val ligatures: Boolean? = null,
    val lineHeight: Double? = null,
    val linkColor: Color? = null,
    val maximalLineLength: Double? = null,
    val minimalLineLength: Double? = null,
    val optimalLineLength: Double? = null,
    val overridePublisherColors: Boolean? = null,
    val pageMargins: Double? = null,
    val paragraphIndent: Double? = null,
    val paragraphSpacing: Double? = null,
    val readingProgression: ReadingProgression? = null,
    val scroll: Boolean? = null,
    val textAlign: TextAlign? = null,
    val textColor: Color? = null,
    val textNormalization: Boolean? = null,
    val visitedLinkColor: Color? = null,
    val wordSpacing: Double? = null,
)
