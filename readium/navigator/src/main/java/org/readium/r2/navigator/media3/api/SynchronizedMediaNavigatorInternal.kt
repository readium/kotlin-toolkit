/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.api

import kotlinx.coroutines.flow.StateFlow

interface SynchronizedMediaNavigatorInternal<P : MediaNavigatorInternal.Position,
    R : MediaNavigatorInternal.RelaxedPosition, E : MediaNavigatorInternal.Error> :
    MediaNavigatorInternal<P, R, E> {

    data class Utterance<P : MediaNavigatorInternal.Position>(
        val text: String,
        val position: P,
        val range: IntRange?
    )

    val utterance: StateFlow<Utterance<P>>
}
