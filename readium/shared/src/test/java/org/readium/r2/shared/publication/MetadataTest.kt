/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Instant
import org.readium.r2.shared.util.Language
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetadataTest {

    @Test fun `parse minimal JSON`() {
        assertEquals(
            Metadata(localizedTitle = LocalizedString("Title")),
            Metadata.fromJSON(JSONObject("{'title': 'Title'}"))
        )
    }

    @Test fun `parse full JSON`() {
        assertEquals(
            Metadata(
                identifier = "1234",
                type = "epub",
                conformsTo = setOf(Publication.Profile.EPUB, Publication.Profile.PDF),
                localizedTitle = LocalizedString.fromStrings(
                    mapOf(
                        "en" to "Title",
                        "fr" to "Titre"
                    )
                ),
                localizedSubtitle = LocalizedString.fromStrings(
                    mapOf(
                        "en" to "Subtitle",
                        "fr" to "Sous-titre"
                    )
                ),
                accessibility = Accessibility(
                    conformsTo = setOf(Accessibility.Profile.EPUB_A11Y_10_WCAG_20_A),
                    accessModes = setOf(Accessibility.AccessMode.TEXTUAL),
                    accessModesSufficient = setOf(setOf(Accessibility.PrimaryAccessMode.TEXTUAL)),
                    features = setOf(Accessibility.Feature.ARIA),
                    hazards = setOf(Accessibility.Hazard.FLASHING)
                ),
                modified = Instant.parse("2001-01-01T12:36:27.000Z"),
                published = Instant.parse("2001-01-02T12:36:27.000Z"),

                languages = listOf("en", "fr"),
                localizedSortAs = LocalizedString("sort key"),
                subjects = listOf(Subject(name = "Science Fiction"), Subject(name = "Fantasy")),
                authors = listOf(Contributor(name = "Author")),
                translators = listOf(Contributor(name = "Translator")),
                editors = listOf(Contributor(name = "Editor")),
                artists = listOf(Contributor(name = "Artist")),
                illustrators = listOf(Contributor(name = "Illustrator")),
                letterers = listOf(Contributor(name = "Letterer")),
                pencilers = listOf(Contributor(name = "Penciler")),
                colorists = listOf(Contributor(name = "Colorist")),
                inkers = listOf(Contributor(name = "Inker")),
                narrators = listOf(Contributor(name = "Narrator")),
                contributors = listOf(Contributor(name = "Contributor")),
                publishers = listOf(Contributor(name = "Publisher")),
                imprints = listOf(Contributor(name = "Imprint")),
                readingProgression = ReadingProgression.RTL,
                description = "Description",
                duration = 4.24,
                numberOfPages = 240,
                belongsTo = mapOf(
                    "schema:Periodical" to listOf(Contributor(name = "Periodical")),
                    "schema:Newspaper" to listOf(
                        Contributor(name = "Newspaper 1"),
                        Contributor(name = "Newspaper 2")
                    )
                ),
                belongsToCollections = listOf(Contributor(name = "Collection")),
                belongsToSeries = listOf(Contributor(name = "Series")),
                tdm = Tdm(
                    reservation = Tdm.Reservation.ALL,
                    policy = AbsoluteUrl("http://example.com/tdm-policy")!!
                ),
                otherMetadata = mapOf(
                    "other-metadata1" to "value",
                    "other-metadata2" to listOf(42)
                )
            ),
            Metadata.fromJSON(
                JSONObject(
                    """{
                "identifier": "1234",
                "@type": "epub",
                "conformsTo": [
                    "https://readium.org/webpub-manifest/profiles/epub",
                    "https://readium.org/webpub-manifest/profiles/pdf"
                ],
                "title": {"en": "Title", "fr": "Titre"},
                "subtitle": {"en": "Subtitle", "fr": "Sous-titre"},
                "modified": "2001-01-01T12:36:27.000Z",
                "published": "2001-01-02T12:36:27.000Z",
                "accessibility": {
                    "conformsTo": "http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-a",
                    "accessMode": ["textual"],
                    "accessModeSufficient": ["textual"],
                    "hazard": ["flashing"],
                    "feature": ["ARIA"]
                },
                "language": ["en", "fr"],
                "sortAs": "sort key",
                "subject": ["Science Fiction", "Fantasy"],
                "author": "Author",
                "translator": "Translator",
                "editor": "Editor",
                "artist": "Artist",
                "illustrator": "Illustrator",
                "letterer": "Letterer",
                "penciler": "Penciler",
                "colorist": "Colorist",
                "inker": "Inker",
                "narrator": "Narrator",
                "contributor": "Contributor",
                "publisher": "Publisher",
                "imprint": "Imprint",
                "readingProgression": "rtl",
                "description": "Description",
                "duration": 4.24,
                "numberOfPages": 240,
                "belongsTo": {
                    "collection": "Collection",
                    "series": "Series",
                    "schema:Periodical": "Periodical",
                    "schema:Newspaper": [ "Newspaper 1", "Newspaper 2" ]
                },
                "tdm": {
                    "reservation": "all",
                    "policy": "http://example.com/tdm-policy"
                },
                "other-metadata1": "value",
                "other-metadata2": [42]
            }"""
                )
            )
        )
    }

    @Test fun `parse null JSON`() {
        assertNull(Metadata.fromJSON(null))
    }

    @Test fun `parse JSON with single profile`() {
        assertEquals(
            Metadata(
                conformsTo = setOf(Publication.Profile.DIVINA),
                localizedTitle = LocalizedString("Title")
            ),
            Metadata.fromJSON(
                JSONObject(
                    """{
                "title": "Title",
                "conformsTo": "https://readium.org/webpub-manifest/profiles/divina"
            }"""
                )
            )
        )
    }

    @Test fun `parse JSON with single language`() {
        assertEquals(
            Metadata(
                localizedTitle = LocalizedString("Title"),
                languages = listOf("fr")
            ),
            Metadata.fromJSON(
                JSONObject(
                    """{
                "title": "Title",
                "language": "fr"
            }"""
                )
            )
        )
    }

    @Test fun `parse JSON {duration} requires positive`() {
        assertEquals(
            Metadata(localizedTitle = LocalizedString("t")),
            Metadata.fromJSON(JSONObject("{'title': 't', 'duration': -20}"))
        )
    }

    @Test fun `parse JSON {numberOfPages} requires positive`() {
        assertEquals(
            Metadata(localizedTitle = LocalizedString("t")),
            Metadata.fromJSON(JSONObject("{'title': 't', 'numberOfPages': -20}"))
        )
    }

    @Test fun `get minimal JSON`() {
        assertJSONEquals(
            JSONObject(
                """{
                "title": {"und": "Title"},
                "readingProgression": "auto"
            }"""
            ),
            Metadata(localizedTitle = LocalizedString("Title")).toJSON()
        )
    }

    @Test fun `get full JSON`() {
        assertJSONEquals(
            JSONObject(
                """{
                "identifier": "1234",
                "@type": "epub",
                "conformsTo": [
                    "https://readium.org/webpub-manifest/profiles/epub",
                    "https://readium.org/webpub-manifest/profiles/pdf"
                ],
                "title": {"en": "Title", "fr": "Titre"},
                "subtitle": {"en": "Subtitle", "fr": "Sous-titre"},
                "modified": "2001-01-01T12:36:27Z",
                "published": "2001-01-02T12:36:27Z",
                "accessibility": {
                    "conformsTo": ["http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-a"],
                    "accessMode": ["textual"],
                    "accessModeSufficient": [["textual"]],
                    "hazard": ["flashing"],
                    "feature": ["ARIA"]
                },
                "language": ["en", "fr"],
                "sortAs": {"en": "sort key", "fr": "clé de tri"},
                "subject": [
                    {"name": {"und": "Science Fiction"}},
                    {"name": {"und": "Fantasy"}}
                ],
                "author": [{"name": {"und": "Author"}}],
                "translator": [{"name": {"und": "Translator"}}],
                "editor": [{"name": {"und": "Editor"}}],
                "artist": [{"name": {"und": "Artist"}}],
                "illustrator": [{"name": {"und": "Illustrator"}}],
                "letterer": [{"name": {"und": "Letterer"}}],
                "penciler": [{"name": {"und": "Penciler"}}],
                "colorist": [{"name": {"und": "Colorist"}}],
                "inker": [{"name": {"und": "Inker"}}],
                "narrator": [{"name": {"und": "Narrator"}}],
                "contributor": [{"name": {"und": "Contributor"}}],
                "publisher": [{"name": {"und": "Publisher"}}],
                "imprint": [{"name": {"und": "Imprint"}}],
                "readingProgression": "rtl",
                "description": "Description",
                "duration": 4.24,
                "numberOfPages": 240,
                "belongsTo": {
                    "collection": [{"name": {"und": "Collection"}}],
                    "series": [{"name": {"und": "Series"}}],
                    "schema:Periodical": [{"name": {"und": "Periodical"}}]
                },
                "tdm": {
                    "reservation": "all",
                    "policy": "http://example.com/tdm-policy"
                },
                "other-metadata1": "value",
                "other-metadata2": [42]
            }"""
            ),
            Metadata(
                identifier = "1234",
                type = "epub",
                conformsTo = setOf(Publication.Profile.EPUB, Publication.Profile.PDF),
                localizedTitle = LocalizedString.fromStrings(
                    mapOf(
                        "en" to "Title",
                        "fr" to "Titre"
                    )
                ),
                localizedSubtitle = LocalizedString.fromStrings(
                    mapOf(
                        "en" to "Subtitle",
                        "fr" to "Sous-titre"
                    )
                ),
                modified = Instant.parse("2001-01-01T12:36:27.000Z"),
                published = Instant.parse("2001-01-02T12:36:27.000Z"),
                accessibility = Accessibility(
                    conformsTo = setOf(Accessibility.Profile.EPUB_A11Y_10_WCAG_20_A),
                    accessModes = setOf(Accessibility.AccessMode.TEXTUAL),
                    accessModesSufficient = setOf(setOf(Accessibility.PrimaryAccessMode.TEXTUAL)),
                    features = setOf(Accessibility.Feature.ARIA),
                    hazards = setOf(Accessibility.Hazard.FLASHING)
                ),
                languages = listOf("en", "fr"),
                localizedSortAs = LocalizedString.fromStrings(
                    mapOf(
                        "en" to "sort key",
                        "fr" to "clé de tri"
                    )
                ),
                subjects = listOf(Subject(name = "Science Fiction"), Subject(name = "Fantasy")),
                authors = listOf(Contributor(name = "Author")),
                translators = listOf(Contributor(name = "Translator")),
                editors = listOf(Contributor(name = "Editor")),
                artists = listOf(Contributor(name = "Artist")),
                illustrators = listOf(Contributor(name = "Illustrator")),
                letterers = listOf(Contributor(name = "Letterer")),
                pencilers = listOf(Contributor(name = "Penciler")),
                colorists = listOf(Contributor(name = "Colorist")),
                inkers = listOf(Contributor(name = "Inker")),
                narrators = listOf(Contributor(name = "Narrator")),
                contributors = listOf(Contributor(name = "Contributor")),
                publishers = listOf(Contributor(name = "Publisher")),
                imprints = listOf(Contributor(name = "Imprint")),
                readingProgression = ReadingProgression.RTL,
                description = "Description",
                duration = 4.24,
                numberOfPages = 240,
                belongsTo = mapOf("schema:Periodical" to listOf(Contributor(name = "Periodical"))),
                belongsToCollections = listOf(Contributor(name = "Collection")),
                belongsToSeries = listOf(Contributor(name = "Series")),
                tdm = Tdm(
                    reservation = Tdm.Reservation.ALL,
                    policy = AbsoluteUrl("http://example.com/tdm-policy")!!
                ),
                otherMetadata = mapOf(
                    "other-metadata1" to "value",
                    "other-metadata2" to listOf(42)
                )
            ).toJSON()
        )
    }

    @Test fun `get primary language with no language`() {
        assertNull(createMetadata(languages = listOf(), readingProgression = null).language)
        assertNull(
            createMetadata(languages = listOf(), readingProgression = ReadingProgression.LTR).language
        )
        assertNull(
            createMetadata(languages = listOf(), readingProgression = ReadingProgression.RTL).language
        )
    }

    @Test fun `get primary language with a single language`() {
        assertEquals(
            Language("en"),
            createMetadata(languages = listOf("en"), readingProgression = null).language
        )
        assertEquals(
            Language("en"),
            createMetadata(languages = listOf("en"), readingProgression = ReadingProgression.LTR).language
        )
        assertEquals(
            Language("en"),
            createMetadata(languages = listOf("en"), readingProgression = ReadingProgression.RTL).language
        )
    }

    private fun createMetadata(languages: List<String>, readingProgression: ReadingProgression?): Metadata =
        Metadata(
            localizedTitle = LocalizedString("Title"),
            languages = languages,
            readingProgression = readingProgression
        )
}
