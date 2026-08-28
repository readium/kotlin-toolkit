/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.license.container

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull

class ZipUtilTest {

    private val zipPath: String =
        ZipUtilTest::class.java.getResource("futuristic_tales.cbz")!!.path

    private val zipFile: ZipFile =
        ZipFile(zipPath)

    private val entryNames: List<String> = listOf(
        "Cory Doctorow's Futuristic Tales of the Here and Now/a-fc.jpg",
        "Cory Doctorow's Futuristic Tales of the Here and Now/x-002.jpg",
        "Cory Doctorow's Futuristic Tales of the Here and Now/x-003.jpg",
        "Cory Doctorow's Futuristic Tales of the Here and Now/x-004.jpg"
    )

    private val aFcPath: String =
        ZipUtilTest::class.java.getResource("a-fc.jpg")!!.path

    private fun ZipFile.readEntry(name: String): ByteArray? {
        val entry = getEntry(name) ?: return null
        val stream = getInputStream(entry)
        return stream.readBytes()
    }

    private fun ZipInputStream.readEntries(): Map<String, ByteArray> {
        val modifiedEntries = mutableMapOf<String, ByteArray>()

        do {
            val entry = nextEntry
            if (entry != null) {
                modifiedEntries[entry.name] = readBytes()
            }
        } while (entry != null)

        return modifiedEntries
    }

    @Test
    fun addEntryWorks() {
        val entryToAdd = "Cory Doctorow's Futuristic Tales of the Here and Now/x-005.jpg"

        val modifiedZip = run {
            val outStream = ByteArrayOutputStream()
            zipFile.addOrReplaceEntry(
                entryToAdd,
                FileInputStream(aFcPath),
                outStream
            )
            outStream.toByteArray()
        }

        val modifiedZipStream = ZipInputStream(ByteArrayInputStream(modifiedZip))
        val modifiedEntries = modifiedZipStream.readEntries()

        for (name in entryNames) {
            val modifiedEntry = assertNotNull(modifiedEntries[name])
            val expected = zipFile.readEntry(name)
            assertContentEquals(expected, modifiedEntry)
        }

        assert(entryToAdd in modifiedEntries.keys)
        assertContentEquals(
            zipFile.readEntry(entryNames[0]),
            modifiedEntries[entryToAdd]
        )
    }

    @Test
    fun replaceEntryWorks() {
        val entryToReplace = "Cory Doctorow's Futuristic Tales of the Here and Now/x-004.jpg"

        val modifiedZip = run {
            val outStream = ByteArrayOutputStream()
            zipFile.addOrReplaceEntry(
                entryToReplace,
                FileInputStream(aFcPath),
                outStream
            )
            outStream.toByteArray()
        }

        val modifiedZipStream = ZipInputStream(ByteArrayInputStream(modifiedZip))
        val modifiedEntries = modifiedZipStream.readEntries()

        for (name in entryNames) {
            val expected = if (name == entryToReplace) {
                zipFile.readEntry(entryNames[0])
            } else {
                zipFile.readEntry(name)
            }

            val modifiedEntry = assertNotNull(modifiedEntries[name])
            assertContentEquals(expected, modifiedEntry)
        }
    }
}
