/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.extensions

import android.graphics.RectF
import org.json.JSONObject

/**
 * Parses a [RectF] from its JSON representation.
 */
fun JSONObject.optRectF(name: String): RectF? =
    optJSONObject(name)?.let { json ->
        val left = json.optDouble("left").toFloat()
        val top = json.optDouble("top").toFloat()
        val right = json.optDouble("right").toFloat()
        val bottom = json.optDouble("bottom").toFloat()
        RectF(left, top, right, bottom)
    }
