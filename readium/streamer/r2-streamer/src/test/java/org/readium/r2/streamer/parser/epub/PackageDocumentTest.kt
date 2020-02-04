/* Module: r2-streamer-kotlin
* Developers: Quentin Gliosca
*
* Copyright (c) 2018. Readium Foundation. All rights reserved.
* Use of this source code is governed by a BSD-style license which is detailed in the
* LICENSE file present in the project repository where this source code is maintained.
*/

package org.readium.r2.streamer.parser.epub

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.shared.publication.Publication

fun parsePackageDocument(path: String) : Publication {
    val pub = PackageDocumentParser::class.java.getResourceAsStream(path)
            ?.let { XmlParser().parse(it) }
            ?.let { PackageDocumentParser.parse(it, "OEBPS/content.opf") }
            ?.let { Epub(it) }
            ?.toPublication()
    check (pub != null)
    return pub
}

class ReadingProgressionTest {
    @Test
    fun `No page progression direction is mapped to default`() {
        assertThat(parsePackageDocument("package/progression-none.opf").metadata.readingProgression)
                .isEqualTo(ReadingProgression.AUTO)
    }

    @Test
    fun `Default page progression direction is rightly parsed`() {
        assertThat(parsePackageDocument("package/progression-default.opf").metadata.readingProgression)
                .isEqualTo(ReadingProgression.AUTO)
    }

    @Test
    fun `Ltr page progression direction is rightly parsed`() {
        assertThat(parsePackageDocument("package/progression-ltr.opf").metadata.readingProgression)
                .isEqualTo(ReadingProgression.LTR)
    }

    @Test
    fun `Rtl page progression direction is rightly parsed`() {
        assertThat(parsePackageDocument("package/progression-rtl.opf").metadata.readingProgression)
                .isEqualTo(ReadingProgression.RTL)
    }
}