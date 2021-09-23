/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.presentation

import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Metadata

class MetadataTest {

    @Test fun `get Metadata {presentation} when available`() {
        assertEquals(
            Presentation(continuous = false, orientation = Presentation.Orientation.LANDSCAPE),
            Metadata(
                localizedTitle = LocalizedString("Title"),
                otherMetadata = mapOf("presentation" to mapOf(
                    "continuous" to false,
                    "orientation" to "landscape"
                ))
            ).presentation
        )
    }

    @Test fun `get Metadata {presentation} when missing`() {
        assertEquals(
            Presentation(),
            Metadata(localizedTitle = LocalizedString("Title")).presentation
        )
    }

}
