/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer

import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Asserts that [actual] contains exactly the elements of [expected], in any order.
 *
 * This is a multiset comparison: duplicates are significant, only the order is not.
 */
fun <T> assertContainsExactlyInAnyOrder(expected: Collection<T>, actual: Collection<T>?) {
    val actualElements = assertNotNull(actual)
    assertEquals(
        expected.groupingBy { it }.eachCount(),
        actualElements.groupingBy { it }.eachCount()
    )
}
