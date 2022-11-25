/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.joda.time.DateTime
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.Link as SharedLink
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.publication.presentation.presentation
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContributorParsingTest {
    private val epub2Metadata = parsePackageDocument("package/contributors-epub2.opf").metadata
    private val epub3Metadata = parsePackageDocument("package/contributors-epub3.opf").metadata

    @Test
    fun `dc_creator is by default an author`() {
        val contributor = Contributor(localizedName = LocalizedString("Author 1"))
        assertThat(epub2Metadata.authors).contains(contributor)
        assertThat(epub3Metadata.authors).contains(contributor)
    }

    @Test
    fun `dc_publisher is a publisher`() {
        val contributor = Contributor(localizedName = LocalizedString("Publisher 1"))
        assertThat(epub2Metadata.publishers).contains(contributor)
        assertThat(epub3Metadata.publishers).contains(contributor)
    }

    @Test
    fun `dc_contributor is by default a contributor`() {
        val contributor = Contributor(localizedName = LocalizedString("Contributor 1"))
        assertThat(epub2Metadata.contributors).contains(contributor)
        assertThat(epub3Metadata.contributors).contains(contributor)
    }

    @Test
    fun `Unknown roles are ignored`() {
        val contributor = Contributor(localizedName = LocalizedString("Contributor 2"), roles = setOf("unknown"))
        assertThat(epub2Metadata.contributors).contains(contributor)
        assertThat(epub3Metadata.contributors).contains(contributor)
    }

    @Test
    fun `file-as is parsed`() {
        val contributor = Contributor(
            localizedName = LocalizedString("Contributor 3"),
            localizedSortAs = LocalizedString("Sorting Key")
        )
        assertThat(epub2Metadata.contributors).contains(contributor)
        assertThat(epub3Metadata.contributors).contains(contributor)
    }

    @Test
    fun `Localized contributors are rightly parsed (epub3 only)`() {
        val contributor = Contributor(
            localizedName = LocalizedString.fromStrings(
                mapOf(
                    null to "Contributor 4",
                    "fr" to "Contributeur 4 en français"
                )
            )
        )
        assertThat(epub3Metadata.contributors).contains(contributor)
    }

    @Test
    fun `Only the first role is considered (epub3 only)`() {
        val contributor = Contributor(localizedName = LocalizedString("Cameleon"))
        assertThat(epub3Metadata.authors).contains(contributor)
        assertThat(epub3Metadata.publishers).doesNotContain(contributor)
    }

    @Test
    fun `Media Overlays narrators are rightly parsed (epub3 only)`() {
        val contributor = Contributor(localizedName = LocalizedString("Media Overlays Narrator"))
        assertThat(epub3Metadata.narrators).contains(contributor)
    }

    @Test
    fun `Author is rightly parsed`() {
        val contributor = Contributor(localizedName = LocalizedString("Author 2"))
        assertThat(epub2Metadata.authors).contains(contributor)
        assertThat(epub3Metadata.authors).contains(contributor)
    }

    @Test
    fun `Publisher is rightly parsed`() {
        val contributor = Contributor(localizedName = LocalizedString("Publisher 2"))
        assertThat(epub2Metadata.publishers).contains(contributor)
        assertThat(epub3Metadata.publishers).contains(contributor)
    }

    @Test
    fun `Translator is rightly parsed`() {
        val contributor = Contributor(localizedName = LocalizedString("Translator"))
        assertThat(epub2Metadata.translators).contains(contributor)
        assertThat(epub3Metadata.translators).contains(contributor)
    }

    @Test
    fun `Artist is rightly parsed`() {
        val contributor = Contributor(localizedName = LocalizedString("Artist"))
        assertThat(epub2Metadata.artists).contains(contributor)
        assertThat(epub3Metadata.artists).contains(contributor)
    }

    @Test
    fun `Illustrator is rightly parsed`() {
        val contributor = Contributor(
            localizedName = LocalizedString("Illustrator"),
            roles = emptySet()
        )
        assertThat(epub2Metadata.illustrators).contains(contributor)
        assertThat(epub3Metadata.illustrators).contains(contributor)
    }

    @Test
    fun `Colorist is rightly parsed`() {
        val contributor = Contributor(
            localizedName = LocalizedString("Colorist"),
            roles = emptySet()
        )
        assertThat(epub2Metadata.colorists).contains(contributor)
        assertThat(epub3Metadata.colorists).contains(contributor)
    }

    @Test
    fun `Narrator is rightly parsed`() {
        val contributor = Contributor(
            localizedName = LocalizedString("Narrator"),
            roles = emptySet()
        )
        assertThat(epub2Metadata.narrators).contains(contributor)
        assertThat(epub3Metadata.narrators).contains(contributor)
    }

    @Test
    fun `No more contributor than needed`() {
        assertThat(epub2Metadata.authors).size().isEqualTo(2)
        assertThat(epub2Metadata.publishers).size().isEqualTo(2)
        assertThat(epub2Metadata.translators).size().isEqualTo(1)
        assertThat(epub2Metadata.editors).size().isEqualTo(1)
        assertThat(epub2Metadata.artists).size().isEqualTo(1)
        assertThat(epub2Metadata.illustrators).size().isEqualTo(1)
        assertThat(epub2Metadata.colorists).size().isEqualTo(1)
        assertThat(epub2Metadata.narrators).size().isEqualTo(1)
        assertThat(epub2Metadata.contributors).size().isEqualTo(3)

        assertThat(epub3Metadata.authors).size().isEqualTo(3)
        assertThat(epub3Metadata.publishers).size().isEqualTo(2)
        assertThat(epub3Metadata.translators).size().isEqualTo(1)
        assertThat(epub3Metadata.editors).size().isEqualTo(1)
        assertThat(epub3Metadata.artists).size().isEqualTo(1)
        assertThat(epub3Metadata.illustrators).size().isEqualTo(1)
        assertThat(epub3Metadata.colorists).size().isEqualTo(1)
        assertThat(epub3Metadata.narrators).size().isEqualTo(2)
        assertThat(epub3Metadata.contributors).size().isEqualTo(4)
    }
}

@RunWith(RobolectricTestRunner::class)
class TitleTest {
    private val epub2Metadata = parsePackageDocument("package/titles-epub2.opf").metadata
    private val epub3Metadata = parsePackageDocument("package/titles-epub3.opf").metadata

    @Test
    fun `Title is rightly parsed`() {
        assertThat(epub2Metadata.localizedTitle).isEqualTo(
            LocalizedString.fromStrings(
                mapOf(
                    null to "Alice's Adventures in Wonderland"
                )
            )
        )
        assertThat(epub3Metadata.localizedTitle).isEqualTo(
            LocalizedString.fromStrings(
                mapOf(
                    null to "Alice's Adventures in Wonderland",
                    "fr" to "Les Aventures d'Alice au pays des merveilles"
                )
            )
        )
    }

    @Test
    fun `Subtitle is rightly parsed (epub3 only)`() {
        assertThat(epub3Metadata.localizedSubtitle).isEqualTo(
            LocalizedString.fromStrings(
                mapOf(
                    "en-GB" to "Alice returns to the magical world from her childhood adventure",
                    "fr" to "Alice retourne dans le monde magique des aventures de son enfance"
                )
            )
        )
    }

    @Test
    fun `file-as is parsed`() {
        assertThat(epub2Metadata.sortAs).isEqualTo("Adventures")
        assertThat(epub3Metadata.sortAs).isEqualTo("Adventures")
    }

    @Test
    fun `Main title takes precedence (epub3 only)`() {
        val metadata = parsePackageDocument("package/title-main-precedence.opf").metadata
        assertThat(metadata.title).isEqualTo("Main title takes precedence")
    }

    @Test
    fun `The selected subtitle has the lowest display-seq property (epub3 only)`() {
        val metadata = parsePackageDocument("package/title-multiple-subtitles.opf").metadata
        assertThat(metadata.localizedSubtitle).isEqualTo(LocalizedString.fromStrings(mapOf(null to "Subtitle 2")))
    }
}

@RunWith(RobolectricTestRunner::class)
class SubjectTest {
    private val complexMetadata = parsePackageDocument("package/subjects-complex.opf").metadata // epub3 only

    @Test
    fun `Localized subjects are rightly parsed (epub3 only)`() {
        val subject = complexMetadata.subjects.first()
        assertNotNull(subject)
        assertThat(subject.localizedName).isEqualTo(
            LocalizedString.fromStrings(
                mapOf(
                    "en" to "FICTION / Occult & Supernatural",
                    "fr" to "FICTION / Occulte & Surnaturel"
                )
            )
        )
    }

    @Test
    fun `file-as is rightly parsed (epub3 only)`() {
        val subject = complexMetadata.subjects.first()
        assertNotNull(subject)
        assertThat(subject.sortAs).isEqualTo("occult")
    }

    @Test
    fun `code and scheme are rightly parsed (epub3 only)`() {
        val subject = complexMetadata.subjects.first()
        assertNotNull(subject)
        assertThat(subject.scheme).isEqualTo("BISAC")
        assertThat(subject.code).isEqualTo("FIC024000")
    }

    @Test
    fun `Comma separated single subject is splitted`() {
        val subjects = parsePackageDocument("package/subjects-single.opf").metadata.subjects
        assertThat(subjects).contains(
            Subject(localizedName = LocalizedString("apple")),
            Subject(localizedName = LocalizedString("banana")),
            Subject(localizedName = LocalizedString("pear"))
        )
    }

    @Test
    fun `Comma separated multiple subjects are not splitted`() {
        val subjects = parsePackageDocument("package/subjects-multiple.opf").metadata.subjects
        assertThat(subjects).contains(
            Subject(localizedName = LocalizedString("fiction")),
            Subject(localizedName = LocalizedString("apple; banana,  pear"))
        )
    }
}

@RunWith(RobolectricTestRunner::class)
class DateTest {
    private val epub2Metadata = parsePackageDocument("package/dates-epub2.opf").metadata
    private val epub3Metadata = parsePackageDocument("package/dates-epub3.opf").metadata

    @Test
    fun `Publication date is rightly parsed`() {
        val expected = DateTime.parse("1865-07-04T00:00:00Z").toDate()
        assertThat(epub2Metadata.published).isEqualTo(expected)
        assertThat(epub3Metadata.published).isEqualTo(expected)
    }

    @Test
    fun `Modification date is rightly parsed`() {
        val expected = DateTime.parse("2012-04-02T12:47:00Z").toDate()
        assertThat(epub2Metadata.modified).isEqualTo(expected)
        assertThat(epub3Metadata.modified).isEqualTo(expected)
    }
}

@RunWith(RobolectricTestRunner::class)
class MetadataMiscTest {
    @Test
    fun `conformsTo contains the EPUB profile`() {
        val epub2Metadata = parsePackageDocument("package/contributors-epub2.opf").metadata
        assertThat(epub2Metadata.conformsTo).isEqualTo(setOf(Publication.Profile.EPUB))
        val epub3Metadata = parsePackageDocument("package/contributors-epub3.opf").metadata
        assertThat(epub3Metadata.conformsTo).isEqualTo(setOf(Publication.Profile.EPUB))
    }

    @Test
    fun `Unique identifier is rightly parsed`() {
        val expected = "urn:uuid:2"
        assertThat(parsePackageDocument("package/identifier-unique.opf").metadata.identifier)
            .isEqualTo(expected)
    }

    @Test
    fun `Rendition properties are parsed`() {
        val presentation =
            parsePackageDocument("package/presentation-metadata.opf").metadata.presentation
        assertThat(presentation.continuous).isEqualTo(false)
        assertThat(presentation.overflow).isEqualTo(Presentation.Overflow.SCROLLED)
        assertThat(presentation.spread).isEqualTo(Presentation.Spread.BOTH)
        assertThat(presentation.orientation).isEqualTo(Presentation.Orientation.LANDSCAPE)
        assertThat(presentation.layout).isEqualTo(EpubLayout.FIXED)
    }

    @Test
    fun `Cover link is rightly identified`() {
        val expected = SharedLink(
            href = "/OEBPS/cover.jpg",
            type = "image/jpeg",
            rels = setOf("cover")
        )
        assertThat(parsePackageDocument("package/cover-epub2.opf").resources.firstWithRel("cover"))
            .isEqualTo(expected)
        assertThat(parsePackageDocument("package/cover-epub3.opf").resources.firstWithRel("cover"))
            .isEqualTo(expected)
        assertThat(parsePackageDocument("package/cover-mix.opf").resources.firstWithRel("cover"))
            .isEqualTo(expected)
    }

    @Test(timeout = PARSE_PUB_TIMEOUT)
    fun `Building of MetaItems terminates even if metadata contain cross refinings`() {
        parsePackageDocument("package/meta-termination.opf")
    }

    @Test
    fun `otherMetadata is rightly filled`() {
        val otherMetadata = parsePackageDocument("package/meta-others.opf").metadata.otherMetadata
        assertThat(otherMetadata).contains(
            entry(
                Vocabularies.DCTERMS + "source",
                listOf(
                    "Feedbooks",
                    mapOf("@value" to "Web", "http://my.url/#scheme" to "http"),
                    "Internet"
                )
            ),
            entry(
                "http://my.url/#property0",
                mapOf(
                    "@value" to "refines0",
                    "http://my.url/#property1" to mapOf(
                        "@value" to "refines1",
                        "http://my.url/#property2" to "refines2",
                        "http://my.url/#property3" to "refines3"
                    )
                )
            )
        )
        assertThat(otherMetadata).containsOnlyKeys(
            Vocabularies.DCTERMS + "source",
            "presentation",
            "http://my.url/#property0"
        )
    }
}

@RunWith(RobolectricTestRunner::class)
class CollectionTest {
    private val epub2Metadata = parsePackageDocument("package/collections-epub2.opf").metadata
    private val epub3Metadata = parsePackageDocument("package/collections-epub3.opf").metadata

    @Test
    fun `Basic collection are rightly parsed (epub3 only)`() {
        assertThat(epub3Metadata.belongsToCollections).contains(
            Collection(localizedName = LocalizedString.fromStrings(mapOf("en" to "Collection B")))
        )
    }

    @Test
    fun `Collections with unknown type are put into belongsToCollections (epub3 only)`() {
        assertThat(epub3Metadata.belongsToCollections).contains(
            Collection(localizedName = LocalizedString.fromStrings(mapOf("en" to "Collection A")))
        )
    }

    @Test
    fun `Localized series are rightly parsed (epub3 only`() {
        val names = LocalizedString.fromStrings(
            mapOf(
                "en" to "Series A",
                "fr" to "Série A"
            )
        )
        assertThat(epub3Metadata.belongsToSeries).contains(
            Collection(localizedName = names, identifier = "ser-a", position = 2.0)
        )
    }

    @Test
    fun `Series with position are rightly computed`() {
        val expected =
            Collection(localizedName = LocalizedString.fromStrings(mapOf("en" to "Series B")), position = 1.5)
        assertThat(epub2Metadata.belongsToSeries).contains(expected)
        assertThat(epub3Metadata.belongsToSeries).contains(expected)
    }
}

@RunWith(RobolectricTestRunner::class)
class AccessibilityTest {
    private val epub2Metadata = parsePackageDocument("package/accessibility-epub2.opf").metadata
    private val epub3Metadata = parsePackageDocument("package/accessibility-epub3.opf").metadata

    @Test fun `summary is rightly parsed`() {
        val expected = "The publication contains structural and page navigation."
        assertThat(epub2Metadata.accessibility?.summary).isEqualTo(expected)
        assertThat(epub3Metadata.accessibility?.summary).isEqualTo(expected)
    }

    @Test fun `conformsTo contains WCAG profiles and only them`() {
        assertThat(epub2Metadata.accessibility?.conformsTo).containsExactlyInAnyOrder(Accessibility.Profile.EPUB_A11Y_10_WCAG_20_A)
        assertThat(epub3Metadata.accessibility?.conformsTo).containsExactlyInAnyOrder(Accessibility.Profile.EPUB_A11Y_10_WCAG_20_A)
    }

    @Test fun `certification is rightly parsed`() {
        val expectedCertification = Accessibility.Certification(
            certifiedBy = "Accessibility Testers Group",
            credential = "DAISY OK",
            report = "https://example.com/a11y-report/"
        )
        assertThat(epub2Metadata.accessibility?.certification).isEqualTo(expectedCertification)
        assertThat(epub3Metadata.accessibility?.certification).isEqualTo(expectedCertification)
    }

    @Test fun `features are rightly parsed`() {
        assertThat(epub2Metadata.accessibility?.features)
            .containsExactlyInAnyOrder(Accessibility.Feature.ALTERNATIVE_TEXT, Accessibility.Feature.STRUCTURAL_NAVIGATION)
    }

    @Test fun `hazards are rightly parsed`() {
        assertThat(epub2Metadata.accessibility?.hazards)
            .containsExactlyInAnyOrder(Accessibility.Hazard.MOTION_SIMULATION, Accessibility.Hazard.NO_SOUND_HAZARD)
        assertThat(epub3Metadata.accessibility?.hazards)
            .containsExactlyInAnyOrder(Accessibility.Hazard.MOTION_SIMULATION, Accessibility.Hazard.NO_SOUND_HAZARD)
    }

    @Test fun `accessModes are rightly parsed`() {
        assertThat(epub2Metadata.accessibility?.accessModes)
            .containsExactlyInAnyOrder(Accessibility.AccessMode.VISUAL, Accessibility.AccessMode.TEXTUAL)
        assertThat(epub3Metadata.accessibility?.accessModes)
            .containsExactlyInAnyOrder(Accessibility.AccessMode.VISUAL, Accessibility.AccessMode.TEXTUAL)
    }

    @Test fun `accessModesSufficient are rightly parsed`() {
        assertThat(epub2Metadata.accessibility?.accessModesSufficient)
            .containsExactlyInAnyOrder(
                setOf(Accessibility.PrimaryAccessMode.VISUAL, Accessibility.PrimaryAccessMode.TEXTUAL),
                setOf(Accessibility.PrimaryAccessMode.TEXTUAL)
            )
        assertThat(epub3Metadata.accessibility?.accessModesSufficient)
            .containsExactlyInAnyOrder(
                setOf(Accessibility.PrimaryAccessMode.VISUAL, Accessibility.PrimaryAccessMode.TEXTUAL),
                setOf(Accessibility.PrimaryAccessMode.TEXTUAL)
            )
    }

    @Test fun `non-accessibility conformsTo end up in otherMetadata`() {
        assertThat(epub2Metadata.otherMetadata).contains(
            entry(
                Vocabularies.DCTERMS + "conformsTo",
                "any profile"
            )
        )
        assertThat(epub3Metadata.otherMetadata).contains(
            entry(
                Vocabularies.DCTERMS + "conformsTo",
                "any profile"
            )
        )
    }
}
