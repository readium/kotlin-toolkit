/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable.preferences

import org.readium.navigator.common.Settings
import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ImageFilter
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
public data class ReflowableWebSettings(
    val backgroundColor: Color,
    val columnCount: Int?,
    val fontFamily: FontFamily?,
    val fontSize: Double,
    val fontWeight: Double?,
    val hyphens: Boolean?,
    val imageFilter: ImageFilter?,
    val language: Language?,
    val letterSpacing: Double?,
    val ligatures: Boolean?,
    val lineHeight: Double?,
    val linkColor: Color,
    val maximalLineLength: Double?,
    val minimalLineLength: Double?,
    val minMargins: Double,
    val optimalLineLength: Double,
    val overridePublisherColors: Boolean,
    val paragraphIndent: Double?,
    val paragraphSpacing: Double?,
    val readingProgression: ReadingProgression,
    val scroll: Boolean,
    val textAlign: TextAlign?,
    val textColor: Color,
    val textNormalization: Boolean,
    val verticalText: Boolean,
    val visitedColor: Color,
    val wordSpacing: Double?,
) : Settings
