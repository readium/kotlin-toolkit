/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)

package org.readium.r2.streamer.parser.epub

import java.io.File
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.guided.GuidedNavigationAudioRef
import org.readium.r2.shared.guided.GuidedNavigationDocument
import org.readium.r2.shared.guided.GuidedNavigationObject
import org.readium.r2.shared.guided.GuidedNavigationRef
import org.readium.r2.shared.guided.GuidedNavigationTextRef
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.file.DirectoryContainer
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.xml.XmlParser
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmilBasedMediaOverlaysServiceTest {

    private val smilDir = requireNotNull(
        SmilBasedMediaOverlaysServiceTest::class.java
            .getResource("smil")
            ?.path
            ?.let { File(it) }
    )

    private val smilUrls = listOf(
        RelativeUrl("chapter_001_overlay.smil")!!,
        RelativeUrl("chapter_002_overlay.smil")!!
    )

    private val container: Container<Resource> = DirectoryContainer(
        root = smilDir,
        entries = smilUrls.toSet()
    )

    @Test
    fun `smil files are chained`() = runBlocking {
        val service = SmilBasedMediaOverlaysService(smilUrls, container)
        val iterator = service.iterator()
        val docs = mutableListOf<GuidedNavigationDocument>()
        while (iterator.hasNext()) {
            val doc = assertNotNull(iterator.next().getOrNull())
            docs.add(doc)
        }
        assert(docs.size == 2)
    }
}

@RunWith(RobolectricTestRunner::class)
class SmilParserTest {

    private val chapter1File: File = requireNotNull(
        SmilParserTest::class.java
            .getResource("smil/chapter_001_overlay.smil")
            ?.path
            ?.let { File(it) }
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T : GuidedNavigationRef> firstRefOfClass(
        nodes: List<GuidedNavigationObject>,
        klass: KClass<T>,
    ): T? {
        for (node in nodes) {
            node.refs
                .firstOrNull { klass.isInstance(it) }
                ?.let { return it as T }

            return firstRefOfClass(node.children, klass)
        }

        return null
    }

    private fun parseSmilDoc(): GuidedNavigationDocument {
        val root = chapter1File
            .inputStream()
            .use { XmlParser().parse(it) }

        val doc =
            SmilParser.parse(root, Url("OPS/chapter_001_overlay.smil")!!)

        return assertNotNull(doc)
    }

    @Test
    fun `all leaves are parsed`() {
        fun assertSize(nodes: List<GuidedNavigationObject>) {
            assert(nodes.size == 1 || nodes.size == 27)

            if (nodes.size == 27) {
                return
            }
            for (node in nodes) {
                assertSize(node.children)
            }
        }

        val guidedNavDoc = parseSmilDoc()
        assertSize(guidedNavDoc.guided)
    }

    @Test
    fun `generated href are relative to SMIL`() {
        val guidedNavDoc = parseSmilDoc()
        val firstTextRef = assertNotNull(firstRefOfClass(guidedNavDoc.guided, GuidedNavigationTextRef::class))
        assertEquals(Url("OPS/chapter_001.xhtml#c01h01")!!, firstTextRef.url)

        val firstAudioRef = assertNotNull(firstRefOfClass(guidedNavDoc.guided, GuidedNavigationAudioRef::class))
        assertEquals(Url("OPS/audio/mobydick_001_002_melville.mp4")!!, firstAudioRef.url.removeFragment())
    }

    @Test
    fun `audio clips are correct`() {
        val guidedNavDoc = parseSmilDoc()
        val firstAudioRef = assertNotNull(firstRefOfClass(guidedNavDoc.guided, GuidedNavigationAudioRef::class))
        assertEquals("t=24.5,29.268", firstAudioRef.url.fragment)
    }
}
