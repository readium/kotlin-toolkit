/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NormalizeTest {
    @Test
    fun `Anchors are accepted as href`() {
        assertThat(normalize("OEBPS/xhtml/nav.xhtml", "#toc")).isEqualTo("/OEBPS/xhtml/nav.xhtml#toc")
    }

    @Test
    fun `Directories are accepted as base`() {
        assertThat(normalize("OEBPS/xhtml/", "nav.xhtml")).isEqualTo("/OEBPS/xhtml/nav.xhtml")
    }

    @Test
    fun `href is returned unchanged if it is an absolute path`() {
        assertThat(normalize("OEBPS/content.opf", "/OEBPS/xhtml/index.xhtml")).isEqualTo("/OEBPS/xhtml/index.xhtml")
    }

    @Test
    fun `href is returned unchanged if it is an absolute URI`() {
        assertThat(normalize("OEBPS/content.opf", "http://example.org/index.xhtml"))
    }

    @Test
    fun `Result is percent-decoded`() {
        val base = "OEBPS/xhtml/%E4%B8%8A%E6%B5%B7%2B%E4%B8%AD%E5%9C%8B/base.xhtml"
        val href = "%E4%B8%8A%E6%B5%B7%2B%E4%B8%AD%E5%9C%8B.xhtml"
        assertThat(normalize(base, href)).isEqualTo("/OEBPS/xhtml/上海+中國/上海+中國.xhtml")
        assertThat(normalize("OEBPS/xhtml%20files/nav.xhtml", "chapter1.xhtml")).isEqualTo("/OEBPS/xhtml files/chapter1.xhtml")

    }
}