/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import android.os.Parcel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner

/**
 * Verifies that the Readium-owned [Parcelize] annotation still triggers the Parcelize compiler
 * plugin on Android through the `additionalAnnotation` plugin option.
 */
@RunWith(RobolectricTestRunner::class)
class ParcelizeTest {

    @Test
    fun `a class annotated with the Readium Parcelize round-trips through a Parcel`() {
        val locator = Locator(
            href = Url("http://locator")!!,
            mediaType = MediaType.HTML,
            title = "Title",
            locations = Locator.Locations(progression = 0.5),
            text = Locator.Text(highlight = "highlight")
        )

        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(locator, 0)
            parcel.setDataPosition(0)
            @Suppress("Deprecation")
            val unparceled = parcel.readParcelable<Locator>(Locator::class.java.classLoader)

            assertEquals(locator, unparceled)
        } finally {
            parcel.recycle()
        }
    }
}
