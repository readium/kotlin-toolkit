package org.readium.r2.shared.publication.archive

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.publication.Properties
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PropertiesTest {

    @Test
    fun `get no archive`() {
        assertNull(Properties().archive)
    }

    @Test
    fun `get full archive`() {
        assertEquals(
            ArchiveProperties(entryLength = 8273, isEntryCompressed = true),
            Properties(
                mapOf(
                    "archive" to mapOf(
                        "entryLength" to 8273,
                        "isEntryCompressed" to true
                    )
                )
            ).archive
        )
    }

    @Test
    fun `get invalid archive`() {
        assertNull(
            Properties(
                mapOf(
                    "archive" to mapOf(
                        "foo" to "bar"
                    )
                )
            ).archive
        )
    }

    @Test
    fun `get incomplete archive`() {
        assertNull(
            Properties(
                mapOf(
                    "archive" to mapOf(
                        "isEntryCompressed" to true
                    )
                )
            ).archive
        )

        assertNull(
            Properties(
                mapOf(
                    "archive" to mapOf(
                        "entryLength" to 8273
                    )
                )
            ).archive
        )
    }

    @Test
    fun `get archive JSON`() {
        assertJSONEquals(
            JSONObject(
                mapOf(
                    "entryLength" to 8273L,
                    "isEntryCompressed" to true
                )
            ),
            ArchiveProperties(entryLength = 8273, isEntryCompressed = true).toJSON()
        )
    }
}
