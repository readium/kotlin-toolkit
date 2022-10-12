/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
interface FixedLayoutSettings {

    val readingProgression: EnumSetting<ReadingProgression>?

    val scroll: Setting<Boolean>?

    val scrollAxis: EnumSetting<Axis>?

    val spread: EnumSetting<Spread>?

    val offset: Setting<Boolean>?

    val language: Setting<Language?>?

    val fit: EnumSetting<Fit>?

    val pageSpacing: RangeSetting<Double>?
}

@ExperimentalReadiumApi
interface FixedLayoutPreferencesEditor {

    fun setReadingProgression(readingProgression: ReadingProgression?) {}

    fun setScroll(scroll: Boolean) {}

    fun setScrollAxis(axis: Axis) {}

    fun setSpread(spread: Spread) {}

    fun setOffset(offset: Boolean) {}

    fun setLanguage(language: Language?) {}

    fun setFit(fit: Fit) {}

    fun increasePageSpacing() {}

    fun decreasePageSpacing() {}
}