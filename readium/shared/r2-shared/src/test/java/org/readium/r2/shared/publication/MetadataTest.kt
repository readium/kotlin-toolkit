/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.extensions.iso8601ToDate

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
                localizedTitle = LocalizedString.fromStrings(mapOf(
                    "en" to "Title",
                    "fr" to "Titre"
                )),
                localizedSubtitle = LocalizedString.fromStrings(mapOf(
                    "en" to "Subtitle",
                    "fr" to "Sous-titre"
                )),
                modified = "2001-01-01T12:36:27.000Z".iso8601ToDate(),
                published = "2001-01-02T12:36:27.000Z".iso8601ToDate(),
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
                belongsToCollections = listOf(Contributor(name = "Collection")),
                belongsToSeries = listOf(Contributor(name = "Series")),
                otherMetadata = mapOf(
                    "other-metadata1" to "value",
                    "other-metadata2" to listOf(42)
                )
            ),
            Metadata.fromJSON(JSONObject("""{
                "identifier": "1234",
                "@type": "epub",
                "title": {"en": "Title", "fr": "Titre"},
                "subtitle": {"en": "Subtitle", "fr": "Sous-titre"},
                "modified": "2001-01-01T12:36:27.000Z",
                "published": "2001-01-02T12:36:27.000Z",
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
                    "series": "Series"
                },
                "other-metadata1": "value",
                "other-metadata2": [42]
            }"""))
        )
    }

    @Test fun `parse null JSON`() {
        assertNull(Metadata.fromJSON(null))
    }

    @Test fun `parse JSON with single language`() {
        assertEquals(
            Metadata(
                localizedTitle = LocalizedString("Title"),
                languages = listOf("fr")
            ),
            Metadata.fromJSON(JSONObject("""{
                "title": "Title",
                "language": "fr"
            }"""))
        )
    }

    @Test fun `parse JSON requires {title}`() {
        assertNull(Metadata.fromJSON(JSONObject("{'duration': 4.24}")))
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
            JSONObject("""{
                "title": {"und": "Title"},
                "readingProgression": "auto"
            }"""),
            Metadata(localizedTitle = LocalizedString("Title")).toJSON()
        )
    }

    @Test fun `get full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "identifier": "1234",
                "@type": "epub",
                "title": {"en": "Title", "fr": "Titre"},
                "subtitle": {"en": "Subtitle", "fr": "Sous-titre"},
                "modified": "2001-01-01T12:36:27.000Z",
                "published": "2001-01-02T12:36:27.000Z",
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
                    "series": [{"name": {"und": "Series"}}]
                },
                "other-metadata1": "value",
                "other-metadata2": [42]
            }"""),
            Metadata(
                identifier = "1234",
                type = "epub",
                localizedTitle = LocalizedString.fromStrings(mapOf(
                    "en" to "Title",
                    "fr" to "Titre"
                )),
                localizedSubtitle = LocalizedString.fromStrings(mapOf(
                    "en" to "Subtitle",
                    "fr" to "Sous-titre"
                )),
                modified = "2001-01-01T12:36:27.000Z".iso8601ToDate(),
                published = "2001-01-02T12:36:27.000Z".iso8601ToDate(),
                languages = listOf("en", "fr"),
                localizedSortAs = LocalizedString.fromStrings(mapOf(
                    "en" to "sort key",
                    "fr" to "clé de tri"
                )),
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
                belongsToCollections = listOf(Contributor(name = "Collection")),
                belongsToSeries = listOf(Contributor(name = "Series")),
                otherMetadata = mapOf(
                    "other-metadata1" to "value",
                    "other-metadata2" to listOf(42)
                )
            ).toJSON()
        )
    }

    @Test
    fun `effectiveReadingProgression falls back on LTR`() {
        val metadata = createMetadata(languages = emptyList(), readingProgression = ReadingProgression.AUTO)
        assertEquals(ReadingProgression.LTR, metadata.effectiveReadingProgression)
    }

    @Test
    fun `effectiveReadingProgression falls back on priveded reading progression`() {
        val metadata = createMetadata(languages = emptyList(), readingProgression = ReadingProgression.RTL)
        assertEquals(ReadingProgression.RTL, metadata.effectiveReadingProgression)
    }

    @Test
    fun `effectiveReadingProgression with RTL languages`() {
        assertEquals(ReadingProgression.RTL, createMetadata(languages = listOf("zh-Hant"), readingProgression = ReadingProgression.AUTO).effectiveReadingProgression)
        assertEquals(ReadingProgression.RTL, createMetadata(languages = listOf("zh-TW"), readingProgression = ReadingProgression.AUTO).effectiveReadingProgression)
        assertEquals(ReadingProgression.RTL, createMetadata(languages = listOf("ar"), readingProgression = ReadingProgression.AUTO).effectiveReadingProgression)
        assertEquals(ReadingProgression.RTL, createMetadata(languages = listOf("fa"), readingProgression = ReadingProgression.AUTO).effectiveReadingProgression)
        assertEquals(ReadingProgression.RTL, createMetadata(languages = listOf("he"), readingProgression = ReadingProgression.AUTO).effectiveReadingProgression)
        assertEquals(ReadingProgression.LTR, createMetadata(languages = listOf("he"), readingProgression = ReadingProgression.LTR).effectiveReadingProgression)
    }

    @Test
    fun `effectiveReadingProgression ignores multiple languages`() {
        assertEquals(ReadingProgression.LTR, createMetadata(languages = listOf("ar", "fa"), readingProgression = ReadingProgression.AUTO).effectiveReadingProgression)
    }

    @Test
    fun `effectiveReadingProgression ignores language case`() {
        assertEquals(ReadingProgression.RTL, createMetadata(languages = listOf("AR"), readingProgression = ReadingProgression.AUTO).effectiveReadingProgression)
    }

    @Test
    fun `effectiveReadingProgression ignores language region, except for Chinese`() {
        assertEquals(ReadingProgression.RTL, createMetadata(languages = listOf("ar-foo"), readingProgression = ReadingProgression.AUTO).effectiveReadingProgression)
        // But not for ZH
        assertEquals(ReadingProgression.LTR, createMetadata(languages = listOf("zh-foo"), readingProgression = ReadingProgression.AUTO).effectiveReadingProgression)
    }

    private fun createMetadata(languages: List<String>, readingProgression: ReadingProgression): Metadata =
        Metadata(localizedTitle = LocalizedString("Title"), languages = languages, readingProgression = readingProgression)

}
