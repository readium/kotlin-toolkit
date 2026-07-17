package org.readium.r2.shared.util.resource

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.util.archive.ArchiveProperties
import org.readium.r2.shared.util.archive.archive
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PropertiesTest {

    @Test
    fun `get no archive`() {
        assertNull(Resource.Properties().archive)
    }

    @Test
    fun `get full archive`() {
        assertEquals(
            ArchiveProperties(entryLength = 8273, isEntryCompressed = true),
            Resource.Properties(
                mapOf(
                    "https://readium.org/webpub-manifest/properties#archive" to mapOf(
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
            Resource.Properties(
                mapOf(
                    "https://readium.org/webpub-manifest/properties#archive" to mapOf(
                        "foo" to "bar"
                    )
                )
            ).archive
        )
    }

    @Test
    fun `get incomplete archive`() {
        assertNull(
            Resource.Properties(
                mapOf(
                    "https://readium.org/webpub-manifest/properties#archive" to mapOf(
                        "isEntryCompressed" to true
                    )
                )
            ).archive
        )

        assertNull(
            Resource.Properties(
                mapOf(
                    "https://readium.org/webpub-manifest/properties#archive" to mapOf(
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
