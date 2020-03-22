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

class ResourceTransformerTest {

    @Test
    fun `Simple media type matching works`() {
        val filter = object : ResourceTransformer {
            override val priority = 0
            override val accepts = listOf("audio/opus", "text/css")
            override fun filter(resource: ResourceImpl, link: Link): ResourceImpl = resource
        }

        val acceptedResource = object : ResourceImpl(Link(href = "/stylesheet.css", type = "text/css")) {
            override fun stream(): InputStream? = null
        }

        assertThat(filter.accepts(acceptedResource)).isTrue()

        val rejectedResource = object : ResourceImpl(Link(href = "/chap1.xhtml", type = "application/xhtml+xml")) {
            override fun stream(): InputStream? = null
        }

        assertThat(filter.accepts(rejectedResource)).isFalse()
    }

    @Test
    fun `Wildcards are supported in media types`() {
        val filter = object : ResourceTransformer {
            override val priority = 0
            override val accepts = listOf("audio/*")
            override fun filter(resource: ResourceImpl, link: Link): ResourceImpl = resource
        }

        val resource = object : ResourceImpl(Link(href = "/chap1.opus", type = "audio/opus")) {
            override fun stream(): InputStream? = null
        }
        assertThat(filter.accepts(resource)).isTrue()
    }

    @Test
    fun `A null media type matches no filter`() {
        val filter = object : ResourceTransformer {
            override val priority = 0
            override val accepts = listOf("audio/*")
            override fun filter(resource: ResourceImpl, link: Link): ResourceImpl = resource
        }

        val resource = object : ResourceImpl(Link(href = "/chap1.opus")) {
            override fun stream(): InputStream? = null
        }
        assertThat(filter.accepts(resource)).isFalse()
    }

    @Test
    fun `An empty filter list matches no media type `() {
        val filter = object : ResourceTransformer {
            override val priority: Int = 0
            override val accepts: Collection<String> = emptyList()
            override fun filter(resource: ResourceImpl, link: Link): ResourceImpl = resource
        }

        val resource1 = object : ResourceImpl(Link(href = "/chap1.opus", type = null)) {
            override fun stream(): InputStream? = null
        }
        assertThat(filter.accepts(resource1)).isFalse()

        val resource2 = object : ResourceImpl(Link(href = "/chap2.opus", type = "audio/opus")) {
            override fun stream(): InputStream? = null
        }
        assertThat(filter.accepts(resource2)).isFalse()
    }
}