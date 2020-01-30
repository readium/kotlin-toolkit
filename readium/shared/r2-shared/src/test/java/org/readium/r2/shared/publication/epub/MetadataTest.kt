/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.epub

import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Metadata

class MetadataTest {

    @Test fun `get Metadata {layout} when available`() {
        assertEquals(
            EpubLayout.FIXED,
            Metadata(
                localizedTitle = LocalizedString("Title"),
                otherMetadata = mapOf("layout" to "fixed")
            ).layout
        )
    }

    @Test fun `get Metadata {layout} when missing`() {
        assertNull(Metadata(
            localizedTitle = LocalizedString("Title")
        ).layout)
    }

}