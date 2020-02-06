/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.junit.Test

class EpubTest {
    private val propertiesPub = parsePackageDocument("package/links-properties.opf")

    @Test
    fun `nav item is identified`() {

    }
}