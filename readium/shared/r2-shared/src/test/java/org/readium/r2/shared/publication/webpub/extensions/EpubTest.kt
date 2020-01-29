package org.readium.r2.shared.publication.webpub.extensions

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.publication.webpub.LocalizedString
import org.readium.r2.shared.publication.webpub.PublicationCollection
import org.readium.r2.shared.publication.webpub.ReadingProgression
import org.readium.r2.shared.publication.webpub.WebPublication
import org.readium.r2.shared.publication.webpub.link.Link
import org.readium.r2.shared.publication.webpub.link.Properties
import org.readium.r2.shared.publication.webpub.metadata.Metadata

class EpubTest {

    // EpubLayout

    @Test fun `parse layout`() {
        assertEquals(EpubLayout.FIXED, EpubLayout.from("fixed"))
        assertEquals(EpubLayout.REFLOWABLE, EpubLayout.from("reflowable"))
        assertNull(EpubLayout.from("foobar"))
        assertNull(EpubLayout.from(null))
    }

    @Test fun `parse layout from EPUB rendition property`() {
        assertEquals(EpubLayout.REFLOWABLE, EpubLayout.fromEpub("reflowable"))
        assertEquals(EpubLayout.FIXED, EpubLayout.fromEpub("pre-paginated"))
        assertEquals(EpubLayout.REFLOWABLE, EpubLayout.fromEpub("foobar"))
        assertEquals(EpubLayout.FIXED, EpubLayout.fromEpub("foobar", fallback = EpubLayout.FIXED))
    }

    @Test fun `get layout value`() {
        assertEquals("fixed", EpubLayout.FIXED.value)
        assertEquals("reflowable", EpubLayout.REFLOWABLE.value)
    }


    // EpubEncryption

    @Test fun `parse minimal JSON`() {
        assertEquals(
            EpubEncryption(algorithm = "http://algo"),
            EpubEncryption.fromJSON(JSONObject("{'algorithm': 'http://algo'}"))
        )
    }

    @Test fun `parse full JSON`() {
        assertEquals(
            EpubEncryption(
                algorithm = "http://algo",
                compression = "gzip",
                originalLength = 42099,
                profile = "http://profile",
                scheme = "http://scheme"
            ),
            EpubEncryption.fromJSON(JSONObject("""{
                "algorithm": "http://algo",
                "compression": "gzip",
                "originalLength": 42099,
                "profile": "http://profile",
                "scheme": "http://scheme"
            }"""))
        )
    }

    @Test fun `parse null JSON`() {
        assertNull(EpubEncryption.fromJSON(null))
    }

    @Test fun `parse JSON requires {algorithm}`() {
        assertNull(EpubEncryption.fromJSON(JSONObject("{'compression': 'gzip'}")))
    }

    @Test fun `get minimal JSON`() {
        assertJSONEquals(
            JSONObject("{'algorithm': 'http://algo'}"),
            EpubEncryption(algorithm = "http://algo").toJSON()
        )
    }

    @Test fun `get full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "algorithm": "http://algo",
                "compression": "gzip",
                "originalLength": 42099,
                "profile": "http://profile",
                "scheme": "http://scheme"
            }"""),
            EpubEncryption(
                algorithm = "http://algo",
                compression = "gzip",
                originalLength = 42099,
                profile = "http://profile",
                scheme = "http://scheme"
            ).toJSON()
        )
    }


    // EPUB extensions for [WebPublication].

    private fun createWebPublication(
        otherCollections: List<PublicationCollection> = emptyList()
    ) = WebPublication(
        metadata = Metadata(localizedTitle = LocalizedString("Title")),
        otherCollections = otherCollections
    )

    @Test fun `get {pageList}`() {
        val links = listOf(Link(href = "/page1.html"))
        assertEquals(
            links,
            createWebPublication(otherCollections = listOf(
                PublicationCollection(role = "page-list", links = links)
            )).pageList
        )
    }

    @Test fun `get {pageList} when missing`() {
        assertEquals(0, createWebPublication().pageList.size)
    }

    @Test fun `get {landmarks}`() {
        val links = listOf(Link(href = "/landmark.html"))
        assertEquals(
            links,
            createWebPublication(otherCollections = listOf(
                PublicationCollection(role = "landmarks", links = links)
            )).landmarks
        )
    }

    @Test fun `get {landmarks} when missing`() {
        assertEquals(0, createWebPublication().landmarks.size)
    }

    @Test fun `get {listOfAudioClips}`() {
        val links = listOf(Link(href = "/audio.mp3"))
        assertEquals(
            links,
            createWebPublication(otherCollections = listOf(
                PublicationCollection(role = "loa", links = links)
            )).listOfAudioClips
        )
    }

    @Test fun `get {listOfAudioClips} when missing`() {
        assertEquals(0, createWebPublication().listOfAudioClips.size)
    }

    @Test fun `get {listOfIllustrations}`() {
        val links = listOf(Link(href = "/image.jpg"))
        assertEquals(
            links,
            createWebPublication(otherCollections = listOf(
                PublicationCollection(role = "loi", links = links)
            )).listOfIllustrations
        )
    }

    @Test fun `get {listOfIllustrations} when missing`() {
        assertEquals(0, createWebPublication().listOfIllustrations.size)
    }

    @Test fun `get {listOfTables}`() {
        val links = listOf(Link(href = "/table.html"))
        assertEquals(
            links,
            createWebPublication(otherCollections = listOf(
                PublicationCollection(role = "lot", links = links)
            )).listOfTables
        )
    }

    @Test fun `get {listOfTables} when missing`() {
        assertEquals(0, createWebPublication().listOfTables.size)
    }

    @Test fun `get {listOfVideoClips}`() {
        val links = listOf(Link(href = "/video.mov"))
        assertEquals(
            links,
            createWebPublication(otherCollections = listOf(
                PublicationCollection(role = "lov", links = links)
            )).listOfVideoClips
        )
    }

    @Test fun `get {listOfVideoClips} when missing`() {
        assertEquals(0, createWebPublication().listOfVideoClips.size)
    }


    // EPUB extensions for [Metadata].

    @Test fun `get Metadata {layout} when available`() {
        assertEquals(
            EpubLayout.FIXED,
            Metadata(
                localizedTitle = LocalizedString("Title"),
                otherMetadata = mapOf("layout" to "fixed")
            ).layout
        )
    }

    @Test fun `get Metadata {layout} when missing`() {
        assertNull(Metadata(localizedTitle = LocalizedString("Title")).layout)
    }


    // EPUB extensions for link [Properties].

    @Test fun `get Properties {contains} when available`() {
        assertEquals(
            setOf("mathml", "onix"),
            Properties(otherProperties = mapOf("contains" to listOf("mathml", "onix"))).contains
        )
    }

    @Test fun `get Properties {contains} removes duplicates`() {
        assertEquals(
            setOf("mathml", "onix"),
            Properties(otherProperties = mapOf("contains" to listOf("mathml", "onix", "onix"))).contains
        )
    }

    @Test fun `get Properties {contains} when missing`() {
        assertEquals(emptySet<String>(), Properties().contains)
    }

    @Test fun `get Properties {contains} skips duplicates`() {
        assertEquals(
            setOf("mathml"),
            Properties(otherProperties = mapOf("contains" to listOf("mathml", "mathml"))).contains
        )
    }

    @Test fun `get Properties {layout} when available`() {
        assertEquals(
            EpubLayout.FIXED,
            Properties(otherProperties = mapOf("layout" to "fixed")).layout
        )
    }

    @Test fun `get Properties {layout} when missing`() {
        assertNull(Properties().layout)
    }

    @Test fun `get Properties {mediaOverlay} when available`() {
        assertEquals(
            "http://media-overlay",
            Properties(otherProperties = mapOf("mediaOverlay" to "http://media-overlay")).mediaOverlay
        )
    }

    @Test fun `get Properties {mediaOverlay} when missing`() {
        assertNull(Properties().mediaOverlay)
    }

    @Test fun `get Properties {encryption} when available`() {
        assertEquals(
            EpubEncryption(algorithm = "http://algo", compression = "gzip"),
            Properties(otherProperties = mapOf("encrypted" to mapOf(
                "algorithm" to "http://algo",
                "compression" to "gzip"
            ))).encryption
        )
    }

    @Test fun `get Properties {encryption} when missing`() {
        assertNull(Properties().encryption)
    }

    @Test fun `get Properties {encryption} when not valid`() {
        assertNull(Properties(otherProperties = mapOf("encrypted" to "invalid")).encryption)
    }

}