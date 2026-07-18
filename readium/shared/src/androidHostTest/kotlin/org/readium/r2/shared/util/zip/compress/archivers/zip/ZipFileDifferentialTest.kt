/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.compress.archivers.zip

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.fixturesFileSystem
import org.readium.r2.shared.util.zip.FileChannelAdapter
import org.readium.r2.shared.util.zip.compress.utils.IOUtils

/**
 * Differential test for phase 05b: for each fixture, asserts that the ported common [ZipFile]
 * returns byte-identical entry contents and metadata to the legacy vendored implementation
 * (`:readium:readium-shared-zip-legacy`).
 *
 * The zip64 and unicode fixtures are deliberately excluded: the vendored legacy
 * `ExtraFieldUtils` registry was empty, so the legacy `ZipFile` cannot read zip64 archives nor
 * honor InfoZIP Unicode extra fields. The ported implementation restores both behaviors (see
 * `docs/kmp/migration-notes.md`); they are covered by the common `ZipFileTest` instead.
 */
class ZipFileDifferentialTest {

    private val fixtures = Fixtures("zip")

    private val fixtureNames = listOf("epub.epub", "basic.zip", "datadescriptor.zip")

    @Test
    fun portedZipFileMatchesLegacyImplementation() = runTest {
        for (fixture in fixtureNames) {
            compareFixture(fixture)
        }
    }

    private suspend fun compareFixture(fixture: String) {
        val file = fixtures.path(fixture).toFile()

        val legacyChannel =
            org.readium.r2.shared.util.zip.legacyjvm.FileChannelAdapter(file, "r")
        val legacyZip =
            org.readium.r2.shared.util.zip.legacycompress.archivers.zip.ZipFile(legacyChannel)

        val channel = FileChannelAdapter(fixturesFileSystem.openReadOnly(fixtures.path(fixture)))
        val zip = ZipFile(channel)

        try {
            val legacyEntries = legacyZip.entries.toList()
            val entries = zip.entries

            assertEquals(
                legacyEntries.map { it.name },
                entries.map { it.name },
                "Entry names of $fixture"
            )

            for (legacyEntry in legacyEntries) {
                val entry = assertNotNull(zip.getEntry(legacyEntry.name), legacyEntry.name)
                val context = "Entry ${legacyEntry.name} of $fixture"
                assertEquals(legacyEntry.size, entry.size, "$context: size")
                assertEquals(legacyEntry.compressedSize, entry.compressedSize, "$context: csize")
                assertEquals(legacyEntry.method, entry.method, "$context: method")
                assertEquals(legacyEntry.crc, entry.crc, "$context: crc")
                assertEquals(legacyEntry.isDirectory, entry.isDirectory, "$context: isDirectory")

                if (legacyEntry.isDirectory) {
                    continue
                }

                val legacyContent = legacyZip.getInputStream(legacyEntry).use { it.readBytes() }
                val stream = assertNotNull(zip.getInputStream(entry), context)
                val content = try {
                    IOUtils.toByteArray(stream)
                } finally {
                    stream.close()
                }
                assertContentEquals(legacyContent, content, "$context: content")
            }
        } finally {
            legacyZip.close()
            zip.close()
        }
    }
}
