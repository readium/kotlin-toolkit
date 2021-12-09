/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.extensions

import android.text.format.DateUtils
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
internal fun List<Duration>.sum(): Duration =
    fold(0.seconds) { a, b -> a + b }

@JvmName("sumNullable")
@ExperimentalTime
internal fun List<Duration?>.sum(): Duration =
    fold(0.seconds) { a, b -> a + (b ?: 0.seconds) }

@ExperimentalTime
internal fun Duration.formatElapsedTime(): String =
    DateUtils.formatElapsedTime(toLong(DurationUnit.SECONDS))
