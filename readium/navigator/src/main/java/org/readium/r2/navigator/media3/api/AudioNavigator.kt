/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.api

import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * A [MediaNavigator] playing audio files.
 */
@ExperimentalReadiumApi
interface AudioNavigator<P : AudioNavigator.Position> : MediaNavigator<P> {

    interface Position : MediaNavigator.Position

    interface Resource : MediaNavigator.Resource {

        val position: Duration

        val buffered: Duration?
    }

    interface ReadingOrder : MediaNavigator.ReadingOrder {

        val duration: Duration?

        override val items: List<Item>

        interface Item : MediaNavigator.ReadingOrder.Item {

            val duration: Duration?
        }
    }

    override val position: StateFlow<P>

    override val readingOrder: ReadingOrder

    override val resource: StateFlow<Resource>
}
