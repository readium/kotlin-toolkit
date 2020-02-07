/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.Serializable

// FIXME: To be removed once we migrate from Activity to Fragment, then serializing the Publication won't be necessary anymore – see https://github.com/readium/r2-navigator-kotlin/issues/115
// Commented because running tests with Roboletric is too slow
//@RunWith(AndroidJUnit4::class)
//class PublicationParcelizationTest {
//
//    private fun createPublication(
//        positionListFactory: PositionListFactory = { emptyList() }
//    ) = Publication(
//        metadata = Metadata(
//            localizedTitle = LocalizedString("Title")
//        ),
//        positionListFactory = positionListFactory as Serializable
//    )
//
//    @Test fun `parcelization of the {positionListFactory}`() {
//        var publication = createPublication(
//            positionListFactory = { listOf(Locator(href="locator", type = "")) }
//        )
//        val intent = Intent()
//        intent.putExtra("publication", publication)
//        publication = intent.getParcelableExtra("publication") as Publication
//
//        assertEquals(
//            listOf(Locator(href = "locator", type = "")),
//            publication.positionList
//        )
//    }
//
//}
