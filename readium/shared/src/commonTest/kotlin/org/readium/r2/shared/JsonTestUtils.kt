/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared

import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Asserts that two [JsonObject] are equal.
 */
fun assertJSONEquals(expected: JsonObject, actual: JsonObject) {
    assertEquals(expected, actual)
}

/**
 * Asserts that two [JsonArray] are equal.
 */
fun assertJSONEquals(expected: JsonArray, actual: JsonArray) {
    assertEquals(expected, actual)
}
