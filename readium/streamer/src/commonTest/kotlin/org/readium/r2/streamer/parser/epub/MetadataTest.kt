/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.streamer.parser.epub

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.toInstant
import org.readium.r2.shared.publication.Accessibility
import org.readium.r2.shared.publication.Collection
import org.readium.r2.shared.publication.Contributor
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Layout
import org.readium.r2.shared.publication.Link as SharedLink
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.Subject
import org.readium.r2.shared.publication.Tdm
import org.readium.r2.shared.publication.firstWithRel
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.assertContainsExactlyInAnyOrder

class ContributorParsingTest {
    private val epub2Metadata = parsePackageDocument("package/contributors-epub2.opf").metadata
    private val epub3Metadata = parsePackageDocument("package/contributors-epub3.opf").metadata

    @Test
    fun `dc_creator is by default an author`() {
        val contributor = Contributor(localizedName = LocalizedString("Author 1"))
        assertContains(
            assertNotNull(epub2Metadata.authors),
            contributor
        )
        assertContains(
            assertNotNull(epub3Metadata.authors),
            contributor
        )
    }

    @Test
    fun `dc_publisher is a publisher`() {
        val contributor = Contributor(localizedName = LocalizedString("Publisher 1"))
        assertContains(
            assertNotNull(epub2Metadata.publishers),
            contributor
        )
        assertContains(
            assertNotNull(epub3Metadata.publishers),
            contributor
        )
    }

    @Test
    fun `dc_contributor is by default a contributor`() {
        val contributor = Contributor(localizedName = LocalizedString("Contributor 1"))
        assertContains(
            assertNotNull(epub2Metadata.contributors),
            contributor
        )
        assertContains(
            assertNotNull(epub3Metadata.contributors),
            contributor
        )
    }

    @Test
    fun `Unknown roles are ignored`() {
        val contributor = Contributor(
            localizedName = LocalizedString("Contributor 2"),
            roles = setOf("unknown")
        )
        assertContains(
            assertNotNull(epub2Metadata.contributors),
            contributor
        )
        assertContains(
            assertNotNull(epub3Metadata.contributors),
            contributor
        )
    }

    @Test
    fun `file-as is parsed`() {
        val contributor = Contributor(
            localizedName = LocalizedString("Contributor 3"),
            localizedSortAs = LocalizedString("Sorting Key")
        )
        assertContains(
            assertNotNull(epub2Metadata.contributors),
            contributor
        )
        assertContains(
            assertNotNull(epub3Metadata.contributors),
            contributor
        )
    }

    @Test
    fun `Localized contributors are rightly parsed epub3 only`() {
        val contributor = Contributor(
            localizedName = LocalizedString.fromStrings(
                mapOf(
                    null to "Contributor 4",
                    "fr" to "Contributeur 4 en français"
                )
            )
        )
        assertContains(
            assertNotNull(epub3Metadata.contributors),
            contributor
        )
    }

    @Test
    fun `Only the first role is considered epub3 only`() {
        val contributor = Contributor(localizedName = LocalizedString("Cameleon"))
        assertContains(
            assertNotNull(epub3Metadata.authors),
            contributor
        )
        assertFalse(
            assertNotNull(epub3Metadata.publishers).contains(
                contributor
            )
        )
    }

    @Test
    fun `Media Overlays narrators are rightly parsed epub3 only`() {
        val contributor = Contributor(localizedName = LocalizedString("Media Overlays Narrator"))
        assertContains(
            assertNotNull(epub3Metadata.narrators),
            contributor
        )
    }

    @Test
    fun `Author is rightly parsed`() {
        val contributor = Contributor(localizedName = LocalizedString("Author 2"))
        assertContains(
            assertNotNull(epub2Metadata.authors),
            contributor
        )
        assertContains(
            assertNotNull(epub3Metadata.authors),
            contributor
        )
    }

    @Test
    fun `Publisher is rightly parsed`() {
        val contributor = Contributor(localizedName = LocalizedString("Publisher 2"))
        assertContains(
            assertNotNull(epub2Metadata.publishers),
            contributor
        )
        assertContains(
            assertNotNull(epub3Metadata.publishers),
            contributor
        )
    }

    @Test
    fun `Translator is rightly parsed`() {
        val contributor = Contributor(localizedName = LocalizedString("Translator"))
        assertContains(
            assertNotNull(epub2Metadata.translators),
            contributor
        )
        assertContains(
            assertNotNull(epub3Metadata.translators),
            contributor
        )
    }

    @Test
    fun `Artist is rightly parsed`() {
        val contributor = Contributor(localizedName = LocalizedString("Artist"))
        assertContains(
            assertNotNull(epub2Metadata.artists),
            contributor
        )
        assertContains(
            assertNotNull(epub3Metadata.artists),
            contributor
        )
    }

    @Test
    fun `Illustrator is rightly parsed`() {
        val contributor = Contributor(
            localizedName = LocalizedString("Illustrator"),
            roles = emptySet()
        )
        assertContains(
            assertNotNull(epub2Metadata.illustrators),
            contributor
        )
        assertContains(
            assertNotNull(epub3Metadata.illustrators),
            contributor
        )
    }

    @Test
    fun `Colorist is rightly parsed`() {
        val contributor = Contributor(
            localizedName = LocalizedString("Colorist"),
            roles = emptySet()
        )
        assertContains(
            assertNotNull(epub2Metadata.colorists),
            contributor
        )
        assertContains(
            assertNotNull(epub3Metadata.colorists),
            contributor
        )
    }

    @Test
    fun `Narrator is rightly parsed`() {
        val contributor = Contributor(
            localizedName = LocalizedString("Narrator"),
            roles = emptySet()
        )
        assertContains(
            assertNotNull(epub2Metadata.narrators),
            contributor
        )
        assertContains(
            assertNotNull(epub3Metadata.narrators),
            contributor
        )
    }

    @Test
    fun `No more contributor than needed`() {
        assertEquals(2, epub2Metadata.authors.size)
        assertEquals(2, epub2Metadata.publishers.size)
        assertEquals(1, epub2Metadata.translators.size)
        assertEquals(1, epub2Metadata.editors.size)
        assertEquals(1, epub2Metadata.artists.size)
        assertEquals(1, epub2Metadata.illustrators.size)
        assertEquals(1, epub2Metadata.colorists.size)
        assertEquals(1, epub2Metadata.narrators.size)
        assertEquals(3, epub2Metadata.contributors.size)

        assertEquals(3, epub3Metadata.authors.size)
        assertEquals(2, epub3Metadata.publishers.size)
        assertEquals(1, epub3Metadata.translators.size)
        assertEquals(1, epub3Metadata.editors.size)
        assertEquals(1, epub3Metadata.artists.size)
        assertEquals(1, epub3Metadata.illustrators.size)
        assertEquals(1, epub3Metadata.colorists.size)
        assertEquals(2, epub3Metadata.narrators.size)
        assertEquals(4, epub3Metadata.contributors.size)
    }
}

class TitleTest {
    private val epub2Metadata = parsePackageDocument("package/titles-epub2.opf").metadata
    private val epub3Metadata = parsePackageDocument("package/titles-epub3.opf").metadata

    @Test
    fun `Title is rightly parsed`() {
        assertEquals(
            LocalizedString.fromStrings(
                mapOf(
                    null to "Alice's Adventures in Wonderland"
                )
            ),
            epub2Metadata.localizedTitle
        )
        assertEquals(
            LocalizedString.fromStrings(
                mapOf(
                    null to "Alice's Adventures in Wonderland",
                    "fr" to "Les Aventures d'Alice au pays des merveilles"
                )
            ),
            epub3Metadata.localizedTitle
        )
    }

    @Test
    fun `Subtitle is rightly parsed epub3 only`() {
        assertEquals(
            LocalizedString.fromStrings(
                mapOf(
                    "en-GB" to "Alice returns to the magical world from her childhood adventure",
                    "fr" to "Alice retourne dans le monde magique des aventures de son enfance"
                )
            ),
            epub3Metadata.localizedSubtitle
        )
    }

    @Test
    fun `file-as is parsed`() {
        assertEquals("Adventures", epub2Metadata.sortAs)
        assertEquals("Adventures", epub3Metadata.sortAs)
    }

    @Test
    fun `Main title takes precedence epub3 only`() {
        val metadata = parsePackageDocument("package/title-main-precedence.opf").metadata
        assertEquals("Main title takes precedence", metadata.title)
    }

    @Test
    fun `The selected subtitle has the lowest display-seq property epub3 only`() {
        val metadata = parsePackageDocument("package/title-multiple-subtitles.opf").metadata
        assertEquals(LocalizedString.fromStrings(mapOf(null to "Subtitle 2")), metadata.localizedSubtitle)
    }
}

class SubjectTest {
    private val complexMetadata = parsePackageDocument("package/subjects-complex.opf").metadata // epub3 only

    @Test
    fun `Localized subjects are rightly parsed epub3 only`() {
        val subject = complexMetadata.subjects.first()
        assertNotNull(subject)
        assertEquals(
            LocalizedString.fromStrings(
                mapOf(
                    "en" to "FICTION / Occult & Supernatural",
                    "fr" to "FICTION / Occulte & Surnaturel"
                )
            ),
            subject.localizedName
        )
    }

    @Test
    fun `file-as is rightly parsed epub3 only`() {
        val subject = complexMetadata.subjects.first()
        assertNotNull(subject)
        assertEquals("occult", subject.sortAs)
    }

    @Test
    fun `code and scheme are rightly parsed epub3 only`() {
        val subject = complexMetadata.subjects.first()
        assertNotNull(subject)
        assertEquals("BISAC", subject.scheme)
        assertEquals("FIC024000", subject.code)
    }

    @Test
    fun `Comma separated single subject is splitted`() {
        val subjects = parsePackageDocument("package/subjects-single.opf").metadata.subjects
        assertTrue(
            assertNotNull(subjects).containsAll(
                listOf(
                    Subject(localizedName = LocalizedString("apple")),
                    Subject(localizedName = LocalizedString("banana")),
                    Subject(localizedName = LocalizedString("pear"))
                )
            )
        )
    }

    @Test
    fun `Comma separated multiple subjects are not splitted`() {
        val subjects = parsePackageDocument("package/subjects-multiple.opf").metadata.subjects
        assertTrue(
            assertNotNull(subjects).containsAll(
                listOf(
                    Subject(localizedName = LocalizedString("fiction")),
                    Subject(localizedName = LocalizedString("apple; banana,  pear"))
                )
            )
        )
    }
}

class DateTest {
    private val epub2Metadata = parsePackageDocument("package/dates-epub2.opf").metadata
    private val epub3Metadata = parsePackageDocument("package/dates-epub3.opf").metadata

    @Test
    fun `Publication date is rightly parsed`() {
        val expected = ("1865-07-04T00:00:00Z").toInstant()
        assertEquals(expected, epub2Metadata.published)
        assertEquals(expected, epub3Metadata.published)
    }

    @Test
    fun `Modification date is rightly parsed`() {
        val expected = ("2012-04-02T12:47:00Z").toInstant()
        assertEquals(expected, epub2Metadata.modified)
        assertEquals(expected, epub3Metadata.modified)
    }
}

class MetadataMiscTest {
    @Test
    fun `conformsTo contains the EPUB profile`() {
        val epub2Metadata = parsePackageDocument("package/contributors-epub2.opf").metadata
        assertEquals(setOf(Publication.Profile.EPUB), epub2Metadata.conformsTo)
        val epub3Metadata = parsePackageDocument("package/contributors-epub3.opf").metadata
        assertEquals(setOf(Publication.Profile.EPUB), epub3Metadata.conformsTo)
    }

    @Test
    fun `Unique identifier is rightly parsed`() {
        val expected = "urn:uuid:2"
        assertEquals(expected, parsePackageDocument("package/identifier-unique.opf").metadata.identifier)
    }

    @Test
    fun `Layout property is parsed`() {
        val layout =
            parsePackageDocument("package/presentation-metadata.opf").metadata.layout
        assertEquals(Layout.FIXED, layout)
    }

    @Test
    fun `Cover link is rightly identified`() {
        val expected = SharedLink(
            href = Href("OEBPS/cover.jpg")!!,
            mediaType = MediaType.JPEG,
            rels = setOf("cover")
        )
        assertEquals(expected, parsePackageDocument("package/cover-epub2.opf").resources.firstWithRel("cover"))
        assertEquals(expected, parsePackageDocument("package/cover-epub3.opf").resources.firstWithRel("cover"))
        assertEquals(expected, parsePackageDocument("package/cover-mix.opf").resources.firstWithRel("cover"))
    }

    @Test
    fun `Building of MetaItems terminates even if metadata contain cross refinings`() =
        runTest(timeout = 1.seconds) {
            parsePackageDocument("package/meta-termination.opf")
        }

    @Test
    fun `otherMetadata is rightly filled`() {
        val otherMetadata = parsePackageDocument("package/meta-others.opf").metadata.otherMetadata
        assertEquals(
            listOf(
                mapOf("@value" to "Web", "http://my.url/#scheme" to "http"),
                "Feedbooks"
            ),
            (otherMetadata)[Vocabularies.DCTERMS + "source"]
        )
        assertEquals(
            mapOf(
                "@value" to "refines0",
                "http://my.url/#property1" to mapOf(
                    "@value" to "refines1",
                    "http://my.url/#property2" to "refines2",
                    "http://my.url/#property3" to "refines3"
                )
            ),
            (otherMetadata)["http://my.url/#property0"]
        )
        assertEquals(
            setOf(
                Vocabularies.DCTERMS + "source",
                "http://my.url/#property0"
            ),
            otherMetadata.keys
        )
    }
}

class CollectionTest {
    private val epub2Metadata = parsePackageDocument("package/collections-epub2.opf").metadata
    private val epub3Metadata = parsePackageDocument("package/collections-epub3.opf").metadata

    @Test
    fun `Basic collection are rightly parsed epub3 only`() {
        assertContains(
            assertNotNull(epub3Metadata.belongsToCollections),
            Collection(localizedName = LocalizedString.fromStrings(mapOf("en" to "Collection B")))
        )
    }

    @Test
    fun `Collections with unknown type are put into belongsToCollections epub3 only`() {
        assertContains(
            assertNotNull(epub3Metadata.belongsToCollections),
            Collection(localizedName = LocalizedString.fromStrings(mapOf("en" to "Collection A")))
        )
    }

    @Test
    fun `Localized series are rightly parsed epub3 only`() {
        val names = LocalizedString.fromStrings(
            mapOf(
                "en" to "Series A",
                "fr" to "Série A"
            )
        )
        assertContains(
            assertNotNull(epub3Metadata.belongsToSeries),
            Collection(localizedName = names, identifier = "ser-a", position = 2.0)
        )
    }

    @Test
    fun `Series with position are rightly computed`() {
        val expected =
            Collection(
                localizedName = LocalizedString.fromStrings(mapOf("en" to "Series B")),
                position = 1.5
            )
        assertContains(
            assertNotNull(epub2Metadata.belongsToSeries),
            expected
        )
        assertContains(
            assertNotNull(epub3Metadata.belongsToSeries),
            expected
        )
    }
}

class AccessibilityTest {
    private val epub2Metadata = parsePackageDocument("package/accessibility-epub2.opf").metadata
    private val epub3Metadata = parsePackageDocument("package/accessibility-epub3.opf").metadata

    @Test fun `summary is rightly parsed`() {
        val expected = "The publication contains structural and page navigation."
        assertEquals(expected, epub2Metadata.accessibility?.summary)
        assertEquals(expected, epub3Metadata.accessibility?.summary)
    }

    @Test fun `conformsTo contains WCAG profiles and only them`() {
        assertContainsExactlyInAnyOrder(
            listOf(
                Accessibility.Profile.EPUB_A11Y_10_WCAG_20_A,
                Accessibility.Profile.EPUB_A11Y_11_WCAG_20_AAA,
                Accessibility.Profile.EPUB_A11Y_11_WCAG_21_AA
            ),
            epub2Metadata.accessibility?.conformsTo
        )
        assertContainsExactlyInAnyOrder(
            listOf(
                Accessibility.Profile.EPUB_A11Y_10_WCAG_20_A,
                Accessibility.Profile.EPUB_A11Y_11_WCAG_20_AAA,
                Accessibility.Profile.EPUB_A11Y_11_WCAG_21_AA
            ),
            epub3Metadata.accessibility?.conformsTo
        )
    }

    @Test fun `certification is rightly parsed`() {
        val expectedCertification = Accessibility.Certification(
            certifiedBy = "Accessibility Testers Group",
            credential = "DAISY OK",
            report = "https://example.com/a11y-report/"
        )
        assertEquals(expectedCertification, epub2Metadata.accessibility?.certification)
        assertEquals(expectedCertification, epub3Metadata.accessibility?.certification)
    }

    @Test fun `features are rightly parsed`() {
        assertContainsExactlyInAnyOrder(
            listOf(
                Accessibility.Feature.ALTERNATIVE_TEXT,
                Accessibility.Feature.STRUCTURAL_NAVIGATION
            ),
            epub2Metadata.accessibility?.features
        )
    }

    @Test fun `hazards are rightly parsed`() {
        assertContainsExactlyInAnyOrder(
            listOf(
                Accessibility.Hazard.MOTION_SIMULATION,
                Accessibility.Hazard.NO_SOUND_HAZARD
            ),
            epub2Metadata.accessibility?.hazards
        )
        assertContainsExactlyInAnyOrder(
            listOf(
                Accessibility.Hazard.MOTION_SIMULATION,
                Accessibility.Hazard.NO_SOUND_HAZARD
            ),
            epub3Metadata.accessibility?.hazards
        )
    }

    @Test fun `exemptions are rightly parsed`() {
        assertContainsExactlyInAnyOrder(
            listOf(
                Accessibility.Exemption.EAA_MICROENTERPRISE,
                Accessibility.Exemption.EAA_FUNDAMENTAL_ALTERATION,
                Accessibility.Exemption.EAA_DISPROPORTIONATE_BURDEN
            ),
            epub2Metadata.accessibility?.exemptions
        )
        assertContainsExactlyInAnyOrder(
            listOf(
                Accessibility.Exemption.EAA_MICROENTERPRISE,
                Accessibility.Exemption.EAA_FUNDAMENTAL_ALTERATION,
                Accessibility.Exemption.EAA_DISPROPORTIONATE_BURDEN
            ),
            epub3Metadata.accessibility?.exemptions
        )
    }

    @Test fun `accessModes are rightly parsed`() {
        assertContainsExactlyInAnyOrder(
            listOf(
                Accessibility.AccessMode.VISUAL,
                Accessibility.AccessMode.TEXTUAL
            ),
            epub2Metadata.accessibility?.accessModes
        )
        assertContainsExactlyInAnyOrder(
            listOf(
                Accessibility.AccessMode.VISUAL,
                Accessibility.AccessMode.TEXTUAL
            ),
            epub3Metadata.accessibility?.accessModes
        )
    }

    @Test fun `accessModesSufficient are rightly parsed`() {
        assertContainsExactlyInAnyOrder(
            listOf(
                setOf(
                    Accessibility.PrimaryAccessMode.VISUAL,
                    Accessibility.PrimaryAccessMode.TEXTUAL
                ),
                setOf(Accessibility.PrimaryAccessMode.TEXTUAL)
            ),
            epub2Metadata.accessibility?.accessModesSufficient
        )
        assertContainsExactlyInAnyOrder(
            listOf(
                setOf(
                    Accessibility.PrimaryAccessMode.VISUAL,
                    Accessibility.PrimaryAccessMode.TEXTUAL
                ),
                setOf(Accessibility.PrimaryAccessMode.TEXTUAL)
            ),
            epub3Metadata.accessibility?.accessModesSufficient
        )
    }

    @Test fun `non-accessibility conformsTo end up in otherMetadata`() {
        assertEquals(
            "any profile",
            (epub2Metadata.otherMetadata)[Vocabularies.DCTERMS + "conformsTo"]
        )
        assertEquals(
            "any profile",
            (epub3Metadata.otherMetadata)[Vocabularies.DCTERMS + "conformsTo"]
        )
    }
}

class TdmTest {
    private val epub2Metadata = parsePackageDocument("package/tdm-epub2.opf").metadata
    private val epub3Metadata = parsePackageDocument("package/tdm-epub3.opf").metadata

    @Test fun `TDM is rightly parsed`() {
        val expected = Tdm(
            reservation = Tdm.Reservation.ALL,
            policy = AbsoluteUrl("https://provider.com/policies/policy.json")!!
        )
        assertEquals(expected, epub2Metadata.tdm)
        assertEquals(expected, epub3Metadata.tdm)
    }
}
