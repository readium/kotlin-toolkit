/*
 * Module: r2-opds-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.opds

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
