/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.accessibility

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.R
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.AccessibilitySummary
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.AdditionalInformation
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.Conformance
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.Hazards
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.Hazards.Hazard
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.Legal
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.Navigation
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.RichContent
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.StaticStatement
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.WaysOfReading
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.publication.Accessibility
import org.readium.r2.shared.publication.Accessibility.AccessMode
import org.readium.r2.shared.publication.Accessibility.Exemption
import org.readium.r2.shared.publication.Accessibility.Feature
import org.readium.r2.shared.publication.Accessibility.PrimaryAccessMode
import org.readium.r2.shared.publication.Accessibility.Profile
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.Presentation
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

private typealias S = AccessibilityDisplayString

@RunWith(RobolectricTestRunner::class)
class AccessibilityMetadataDisplayGuideTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun `ways of reading visual adjustments initialization`() {
        fun test(
            layout: EpubLayout,
            a11y: Accessibility?,
            expected: WaysOfReading.VisualAdjustments,
        ) {
            val publication = publication(layout = layout, accessibility = a11y)
            val sut = WaysOfReading(publication)
            assertEquals(expected, sut.visualAdjustments)
        }

        val displayTransformability =
            Accessibility(features = setOf(Feature.DISPLAY_TRANSFORMABILITY))

        test(EpubLayout.REFLOWABLE, null, WaysOfReading.VisualAdjustments.UNKNOWN)
        test(
            EpubLayout.REFLOWABLE,
            displayTransformability,
            WaysOfReading.VisualAdjustments.MODIFIABLE
        )
        test(EpubLayout.FIXED, null, WaysOfReading.VisualAdjustments.UNMODIFIABLE)
        test(EpubLayout.FIXED, displayTransformability, WaysOfReading.VisualAdjustments.MODIFIABLE)
    }

    @Test
    fun `ways of reading nonvisual reading initialization`() {
        fun test(a11y: Accessibility?, expected: WaysOfReading.NonvisualReading) {
            val publication = publication(accessibility = a11y)
            val sut = WaysOfReading(publication)
            assertEquals(expected, sut.nonvisualReading)
        }

        // No metadata
        test(null, WaysOfReading.NonvisualReading.NO_METADATA)
        test(
            Accessibility(accessModes = emptySet(), accessModesSufficient = emptySet()),
            WaysOfReading.NonvisualReading.NO_METADATA
        )

        // Readable conditions
        test(
            Accessibility(accessModes = setOf(AccessMode.TEXTUAL)),
            WaysOfReading.NonvisualReading.READABLE
        )
        test(
            Accessibility(
                accessModes = setOf(AccessMode.AUDITORY),
                accessModesSufficient = setOf(setOf(PrimaryAccessMode.TEXTUAL))
            ),
            WaysOfReading.NonvisualReading.READABLE
        )

        // Partially readable conditions
        test(
            Accessibility(accessModes = setOf(AccessMode.TEXTUAL, AccessMode.AUDITORY)),
            WaysOfReading.NonvisualReading.NOT_FULLY
        )
        test(
            Accessibility(
                accessModesSufficient = setOf(
                    setOf(
                        PrimaryAccessMode.TEXTUAL,
                        PrimaryAccessMode.AUDITORY
                    )
                )
            ),
            WaysOfReading.NonvisualReading.NOT_FULLY
        )
        test(
            Accessibility(
                accessModes = setOf(AccessMode.VISUAL),
                features = setOf(Feature.LONG_DESCRIPTION)
            ),
            WaysOfReading.NonvisualReading.NOT_FULLY
        )
        test(
            Accessibility(
                accessModes = setOf(AccessMode.VISUAL),
                features = setOf(Feature.ALTERNATIVE_TEXT)
            ),
            WaysOfReading.NonvisualReading.NOT_FULLY
        )
        test(
            Accessibility(
                accessModes = setOf(AccessMode.VISUAL),
                features = setOf(Feature.DESCRIBED_MATH)
            ),
            WaysOfReading.NonvisualReading.NOT_FULLY
        )
        test(
            Accessibility(
                accessModes = setOf(AccessMode.VISUAL),
                features = setOf(Feature.TRANSCRIPT)
            ),
            WaysOfReading.NonvisualReading.NOT_FULLY
        )

        // Unreadable conditions
        test(
            Accessibility(accessModes = setOf(AccessMode.AUDITORY)),
            WaysOfReading.NonvisualReading.UNREADABLE
        )
        test(
            Accessibility(accessModes = setOf(AccessMode.VISUAL)),
            WaysOfReading.NonvisualReading.UNREADABLE
        )
        test(
            Accessibility(
                accessModes = setOf(
                    AccessMode.VISUAL,
                    AccessMode.AUDITORY,
                    AccessMode.MATH_ON_VISUAL
                )
            ),
            WaysOfReading.NonvisualReading.UNREADABLE
        )
    }

    @Test
    fun `ways of reading nonvisual reading alt text initialization`() {
        fun test(a11y: Accessibility?, expected: Boolean) {
            val publication = publication(accessibility = a11y)
            val sut = WaysOfReading(publication)
            assertEquals(expected, sut.nonvisualReadingAltText)
        }

        // No metadata
        test(null, false)

        // No textual alternative features
        test(Accessibility(), false)
        test(Accessibility(features = setOf(Feature.ANNOTATIONS)), false)

        // With textual alternative features
        test(Accessibility(features = setOf(Feature.LONG_DESCRIPTION)), true)
        test(Accessibility(features = setOf(Feature.ALTERNATIVE_TEXT)), true)
        test(Accessibility(features = setOf(Feature.DESCRIBED_MATH)), true)
        test(Accessibility(features = setOf(Feature.TRANSCRIPT)), true)
    }

    @Test
    fun `ways of reading prerecorded audio initialization`() {
        fun test(a11y: Accessibility?, expected: WaysOfReading.PrerecordedAudio) {
            val publication = publication(accessibility = a11y)
            val sut = WaysOfReading(publication)
            assertEquals(expected, sut.prerecordedAudio)
        }

        // No metadata
        test(null, WaysOfReading.PrerecordedAudio.NO_METADATA)
        test(
            Accessibility(accessModes = emptySet(), accessModesSufficient = emptySet()),
            WaysOfReading.PrerecordedAudio.NO_METADATA
        )

        // No audio detected
        test(
            Accessibility(
                accessModes = setOf(AccessMode.TEXTUAL),
                accessModesSufficient = setOf(setOf(PrimaryAccessMode.TEXTUAL))
            ),
            WaysOfReading.PrerecordedAudio.NO_METADATA
        )

        // Audio is sufficient
        test(
            Accessibility(
                accessModes = setOf(AccessMode.TEXTUAL),
                accessModesSufficient = setOf(
                    setOf(PrimaryAccessMode.AUDITORY),
                    setOf(
                        PrimaryAccessMode.TEXTUAL
                    )
                )
            ),
            WaysOfReading.PrerecordedAudio.AUDIO_ONLY
        )
        test(
            Accessibility(
                accessModes = setOf(AccessMode.TEXTUAL, AccessMode.AUDITORY),
                accessModesSufficient = setOf(
                    setOf(PrimaryAccessMode.AUDITORY),
                    setOf(
                        PrimaryAccessMode.TEXTUAL
                    )
                )
            ),
            WaysOfReading.PrerecordedAudio.AUDIO_ONLY
        )

        // Some audio content
        test(
            Accessibility(accessModes = setOf(AccessMode.TEXTUAL, AccessMode.AUDITORY)),
            WaysOfReading.PrerecordedAudio.AUDIO_COMPLEMENTARY
        )
        test(
            Accessibility(
                accessModes = setOf(AccessMode.AUDITORY),
                accessModesSufficient = setOf(setOf(PrimaryAccessMode.TEXTUAL))
            ),
            WaysOfReading.PrerecordedAudio.AUDIO_COMPLEMENTARY
        )

        // Synchronized audio detected
        test(
            Accessibility(
                accessModes = setOf(AccessMode.TEXTUAL),
                accessModesSufficient = setOf(setOf(PrimaryAccessMode.TEXTUAL)),
                features = setOf(Feature.SYNCHRONIZED_AUDIO_TEXT)
            ),
            WaysOfReading.PrerecordedAudio.SYNCHRONIZED
        )
    }

    @Test
    fun `ways of reading title`() {
        assertEquals(
            context.getString(R.string.readium_a11y_ways_of_reading_title),
            WaysOfReading().localizedTitle(context)
        )
    }

    @Test
    fun `ways of reading should always be display`() {
        val sut = WaysOfReading(
            visualAdjustments = WaysOfReading.VisualAdjustments.UNKNOWN,
            nonvisualReading = WaysOfReading.NonvisualReading.NO_METADATA,
            nonvisualReadingAltText = false,
            prerecordedAudio = WaysOfReading.PrerecordedAudio.NO_METADATA
        )
        assertTrue(sut.shouldDisplay)
    }

    @Test
    fun `ways of reading statements`() {
        fun test(sut: WaysOfReading, expected: List<AccessibilityDisplayString>) {
            assertEquals(expected, sut.statements.map { (it as StaticStatement).string })
        }

        test(
            WaysOfReading(
                visualAdjustments = WaysOfReading.VisualAdjustments.UNKNOWN,
                nonvisualReading = WaysOfReading.NonvisualReading.NO_METADATA,
                nonvisualReadingAltText = true,
                prerecordedAudio = WaysOfReading.PrerecordedAudio.NO_METADATA
            ),
            listOf(
                S.WAYS_OF_READING_VISUAL_ADJUSTMENTS_UNKNOWN,
                S.WAYS_OF_READING_NONVISUAL_READING_NO_METADATA,
                S.WAYS_OF_READING_NONVISUAL_READING_ALT_TEXT,
                S.WAYS_OF_READING_PRERECORDED_AUDIO_NO_METADATA
            )
        )

        test(
            WaysOfReading(
                visualAdjustments = WaysOfReading.VisualAdjustments.MODIFIABLE,
                nonvisualReading = WaysOfReading.NonvisualReading.READABLE,
                nonvisualReadingAltText = false,
                prerecordedAudio = WaysOfReading.PrerecordedAudio.SYNCHRONIZED
            ),
            listOf(
                S.WAYS_OF_READING_VISUAL_ADJUSTMENTS_MODIFIABLE,
                S.WAYS_OF_READING_NONVISUAL_READING_READABLE,
                S.WAYS_OF_READING_PRERECORDED_AUDIO_SYNCHRONIZED
            )
        )

        test(
            WaysOfReading(
                visualAdjustments = WaysOfReading.VisualAdjustments.UNMODIFIABLE,
                nonvisualReading = WaysOfReading.NonvisualReading.NOT_FULLY,
                nonvisualReadingAltText = false,
                prerecordedAudio = WaysOfReading.PrerecordedAudio.AUDIO_ONLY
            ),
            listOf(
                S.WAYS_OF_READING_VISUAL_ADJUSTMENTS_UNMODIFIABLE,
                S.WAYS_OF_READING_NONVISUAL_READING_NOT_FULLY,
                S.WAYS_OF_READING_PRERECORDED_AUDIO_ONLY
            )
        )

        test(
            WaysOfReading(
                visualAdjustments = WaysOfReading.VisualAdjustments.UNKNOWN,
                nonvisualReading = WaysOfReading.NonvisualReading.UNREADABLE,
                nonvisualReadingAltText = false,
                prerecordedAudio = WaysOfReading.PrerecordedAudio.AUDIO_COMPLEMENTARY
            ),
            listOf(
                S.WAYS_OF_READING_VISUAL_ADJUSTMENTS_UNKNOWN,
                S.WAYS_OF_READING_NONVISUAL_READING_NONE,
                S.WAYS_OF_READING_PRERECORDED_AUDIO_COMPLEMENTARY
            )
        )
    }

    @Test
    fun `navigation initialization`() {
        fun test(a11y: Accessibility?, expected: Navigation) {
            val publication = publication(accessibility = a11y)
            val sut = Navigation(publication)
            assertEquals(expected, sut)
        }

        // No navigation metadata
        test(
            null,
            Navigation(tableOfContents = false, index = false, headings = false, page = false)
        )
        test(
            Accessibility(),
            Navigation(tableOfContents = false, index = false, headings = false, page = false)
        )

        // Individual features
        test(
            Accessibility(features = setOf(Feature.TABLE_OF_CONTENTS)),
            Navigation(tableOfContents = true, index = false, headings = false, page = false)
        )
        test(
            Accessibility(features = setOf(Feature.INDEX)),
            Navigation(tableOfContents = false, index = true, headings = false, page = false)
        )
        test(
            Accessibility(features = setOf(Feature.STRUCTURAL_NAVIGATION)),
            Navigation(tableOfContents = false, index = false, headings = true, page = false)
        )
        test(
            Accessibility(features = setOf(Feature.PAGE_NAVIGATION)),
            Navigation(tableOfContents = false, index = false, headings = false, page = true)
        )

        // All features
        test(
            Accessibility(
                features = setOf(
                    Feature.INDEX,
                    Feature.STRUCTURAL_NAVIGATION,
                    Feature.PAGE_NAVIGATION,
                    Feature.TABLE_OF_CONTENTS
                )
            ),
            Navigation(tableOfContents = true, index = true, headings = true, page = true)
        )
    }

    @Test
    fun `navigation title`() {
        assertEquals("Navigation", Navigation().localizedTitle(context))
    }

    @Test
    fun `navigation should be displayed if there are metadata`() {
        val navigationWithMetadata = Navigation(
            tableOfContents = false,
            index = false,
            headings = false,
            page = false
        )
        assertFalse(navigationWithMetadata.shouldDisplay)

        val navigationWithoutMetadata = Navigation(
            tableOfContents = true,
            index = false,
            headings = false,
            page = false
        )
        assertTrue(navigationWithoutMetadata.shouldDisplay)
    }

    @Test
    fun `navigation statements`() {
        fun test(navigation: Navigation, expected: List<AccessibilityDisplayString>) {
            assertEquals(expected, navigation.statements.map { (it as StaticStatement).string })
        }

        // Test when no features are enabled.
        test(
            Navigation(
                tableOfContents = false,
                index = false,
                headings = false,
                page = false
            ),
            listOf(S.NAVIGATION_NO_METADATA)
        )

        // Test when all features are enabled
        test(
            Navigation(
                tableOfContents = true,
                index = true,
                headings = true,
                page = true
            ),
            listOf(
                S.NAVIGATION_TOC,
                S.NAVIGATION_INDEX,
                S.NAVIGATION_STRUCTURAL,
                S.NAVIGATION_PAGE_NAVIGATION
            )
        )

        // Test individual features
        test(
            Navigation(tableOfContents = true),
            listOf(S.NAVIGATION_TOC)
        )

        test(
            Navigation(index = true),
            listOf(S.NAVIGATION_INDEX)
        )

        test(
            Navigation(headings = true),
            listOf(S.NAVIGATION_STRUCTURAL)
        )

        test(
            Navigation(page = true),
            listOf(S.NAVIGATION_PAGE_NAVIGATION)
        )
    }

    @Test
    fun `rich content initialization`() {
        fun test(a11y: Accessibility?, expected: RichContent) {
            val publication = publication(accessibility = a11y)
            val sut = RichContent(publication)
            assertEquals(expected, sut)
        }

        // No rich content metadata
        test(
            null,
            RichContent(
                extendedAltTextDescriptions = false,
                mathFormula = false,
                mathFormulaAsMathML = false,
                mathFormulaAsLaTeX = false,
                chemicalFormulaAsMathML = false,
                chemicalFormulaAsLaTeX = false,
                closedCaptions = false,
                openCaptions = false,
                transcript = false
            )
        )
        test(
            Accessibility(),
            RichContent(
                extendedAltTextDescriptions = false,
                mathFormula = false,
                mathFormulaAsMathML = false,
                mathFormulaAsLaTeX = false,
                chemicalFormulaAsMathML = false,
                chemicalFormulaAsLaTeX = false,
                closedCaptions = false,
                openCaptions = false,
                transcript = false
            )
        )

        // Individual features
        test(
            Accessibility(features = setOf(Feature.LONG_DESCRIPTION)),
            RichContent(
                extendedAltTextDescriptions = true,
                mathFormula = false,
                mathFormulaAsMathML = false,
                mathFormulaAsLaTeX = false,
                chemicalFormulaAsMathML = false,
                chemicalFormulaAsLaTeX = false,
                closedCaptions = false,
                openCaptions = false,
                transcript = false
            )
        )
        test(
            Accessibility(features = setOf(Feature.DESCRIBED_MATH)),
            RichContent(
                extendedAltTextDescriptions = false,
                mathFormula = true,
                mathFormulaAsMathML = false,
                mathFormulaAsLaTeX = false,
                chemicalFormulaAsMathML = false,
                chemicalFormulaAsLaTeX = false,
                closedCaptions = false,
                openCaptions = false,
                transcript = false
            )
        )
        test(
            Accessibility(features = setOf(Feature.MATHML)),
            RichContent(
                extendedAltTextDescriptions = false,
                mathFormula = false,
                mathFormulaAsMathML = true,
                mathFormulaAsLaTeX = false,
                chemicalFormulaAsMathML = false,
                chemicalFormulaAsLaTeX = false,
                closedCaptions = false,
                openCaptions = false,
                transcript = false
            )
        )
        test(
            Accessibility(features = setOf(Feature.LATEX)),
            RichContent(
                extendedAltTextDescriptions = false,
                mathFormula = false,
                mathFormulaAsMathML = false,
                mathFormulaAsLaTeX = true,
                chemicalFormulaAsMathML = false,
                chemicalFormulaAsLaTeX = false,
                closedCaptions = false,
                openCaptions = false,
                transcript = false
            )
        )
        test(
            Accessibility(features = setOf(Feature.MATHML_CHEMISTRY)),
            RichContent(
                extendedAltTextDescriptions = false,
                mathFormula = false,
                mathFormulaAsMathML = false,
                mathFormulaAsLaTeX = false,
                chemicalFormulaAsMathML = true,
                chemicalFormulaAsLaTeX = false,
                closedCaptions = false,
                openCaptions = false,
                transcript = false
            )
        )
        test(
            Accessibility(features = setOf(Feature.LATEX_CHEMISTRY)),
            RichContent(
                extendedAltTextDescriptions = false,
                mathFormula = false,
                mathFormulaAsMathML = false,
                mathFormulaAsLaTeX = false,
                chemicalFormulaAsMathML = false,
                chemicalFormulaAsLaTeX = true,
                closedCaptions = false,
                openCaptions = false,
                transcript = false
            )
        )
        test(
            Accessibility(features = setOf(Feature.CLOSED_CAPTIONS)),
            RichContent(
                extendedAltTextDescriptions = false,
                mathFormula = false,
                mathFormulaAsMathML = false,
                mathFormulaAsLaTeX = false,
                chemicalFormulaAsMathML = false,
                chemicalFormulaAsLaTeX = false,
                closedCaptions = true,
                openCaptions = false,
                transcript = false
            )
        )
        test(
            Accessibility(features = setOf(Feature.OPEN_CAPTIONS)),
            RichContent(
                extendedAltTextDescriptions = false,
                mathFormula = false,
                mathFormulaAsMathML = false,
                mathFormulaAsLaTeX = false,
                chemicalFormulaAsMathML = false,
                chemicalFormulaAsLaTeX = false,
                closedCaptions = false,
                openCaptions = true,
                transcript = false
            )
        )
        test(
            Accessibility(features = setOf(Feature.TRANSCRIPT)),
            RichContent(
                extendedAltTextDescriptions = false,
                mathFormula = false,
                mathFormulaAsMathML = false,
                mathFormulaAsLaTeX = false,
                chemicalFormulaAsMathML = false,
                chemicalFormulaAsLaTeX = false,
                closedCaptions = false,
                openCaptions = false,
                transcript = true
            )
        )

        // All features
        test(
            Accessibility(
                features = setOf(
                    Feature.LONG_DESCRIPTION, Feature.DESCRIBED_MATH, Feature.MATHML, Feature.LATEX,
                    Feature.MATHML_CHEMISTRY, Feature.LATEX_CHEMISTRY, Feature.CLOSED_CAPTIONS,
                    Feature.OPEN_CAPTIONS, Feature.TRANSCRIPT
                )
            ),
            RichContent(
                extendedAltTextDescriptions = true,
                mathFormula = true,
                mathFormulaAsMathML = true,
                mathFormulaAsLaTeX = true,
                chemicalFormulaAsMathML = true,
                chemicalFormulaAsLaTeX = true,
                closedCaptions = true,
                openCaptions = true,
                transcript = true
            )
        )
    }

    @Test
    fun `rich content title`() {
        assertEquals("Rich content", RichContent().localizedTitle(context))
    }

    @Test
    fun `rich content should be displayed if there are some metadata`() {
        val richContentWithMetadata = RichContent(
            extendedAltTextDescriptions = true,
            mathFormula = false,
            mathFormulaAsMathML = false,
            mathFormulaAsLaTeX = false,
            chemicalFormulaAsMathML = false,
            chemicalFormulaAsLaTeX = false,
            closedCaptions = false,
            openCaptions = false,
            transcript = false
        )
        assertTrue(richContentWithMetadata.shouldDisplay)

        val richContentWithoutMetadata = RichContent(
            extendedAltTextDescriptions = false,
            mathFormula = false,
            mathFormulaAsMathML = false,
            mathFormulaAsLaTeX = false,
            chemicalFormulaAsMathML = false,
            chemicalFormulaAsLaTeX = false,
            closedCaptions = false,
            openCaptions = false,
            transcript = false
        )
        assertFalse(richContentWithoutMetadata.shouldDisplay)
    }

    @Test
    fun `rich content statements`() {
        fun test(
            richContent: RichContent,
            expected: List<AccessibilityDisplayString>,
        ) {
            assertEquals(
                expected,
                richContent.statements.map { (it as StaticStatement).string }
            )
        }

        // Test when there are no rich content.
        test(
            RichContent(
                extendedAltTextDescriptions = false,
                mathFormula = false,
                mathFormulaAsMathML = false,
                mathFormulaAsLaTeX = false,
                chemicalFormulaAsMathML = false,
                chemicalFormulaAsLaTeX = false,
                closedCaptions = false,
                openCaptions = false,
                transcript = false
            ),
            listOf(S.RICH_CONTENT_UNKNOWN)
        )

        // Test when all features are enabled
        test(
            RichContent(
                extendedAltTextDescriptions = true,
                mathFormula = true,
                mathFormulaAsMathML = true,
                mathFormulaAsLaTeX = true,
                chemicalFormulaAsMathML = true,
                chemicalFormulaAsLaTeX = true,
                closedCaptions = true,
                openCaptions = true,
                transcript = true
            ),
            listOf(
                S.RICH_CONTENT_EXTENDED,
                S.RICH_CONTENT_ACCESSIBLE_MATH_DESCRIBED,
                S.RICH_CONTENT_ACCESSIBLE_MATH_AS_MATHML,
                S.RICH_CONTENT_ACCESSIBLE_MATH_AS_LATEX,
                S.RICH_CONTENT_ACCESSIBLE_CHEMISTRY_AS_MATHML,
                S.RICH_CONTENT_ACCESSIBLE_CHEMISTRY_AS_LATEX,
                S.RICH_CONTENT_CLOSED_CAPTIONS,
                S.RICH_CONTENT_OPEN_CAPTIONS,
                S.RICH_CONTENT_TRANSCRIPT
            )
        )

        // Test individual features
        test(
            RichContent(extendedAltTextDescriptions = true),
            listOf(S.RICH_CONTENT_EXTENDED)
        )

        test(
            RichContent(mathFormula = true),
            listOf(S.RICH_CONTENT_ACCESSIBLE_MATH_DESCRIBED)
        )

        test(
            RichContent(mathFormulaAsMathML = true),
            listOf(S.RICH_CONTENT_ACCESSIBLE_MATH_AS_MATHML)
        )

        test(
            RichContent(mathFormulaAsLaTeX = true),
            listOf(S.RICH_CONTENT_ACCESSIBLE_MATH_AS_LATEX)
        )

        test(
            RichContent(chemicalFormulaAsMathML = true),
            listOf(S.RICH_CONTENT_ACCESSIBLE_CHEMISTRY_AS_MATHML)
        )

        test(
            RichContent(chemicalFormulaAsLaTeX = true),
            listOf(S.RICH_CONTENT_ACCESSIBLE_CHEMISTRY_AS_LATEX)
        )

        test(
            RichContent(closedCaptions = true),
            listOf(S.RICH_CONTENT_CLOSED_CAPTIONS)
        )

        test(
            RichContent(openCaptions = true),
            listOf(S.RICH_CONTENT_OPEN_CAPTIONS)
        )

        test(
            RichContent(transcript = true),
            listOf(S.RICH_CONTENT_TRANSCRIPT)
        )
    }

    @Test
    fun `additional information initialization`() {
        fun test(a11y: Accessibility?, expected: AdditionalInformation) {
            val publication = publication(accessibility = a11y)
            val sut = AdditionalInformation(publication)
            assertEquals(expected, sut)
        }

        // No additional information metadata
        test(null, AdditionalInformation())
        test(Accessibility(), AdditionalInformation())

        // Individual features
        test(
            Accessibility(features = setOf(Feature.PAGE_BREAK_MARKERS)),
            AdditionalInformation(pageBreakMarkers = true)
        )
        test(
            Accessibility(features = setOf(Feature.PRINT_PAGE_NUMBERS)),
            AdditionalInformation(pageBreakMarkers = true)
        )
        test(Accessibility(features = setOf(Feature.ARIA)), AdditionalInformation(aria = true))
        test(
            Accessibility(features = setOf(Feature.AUDIO_DESCRIPTION)),
            AdditionalInformation(audioDescriptions = true)
        )
        test(
            Accessibility(features = setOf(Feature.BRAILLE)),
            AdditionalInformation(braille = true)
        )
        test(
            Accessibility(features = setOf(Feature.RUBY_ANNOTATIONS)),
            AdditionalInformation(rubyAnnotations = true)
        )
        test(
            Accessibility(features = setOf(Feature.FULL_RUBY_ANNOTATIONS)),
            AdditionalInformation(fullRubyAnnotations = true)
        )
        test(
            Accessibility(features = setOf(Feature.HIGH_CONTRAST_AUDIO)),
            AdditionalInformation(highAudioContrast = true)
        )
        test(
            Accessibility(features = setOf(Feature.HIGH_CONTRAST_DISPLAY)),
            AdditionalInformation(highDisplayContrast = true)
        )
        test(
            Accessibility(features = setOf(Feature.LARGE_PRINT)),
            AdditionalInformation(largePrint = true)
        )
        test(
            Accessibility(features = setOf(Feature.SIGN_LANGUAGE)),
            AdditionalInformation(signLanguage = true)
        )
        test(
            Accessibility(features = setOf(Feature.TACTILE_GRAPHIC)),
            AdditionalInformation(tactileGraphics = true)
        )
        test(
            Accessibility(features = setOf(Feature.TACTILE_OBJECT)),
            AdditionalInformation(tactileObjects = true)
        )
        test(
            Accessibility(features = setOf(Feature.TTS_MARKUP)),
            AdditionalInformation(textToSpeechHinting = true)
        )

        // All features
        test(
            Accessibility(
                features = setOf(
                    Feature.PAGE_BREAK_MARKERS,
                    Feature.ARIA,
                    Feature.AUDIO_DESCRIPTION,
                    Feature.BRAILLE,
                    Feature.RUBY_ANNOTATIONS,
                    Feature.FULL_RUBY_ANNOTATIONS,
                    Feature.HIGH_CONTRAST_AUDIO,
                    Feature.HIGH_CONTRAST_DISPLAY,
                    Feature.LARGE_PRINT,
                    Feature.SIGN_LANGUAGE,
                    Feature.TACTILE_GRAPHIC,
                    Feature.TACTILE_OBJECT,
                    Feature.TTS_MARKUP
                )
            ),
            AdditionalInformation(
                pageBreakMarkers = true, aria = true, audioDescriptions = true, braille = true,
                rubyAnnotations = true, fullRubyAnnotations = true, highAudioContrast = true,
                highDisplayContrast = true, largePrint = true, signLanguage = true,
                tactileGraphics = true, tactileObjects = true, textToSpeechHinting = true
            )
        )
    }

    @Test
    fun `additional information title`() {
        assertEquals(
            "Additional accessibility information",
            AdditionalInformation().localizedTitle(context)
        )
    }

    @Test
    fun `additional information should be display if there are some metadata`() {
        val additionalInfoWithMetadata = AdditionalInformation(pageBreakMarkers = true)
        assertTrue(additionalInfoWithMetadata.shouldDisplay)

        val additionalInfoWithoutMetadata = AdditionalInformation()
        assertFalse(additionalInfoWithoutMetadata.shouldDisplay)
    }

    @Test
    fun `additional information statements`() {
        fun test(
            additionalInfo: AdditionalInformation,
            expected: List<AccessibilityDisplayString>,
        ) {
            assertEquals(expected, additionalInfo.statements.map { (it as StaticStatement).string })
        }

        // Test when there are no metadata
        test(AdditionalInformation(), emptyList())

        // Test when all features are enabled
        test(
            AdditionalInformation(
                pageBreakMarkers = true, aria = true, audioDescriptions = true, braille = true,
                rubyAnnotations = true, fullRubyAnnotations = true, highAudioContrast = true,
                highDisplayContrast = true, largePrint = true, signLanguage = true,
                tactileGraphics = true, tactileObjects = true, textToSpeechHinting = true
            ),
            listOf(
                S.ADDITIONAL_ACCESSIBILITY_INFORMATION_PAGE_BREAKS,
                S.ADDITIONAL_ACCESSIBILITY_INFORMATION_ARIA,
                S.ADDITIONAL_ACCESSIBILITY_INFORMATION_AUDIO_DESCRIPTIONS,
                S.ADDITIONAL_ACCESSIBILITY_INFORMATION_BRAILLE,
                S.ADDITIONAL_ACCESSIBILITY_INFORMATION_RUBY_ANNOTATIONS,
                S.ADDITIONAL_ACCESSIBILITY_INFORMATION_FULL_RUBY_ANNOTATIONS,
                S.ADDITIONAL_ACCESSIBILITY_INFORMATION_HIGH_CONTRAST_BETWEEN_FOREGROUND_AND_BACKGROUND_AUDIO,
                S.ADDITIONAL_ACCESSIBILITY_INFORMATION_HIGH_CONTRAST_BETWEEN_TEXT_AND_BACKGROUND,
                S.ADDITIONAL_ACCESSIBILITY_INFORMATION_LARGE_PRINT,
                S.ADDITIONAL_ACCESSIBILITY_INFORMATION_SIGN_LANGUAGE,
                S.ADDITIONAL_ACCESSIBILITY_INFORMATION_TACTILE_GRAPHICS,
                S.ADDITIONAL_ACCESSIBILITY_INFORMATION_TACTILE_OBJECTS,
                S.ADDITIONAL_ACCESSIBILITY_INFORMATION_TEXT_TO_SPEECH_HINTING
            )
        )

        // Test individual features
        test(
            AdditionalInformation(pageBreakMarkers = true),
            listOf(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_PAGE_BREAKS)
        )
        test(
            AdditionalInformation(aria = true),
            listOf(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_ARIA)
        )
        test(
            AdditionalInformation(audioDescriptions = true),
            listOf(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_AUDIO_DESCRIPTIONS)
        )
        test(
            AdditionalInformation(braille = true),
            listOf(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_BRAILLE)
        )
        test(
            AdditionalInformation(rubyAnnotations = true),
            listOf(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_RUBY_ANNOTATIONS)
        )
        test(
            AdditionalInformation(fullRubyAnnotations = true),
            listOf(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_FULL_RUBY_ANNOTATIONS)
        )
        test(
            AdditionalInformation(highAudioContrast = true),
            listOf(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_HIGH_CONTRAST_BETWEEN_FOREGROUND_AND_BACKGROUND_AUDIO)
        )
        test(
            AdditionalInformation(highDisplayContrast = true),
            listOf(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_HIGH_CONTRAST_BETWEEN_TEXT_AND_BACKGROUND)
        )
        test(
            AdditionalInformation(largePrint = true),
            listOf(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_LARGE_PRINT)
        )
        test(
            AdditionalInformation(signLanguage = true),
            listOf(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_SIGN_LANGUAGE)
        )
        test(
            AdditionalInformation(tactileGraphics = true),
            listOf(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_TACTILE_GRAPHICS)
        )
        test(
            AdditionalInformation(tactileObjects = true),
            listOf(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_TACTILE_OBJECTS)
        )
        test(
            AdditionalInformation(textToSpeechHinting = true),
            listOf(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_TEXT_TO_SPEECH_HINTING)
        )
    }

    @Test
    fun `hazards initialization`() {
        fun test(a11y: Accessibility?, expected: Hazards) {
            val publication = publication(accessibility = a11y)
            val sut = Hazards(publication)
            assertEquals(expected, sut)
        }

        // No hazards metadata
        test(
            null,
            Hazards(
                flashing = Hazard.NO_METADATA,
                motion = Hazard.NO_METADATA,
                sound = Hazard.NO_METADATA
            )
        )
        test(
            Accessibility(),
            Hazards(
                flashing = Hazard.NO_METADATA,
                motion = Hazard.NO_METADATA,
                sound = Hazard.NO_METADATA
            )
        )

        // Declared no hazards
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.NONE)),
            Hazards(flashing = Hazard.NO, motion = Hazard.NO, sound = Hazard.NO)
        )
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.NONE, Accessibility.Hazard.FLASHING)),
            Hazards(flashing = Hazard.YES, motion = Hazard.NO, sound = Hazard.NO)
        )
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.NONE, Accessibility.Hazard.MOTION_SIMULATION)),
            Hazards(flashing = Hazard.NO, motion = Hazard.YES, sound = Hazard.NO)
        )
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.NONE, Accessibility.Hazard.UNKNOWN_SOUND_HAZARD)),
            Hazards(
                flashing = Hazard.NO,
                motion = Hazard.NO,
                sound = Hazard.UNKNOWN
            )
        )

        // Declared unknown hazards
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.UNKNOWN)),
            Hazards(
                flashing = Hazard.UNKNOWN,
                motion = Hazard.UNKNOWN,
                sound = Hazard.UNKNOWN
            )
        )
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.UNKNOWN, Accessibility.Hazard.FLASHING)),
            Hazards(
                flashing = Hazard.YES,
                motion = Hazard.UNKNOWN,
                sound = Hazard.UNKNOWN
            )
        )
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.UNKNOWN, Accessibility.Hazard.MOTION_SIMULATION)),
            Hazards(
                flashing = Hazard.UNKNOWN,
                motion = Hazard.YES,
                sound = Hazard.UNKNOWN
            )
        )
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.UNKNOWN, Accessibility.Hazard.NO_SOUND_HAZARD)),
            Hazards(
                flashing = Hazard.UNKNOWN,
                motion = Hazard.UNKNOWN,
                sound = Hazard.NO
            )
        )

        // Flashing
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.FLASHING)),
            Hazards(
                flashing = Hazard.YES,
                motion = Hazard.NO_METADATA,
                sound = Hazard.NO_METADATA
            )
        )
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.NO_FLASHING_HAZARD)),
            Hazards(
                flashing = Hazard.NO,
                motion = Hazard.NO_METADATA,
                sound = Hazard.NO_METADATA
            )
        )
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.UNKNOWN_FLASHING_HAZARD)),
            Hazards(
                flashing = Hazard.UNKNOWN,
                motion = Hazard.NO_METADATA,
                sound = Hazard.NO_METADATA
            )
        )

        // Motion
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.MOTION_SIMULATION)),
            Hazards(
                flashing = Hazard.NO_METADATA,
                motion = Hazard.YES,
                sound = Hazard.NO_METADATA
            )
        )
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.NO_MOTION_SIMULATION_HAZARD)),
            Hazards(
                flashing = Hazard.NO_METADATA,
                motion = Hazard.NO,
                sound = Hazard.NO_METADATA
            )
        )
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.UNKNOWN_MOTION_SIMULATION_HAZARD)),
            Hazards(
                flashing = Hazard.NO_METADATA,
                motion = Hazard.UNKNOWN,
                sound = Hazard.NO_METADATA
            )
        )

        // Sound
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.SOUND)),
            Hazards(
                flashing = Hazard.NO_METADATA,
                motion = Hazard.NO_METADATA,
                sound = Hazard.YES
            )
        )
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.NO_SOUND_HAZARD)),
            Hazards(
                flashing = Hazard.NO_METADATA,
                motion = Hazard.NO_METADATA,
                sound = Hazard.NO
            )
        )
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.UNKNOWN_SOUND_HAZARD)),
            Hazards(
                flashing = Hazard.NO_METADATA,
                motion = Hazard.NO_METADATA,
                sound = Hazard.UNKNOWN
            )
        )

        // Combination of hazards
        test(
            Accessibility(hazards = setOf(Accessibility.Hazard.FLASHING, Accessibility.Hazard.NO_SOUND_HAZARD)),
            Hazards(
                flashing = Hazard.YES,
                motion = Hazard.NO_METADATA,
                sound = Hazard.NO
            )
        )
        test(
            Accessibility(
                hazards = setOf(
                    Accessibility.Hazard.UNKNOWN_FLASHING_HAZARD,
                    Accessibility.Hazard.NO_SOUND_HAZARD,
                    Accessibility.Hazard.MOTION_SIMULATION
                )
            ),
            Hazards(
                flashing = Hazard.UNKNOWN,
                motion = Hazard.YES,
                sound = Hazard.NO
            )
        )
    }

    @Test
    fun `hazards title`() {
        assertEquals("Hazards", Hazards().localizedTitle(context))
    }

    @Test
    fun `hazards should be displayed if metadata is provided`() {
        var hazardsWithMetadata = Hazards(
            flashing = Hazard.YES,
            motion = Hazard.NO_METADATA,
            sound = Hazard.NO_METADATA
        )
        assertTrue(hazardsWithMetadata.shouldDisplay)

        hazardsWithMetadata = Hazards(
            flashing = Hazard.NO_METADATA,
            motion = Hazard.UNKNOWN,
            sound = Hazard.NO_METADATA
        )
        assertTrue(hazardsWithMetadata.shouldDisplay)

        val hazardsWithoutMetadata = Hazards(
            flashing = Hazard.NO_METADATA,
            motion = Hazard.NO_METADATA,
            sound = Hazard.NO_METADATA
        )
        assertFalse(hazardsWithoutMetadata.shouldDisplay)
    }

    @Test
    fun `hazards statements`() {
        fun test(hazards: Hazards, expected: List<AccessibilityDisplayString>) {
            assertEquals(expected, hazards.statements.map { (it as StaticStatement).string })
        }

        // Test when no metadata is provided
        test(
            Hazards(
                flashing = Hazard.NO_METADATA,
                motion = Hazard.NO_METADATA,
                sound = Hazard.NO_METADATA
            ),
            listOf(S.HAZARDS_NO_METADATA)
        )

        // Test when no hazards are present
        test(
            Hazards(
                flashing = Hazard.NO,
                motion = Hazard.NO,
                sound = Hazard.NO
            ),
            listOf(S.HAZARDS_NONE)
        )

        // Test when hazards are unknown
        test(
            Hazards(
                flashing = Hazard.UNKNOWN,
                motion = Hazard.UNKNOWN,
                sound = Hazard.UNKNOWN
            ),
            listOf(S.HAZARDS_UNKNOWN)
        )

        // Test individual hazards
        test(
            Hazards(
                flashing = Hazard.YES,
                motion = Hazard.NO,
                sound = Hazard.NO
            ),
            listOf(
                S.HAZARDS_FLASHING,
                S.HAZARDS_MOTION_NONE,
                S.HAZARDS_SOUND_NONE
            )
        )

        test(
            Hazards(
                flashing = Hazard.NO,
                motion = Hazard.YES,
                sound = Hazard.NO
            ),
            listOf(
                S.HAZARDS_MOTION,
                S.HAZARDS_FLASHING_NONE,
                S.HAZARDS_SOUND_NONE
            )
        )

        test(
            Hazards(
                flashing = Hazard.NO,
                motion = Hazard.NO,
                sound = Hazard.YES
            ),
            listOf(
                S.HAZARDS_SOUND,
                S.HAZARDS_FLASHING_NONE,
                S.HAZARDS_MOTION_NONE
            )
        )

        // Test combinations of hazards
        test(
            Hazards(
                flashing = Hazard.YES,
                motion = Hazard.YES,
                sound = Hazard.YES
            ),
            listOf(S.HAZARDS_FLASHING, S.HAZARDS_MOTION, S.HAZARDS_SOUND)
        )

        test(
            Hazards(
                flashing = Hazard.UNKNOWN,
                motion = Hazard.YES,
                sound = Hazard.NO
            ),
            listOf(
                S.HAZARDS_MOTION,
                S.HAZARDS_FLASHING_UNKNOWN,
                S.HAZARDS_SOUND_NONE
            )
        )

        test(
            Hazards(
                flashing = Hazard.YES,
                motion = Hazard.UNKNOWN,
                sound = Hazard.UNKNOWN
            ),
            listOf(
                S.HAZARDS_FLASHING,
                S.HAZARDS_MOTION_UNKNOWN,
                S.HAZARDS_SOUND_UNKNOWN
            )
        )
    }

    @Test
    fun `conformance initialization`() {
        fun test(a11y: Accessibility?, expected: Conformance) {
            val publication = publication(accessibility = a11y)
            val sut = Conformance(publication)
            assertEquals(expected, sut)
        }

        // No metadata or profile
        test(null, Conformance(profiles = emptySet()))
        test(Accessibility(conformsTo = emptySet()), Conformance(profiles = emptySet()))

        // One profile
        test(Accessibility(conformsTo = setOf(Profile.EPUB_A11Y_10_WCAG_20_A)), Conformance(profiles = setOf(Profile.EPUB_A11Y_10_WCAG_20_A)))

        // Multiple profiles
        test(
            Accessibility(conformsTo = setOf(Profile.EPUB_A11Y_10_WCAG_20_A, Profile.EPUB_A11Y_11_WCAG_20_A)),
            Conformance(profiles = setOf(Profile.EPUB_A11Y_10_WCAG_20_A, Profile.EPUB_A11Y_11_WCAG_20_A))
        )
    }

    @Test
    fun `conformance title`() {
        assertEquals("Conformance", Conformance().localizedTitle(context))
    }

    @Test
    fun `conformance should always be displayed`() {
        assertTrue(Conformance(profiles = emptySet()).shouldDisplay)
        assertTrue(Conformance(profiles = setOf(Profile.EPUB_A11Y_10_WCAG_20_A)).shouldDisplay)
    }

    @Test
    fun `conformance statements`() {
        fun test(profiles: Set<Profile>, expected: AccessibilityDisplayString) {
            assertEquals(
                listOf(expected),
                Conformance(profiles = profiles).statements.map { (it as StaticStatement).string }
            )
        }

        // Test no profile
        test(emptySet(), expected = S.CONFORMANCE_NO)

        // Test unknown profile
        test(setOf(Profile("https://custom-profile")), expected = S.CONFORMANCE_UNKNOWN_STANDARD)

        // Test level A profiles
        test(setOf(Profile.EPUB_A11Y_10_WCAG_20_A), expected = S.CONFORMANCE_A)
        test(setOf(Profile.EPUB_A11Y_11_WCAG_20_A), expected = S.CONFORMANCE_A)
        test(setOf(Profile.EPUB_A11Y_11_WCAG_21_A), expected = S.CONFORMANCE_A)
        test(setOf(Profile.EPUB_A11Y_11_WCAG_22_A), expected = S.CONFORMANCE_A)

        // Test level AA profiles
        test(setOf(Profile.EPUB_A11Y_10_WCAG_20_AA), expected = S.CONFORMANCE_AA)
        test(setOf(Profile.EPUB_A11Y_11_WCAG_20_AA), expected = S.CONFORMANCE_AA)
        test(setOf(Profile.EPUB_A11Y_11_WCAG_21_AA), expected = S.CONFORMANCE_AA)
        test(setOf(Profile.EPUB_A11Y_11_WCAG_22_AA), expected = S.CONFORMANCE_AA)

        // Test level AAA profiles
        test(setOf(Profile.EPUB_A11Y_10_WCAG_20_AAA), expected = S.CONFORMANCE_AAA)
        test(setOf(Profile.EPUB_A11Y_11_WCAG_20_AAA), expected = S.CONFORMANCE_AAA)
        test(setOf(Profile.EPUB_A11Y_11_WCAG_21_AAA), expected = S.CONFORMANCE_AAA)
        test(setOf(Profile.EPUB_A11Y_11_WCAG_22_AAA), expected = S.CONFORMANCE_AAA)

        // Test multiple profiles
        test(setOf(Profile.EPUB_A11Y_10_WCAG_20_A, Profile.EPUB_A11Y_10_WCAG_20_AA, Profile.EPUB_A11Y_10_WCAG_20_AAA), expected = S.CONFORMANCE_AAA)
        test(setOf(Profile.EPUB_A11Y_10_WCAG_20_A, Profile.EPUB_A11Y_10_WCAG_20_AA), expected = S.CONFORMANCE_AA)
    }

    @Test
    fun `legal initialization`() {
        fun test(a11y: Accessibility?, expected: Legal) {
            val publication = publication(accessibility = a11y)
            val sut = Legal(publication)
            assertEquals(expected, sut)
        }

        // No metadata or exemptions
        test(null, Legal(exemption = false))
        test(Accessibility(exemptions = emptySet()), Legal(exemption = false))

        // Exemptions
        test(Accessibility(exemptions = setOf(Exemption.EAA_DISPROPORTIONATE_BURDEN)), Legal(exemption = true))
        test(Accessibility(exemptions = setOf(Exemption.EAA_FUNDAMENTAL_ALTERATION)), Legal(exemption = true))
        test(Accessibility(exemptions = setOf(Exemption.EAA_MICROENTERPRISE)), Legal(exemption = true))
        test(Accessibility(exemptions = setOf(Exemption.EAA_MICROENTERPRISE, Exemption.EAA_FUNDAMENTAL_ALTERATION)), Legal(exemption = true))
    }

    @Test
    fun `legal title`() {
        assertEquals("Legal considerations", Legal().localizedTitle(context))
    }

    @Test
    fun `legal should be displayed if there is an exemption`() {
        assertTrue(Legal(exemption = true).shouldDisplay)
        assertFalse(Legal(exemption = false).shouldDisplay)
    }

    @Test
    fun `legal statements`() {
        // Test when noMetadata is true
        assertEquals(
            listOf(S.LEGAL_CONSIDERATIONS_NO_METADATA),
            Legal(exemption = false).statements.map { (it as StaticStatement).string }
        )

        // Test when exemption is claimed
        assertEquals(
            listOf(S.LEGAL_CONSIDERATIONS_EXEMPT),
            Legal(exemption = true).statements.map { (it as StaticStatement).string }
        )
    }

    @Test
    fun `accessibility summary initialization`() {
        fun test(a11y: Accessibility?, expected: AccessibilitySummary) {
            val publication = publication(accessibility = a11y)
            val sut = AccessibilitySummary(publication)
            assertEquals(expected, sut)
        }

        test(null, AccessibilitySummary(summary = null))
        test(Accessibility(summary = null), AccessibilitySummary(summary = null))
        test(Accessibility(summary = "A summary"), AccessibilitySummary(summary = "A summary"))
    }

    @Test
    fun `accessibility summary title`() {
        assertEquals("Accessibility summary", AccessibilitySummary().localizedTitle(context))
    }

    @Test
    fun `accessibility summary should be displayed if there is a summary`() {
        val summaryWithContent = AccessibilitySummary(summary = "This is a summary.")
        assertTrue(summaryWithContent.shouldDisplay)

        val summaryWithoutContent = AccessibilitySummary(summary = null)
        assertFalse(summaryWithoutContent.shouldDisplay)
    }

    @Test
    fun `accessibility summary statements`() {
        // Test when summary is nil
        assertEquals(
            listOf(S.ACCESSIBILITY_SUMMARY_NO_METADATA),
            AccessibilitySummary(summary = null).statements.map { (it as StaticStatement).string }
        )

        // Test when summary is provided
        val summaryText = "This publication is accessible and includes features such as text-to-speech and high contrast."
        val fields = AccessibilitySummary(summary = summaryText).statements
        assertEquals(1, fields.size)
        val field = fields[0]
        assertEquals(summaryText, field.localizedString(context, descriptive = false))
        assertEquals(summaryText, field.localizedString(context, descriptive = true))
    }

    @OptIn(InternalReadiumApi::class)
    private fun publication(
        layout: EpubLayout? = null,
        accessibility: Accessibility?,
    ): Publication =
        Publication(
            manifest = Manifest(
                metadata = Metadata(
                    accessibility = accessibility,
                    otherMetadata = mapOf(
                        "presentation" to Presentation(
                            layout = layout
                        ).toJSON().toMap()
                    )
                )
            )
        )
}
