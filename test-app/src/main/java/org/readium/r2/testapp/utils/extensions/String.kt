/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils.extensions

import java.text.NumberFormat

/**
 * Formats a [percentage] double into a localized percentage string, e.g. 47%.
 */
fun String.Companion.formatPercentage(percentage: Double): String =
    NumberFormat.getPercentInstance().run {
        maximumFractionDigits = 0
        format(percentage)
    }
