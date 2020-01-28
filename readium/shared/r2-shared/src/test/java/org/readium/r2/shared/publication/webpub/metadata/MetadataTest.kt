/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.webpub.metadata

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.extensions.toIso8601Date
import org.readium.r2.shared.publication.webpub.LocalizedString
import org.readium.r2.shared.publication.webpub.ReadingProgression

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
                localizedTitle = LocalizedString(mapOf(
                    "en" to "Title",
                    "fr" to "Titre"
                )),
                localizedSubtitle = LocalizedString(mapOf(
                    "en" to "Subtitle",
                    "fr" to "Sous-titre"
                )),
                modified = "2001-01-01T12:36:27.000Z".toIso8601Date(),
                published = "2001-01-02T12:36:27.000Z".toIso8601Date(),
                languages = listOf("en", "fr"),
                sortAs = "sort key",
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
                "title": {"UND": "Title"},
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
                "sortAs": "sort key",
                "subject": [
                    {"name": {"UND": "Science Fiction"}},
                    {"name": {"UND": "Fantasy"}}
                ],
                "author": [{"name": {"UND": "Author"}}],
                "translator": [{"name": {"UND": "Translator"}}],
                "editor": [{"name": {"UND": "Editor"}}],
                "artist": [{"name": {"UND": "Artist"}}],
                "illustrator": [{"name": {"UND": "Illustrator"}}],
                "letterer": [{"name": {"UND": "Letterer"}}],
                "penciler": [{"name": {"UND": "Penciler"}}],
                "colorist": [{"name": {"UND": "Colorist"}}],
                "inker": [{"name": {"UND": "Inker"}}],
                "narrator": [{"name": {"UND": "Narrator"}}],
                "contributor": [{"name": {"UND": "Contributor"}}],
                "publisher": [{"name": {"UND": "Publisher"}}],
                "imprint": [{"name": {"UND": "Imprint"}}],
                "readingProgression": "rtl",
                "description": "Description",
                "duration": 4.24,
                "numberOfPages": 240,
                "belongsTo": {
                    "collection": [{"name": {"UND": "Collection"}}],
                    "series": [{"name": {"UND": "Series"}}]
                },
                "other-metadata1": "value",
                "other-metadata2": [42]
            }"""),
            Metadata(
                identifier = "1234",
                type = "epub",
                localizedTitle = LocalizedString(mapOf(
                    "en" to "Title",
                    "fr" to "Titre"
                )),
                localizedSubtitle = LocalizedString(mapOf(
                    "en" to "Subtitle",
                    "fr" to "Sous-titre"
                )),
                modified = "2001-01-01T12:36:27.000Z".toIso8601Date(),
                published = "2001-01-02T12:36:27.000Z".toIso8601Date(),
                languages = listOf("en", "fr"),
                sortAs = "sort key",
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

}
