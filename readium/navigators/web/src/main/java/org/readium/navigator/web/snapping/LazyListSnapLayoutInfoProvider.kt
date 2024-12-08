/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.readium.navigator.web.snapping

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

@JvmInline
internal value class FinalSnappingItem internal constructor(
    @Suppress("unused") private val value: Int,
) {
    companion object {

        val ClosestItem: FinalSnappingItem = FinalSnappingItem(0)

        val NextItem: FinalSnappingItem = FinalSnappingItem(1)

        val PreviousItem: FinalSnappingItem = FinalSnappingItem(2)
    }
}

internal fun Density.calculateFinalSnappingItem(velocity: Float): FinalSnappingItem {
    return if (velocity.absoluteValue < MinFlingVelocityDp.toPx()) {
        FinalSnappingItem.ClosestItem
    } else {
        if (velocity > 0) FinalSnappingItem.NextItem else FinalSnappingItem.PreviousItem
    }
}

internal val MinFlingVelocityDp = 400.dp
