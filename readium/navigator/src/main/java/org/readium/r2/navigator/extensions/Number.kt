/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.extensions

import java.text.NumberFormat
import org.readium.r2.shared.InternalReadiumApi

@InternalReadiumApi
fun Number.format(maximumFractionDigits: Int, percent: Boolean = false): String {
    val format = if (percent) NumberFormat.getPercentInstance() else NumberFormat.getNumberInstance()
    format.maximumFractionDigits = maximumFractionDigits
    return format.format(this)
}
