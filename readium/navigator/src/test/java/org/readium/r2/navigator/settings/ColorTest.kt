/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.settings

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import org.readium.r2.shared.ExperimentalReadiumApi
import kotlin.test.assertEquals
import android.graphics.Color as AndroidColor

class ColorTest {

    val coder = Color.Coder(namedColors = mapOf(
        "red" to AndroidColor.RED,
        "green" to AndroidColor.GREEN,
        "blue" to AndroidColor.BLUE,
    ))

    @Test
    fun `Encode colors`() {
        assertEquals(JsonPrimitive("red"), coder.encode(Color(AndroidColor.RED)))
        assertEquals(JsonPrimitive("green"), coder.encode(Color(AndroidColor.GREEN)))
        assertEquals(JsonPrimitive("blue"), coder.encode(Color(AndroidColor.BLUE)))

        assertEquals(JsonPrimitive(AndroidColor.YELLOW), coder.encode(Color(AndroidColor.YELLOW)))
        assertEquals(JsonPrimitive(AndroidColor.MAGENTA), coder.encode(Color(AndroidColor.MAGENTA)))
    }

    @Test
    fun `Decode colors`() {
        assertEquals(Color(AndroidColor.RED), coder.decode(JsonPrimitive("red")))
        assertEquals(Color(AndroidColor.GREEN), coder.decode(JsonPrimitive("green")))
        assertEquals(Color(AndroidColor.BLUE), coder.decode(JsonPrimitive("blue")))

        assertEquals(Color(AndroidColor.YELLOW), coder.decode(JsonPrimitive(AndroidColor.YELLOW)))
        assertEquals(Color(AndroidColor.MAGENTA), coder.decode(JsonPrimitive(AndroidColor.MAGENTA)))
    }

    @Test
    fun `Decode unknown named color`() {
        assertEquals(Color.AUTO, coder.decode(JsonPrimitive("purple")))
    }

    @Test
    fun `Auto color`() {
        assertEquals(Color.AUTO, coder.decode(JsonNull))
        assertEquals(JsonNull, coder.encode(Color.AUTO))
    }
}