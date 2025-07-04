/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.webapi

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.MapCompanion

/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@Serializable
internal data class JsonOffset(
    val x: Float,
    val y: Float,
) {
    fun toDpOffset(): DpOffset =
        DpOffset(x = x.dp, y = y.dp)
}

@Serializable
internal data class JsonRect(
    val top: Double,
    val right: Double,
    val bottom: Double,
    val left: Double,
    val width: Double,
    val height: Double,
) {
    fun toDpRect(): DpRect =
        DpRect(
            top = top.dp,
            right = right.dp,
            bottom = bottom.dp,
            left = left.dp
        )
}

public enum class Iframe(public val value: String) {
    Left("left"),
    Right("right"),
    ;

    override fun toString(): String {
        return value
    }

    @OptIn(InternalReadiumApi::class)
    public companion object : MapCompanion<String, Iframe>(
        entries.toTypedArray(),
        Iframe::value
    )
}
