/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AccessibilityDisplayStringTest {

    @Test
    fun `get localized string`() {
        val context = RuntimeEnvironment.getApplication()
        val string = AccessibilityDisplayString.WAYS_OF_READING_NONVISUAL_READING_READABLE

        assertEquals(
            "Readable in read aloud or dynamic braille",
            string.localizedString(context, descriptive = false)
        )

        assertEquals(
            "All content can be read as read aloud speech or dynamic braille",
            string.localizedString(context, descriptive = true)
        )
    }
}
