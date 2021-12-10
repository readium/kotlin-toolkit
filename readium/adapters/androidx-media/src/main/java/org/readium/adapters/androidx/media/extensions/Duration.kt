/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.androidx.media.extensions

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
// FIXME: To remove after bumping to Kotlin 1.6
@Suppress("DEPRECATION")
internal fun List<Duration>.sum(): Duration =
    fold(Duration.seconds(0)) { a, b -> a + b }

@JvmName("sumNullable")
@ExperimentalTime
// FIXME: To remove after bumping to Kotlin 1.6
@Suppress("DEPRECATION")
internal fun List<Duration?>.sum(): Duration =
    fold(Duration.seconds(0)) { a, b -> a + (b ?: Duration.seconds(0)) }
