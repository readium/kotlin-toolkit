/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.preferences

import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
interface Preference<T> {

    var value: T?

    val effectiveValue: T

    val isActive: Boolean
}

@ExperimentalReadiumApi
interface EnumPreference<T> : Preference<T> {

    val supportedValues: List<T>
}

@ExperimentalReadiumApi
interface RangePreference<T: Comparable<T>> : Preference<T> {

    val supportedRange: ClosedRange<T>

    fun increment()

    fun decrement()

    fun formatValue(value: T): String
}

@ExperimentalReadiumApi
interface SwitchPreference : Preference<Boolean> {

    fun toggle()
}
