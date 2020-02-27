/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.readium.r2.shared.publication.Link
import java.io.InputStream

class ContentFilterTest {

    @Test
    fun `Simple media type matching works`() {
        val filter = object : ContentFilter {
            override val priority = 0
            override val accepts = listOf("audio/opus", "text/css")
            override fun filter(input: InputStream, link: Link): InputStream = input
        }

        val acceptedLink = Link(href = "/stylesheet.css", type = "text/css")
        assertThat(filter.acceptsLink(acceptedLink)).isTrue()

        val rejectedLink = Link(href = "/chap1.xhtml", type = "application/xhtml+xml")
        assertThat(filter.acceptsLink(rejectedLink)).isFalse()
    }

    @Test
    fun `Wildcards are supported in media types`() {
        val filter = object : ContentFilter {
            override val priority = 0
            override val accepts = listOf("audio/*")
            override fun filter(input: InputStream, link: Link): InputStream = input
        }

        val link = Link(href = "/chap1.opus", type = "audio/opus")
        assertThat(filter.acceptsLink(link)).isTrue()
    }

    @Test
    fun `A null media type matches no filter`() {
        val filter = object : ContentFilter {
            override val priority = 0
            override val accepts = listOf("audio/*")
            override fun filter(input: InputStream, link: Link): InputStream = input
        }

        val link = Link(href = "/chap1.opus")
        assertThat(filter.acceptsLink(link)).isFalse()
    }

    @Test
    fun `An empty filter list matches no media type `() {
        val filter = object : ContentFilter {
            override val priority: Int = 0
            override val accepts: Collection<String> = emptyList()
            override fun filter(input: InputStream, link: Link): InputStream = input
        }

        val link1 = Link(href = "/chap1.opus", type = null)
        assertThat(filter.acceptsLink(link1)).isFalse()
        val link2 = Link(href = "/chap2.opus", type = "audio/opus")
        assertThat(filter.acceptsLink(link2)).isFalse()
    }
}