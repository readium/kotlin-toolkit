/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Language

@OptIn(ExperimentalReadiumApi::class)
interface ReflowaleSettings {

    val backgroundColor: Setting<Color>?

    val columnCount: EnumSetting<ColumnCount>?

    val fontFamily: EnumSetting<FontFamily?>?

    val fontSize: PercentSetting?

    val hyphens: Setting<Boolean>?

    val imageFilter: EnumSetting<ImageFilter>?

    val language: Setting<Language?>?

    val letterSpacing: PercentSetting?

    val ligatures: Setting<Boolean>?

    val lineHeight: RangeSetting<Double>?

    val pageMargins: RangeSetting<Double>?

    val paragraphIndent: PercentSetting?

    val paragraphSpacing: PercentSetting?

    val publisherStyles: Setting<Boolean>?

    val readingProgression: EnumSetting<ReadingProgression>?

    val scroll: Setting<Boolean>?

    val textAlign: EnumSetting<TextAlign>?

    val textColor: Setting<Color>?

    val textNormalization: EnumSetting<TextNormalization>?

    val theme: EnumSetting<Theme>?

    val typeScale: RangeSetting<Double>?

    val verticalText: Setting<Boolean>?

    val wordSpacing: PercentSetting?
}
