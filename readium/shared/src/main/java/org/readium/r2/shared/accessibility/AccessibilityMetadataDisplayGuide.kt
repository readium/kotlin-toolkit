/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.accessibility

import android.content.Context
import androidx.annotation.StringRes
import org.readium.r2.shared.R
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.Statement
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.StaticStatement
import org.readium.r2.shared.extensions.contains
import org.readium.r2.shared.extensions.containsAny
import org.readium.r2.shared.publication.Accessibility
import org.readium.r2.shared.publication.Accessibility.AccessMode
import org.readium.r2.shared.publication.Accessibility.Feature
import org.readium.r2.shared.publication.Accessibility.PrimaryAccessMode
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation

/**
 * When presenting accessibility metadata provided by the publisher, it is
 * suggested that the section is introduced using terms such as "claims" or
 * "declarations" (e.g., "Accessibility Claims").
 *
 * @param waysOfReading The ways of reading display field is a banner heading
 * that groups together the following information about how the content
 * facilitates access.
 * @param navigation Identifies the navigation features included in the publication.
 * @param richContent Indicates the presence of math, chemical formulas,
 * extended descriptions for information rich images, e.g., charts, diagrams,
 * figures, graphs, and whether these are in an accessible format or available
 * in an alternative form, e.g., whether math and chemical formulas are
 * navigable with assistive technologies, or whether extended descriptions are
 * available for information-rich images. In addition, it indicates the presence
 * of videos and if closed captions, open captions, or transcripts for
 * prerecorded audio are available.
 * @param additionalInformation This section lists additional metadata
 * categories that can help users better understand the accessibility
 * characteristics of digital publications. These are for metadata that do not
 * fit into the other categories or are rarely used in trade publishing.
 * @param hazards Identifies any potential hazards (e.g., flashing elements,
 * sounds, and motion simulation) that could afflict physiologically sensitive
 * users.
 * @param conformance Identifies whether the digital publication claims to meet
 * internationally recognized conformance standards for accessibility.
 * @param legal In some jurisdictions publishers may be able to claim an
 * exemption from the provision of accessible publications, including the
 * provision of accessibility metadata. This should always be subject to
 * clarification by legal counsel for each jurisdiction.
 * @param accessibilitySummary The accessibility summary was intended (in EPUB
 * Accessibility 1.0) to describe in human-readable prose the accessibility
 * features present in the publication as well as any shortcomings. Starting
 * with EPUB Accessibility version 1.1 the accessibility summary became a human-
 * readable summary of the accessibility that complements, but does not
 * duplicate, the other discoverability metadata.
 */
public class AccessibilityMetadataDisplayGuide(
    public val waysOfReading: WaysOfReading,
    public val navigation: Navigation,
    public val richContent: RichContent,
    public val additionalInformation: AdditionalInformation,
    public val hazards: Hazards,
    public val conformance: Conformance,
    public val legal: Legal,
    public val accessibilitySummary: AccessibilitySummary,
) {

    /**
     * Creates a new display guide for the given `publication` metadata.
     */
    public constructor(publication: Publication) : this(
        waysOfReading = WaysOfReading(publication),
        navigation = Navigation(publication),
        richContent = RichContent(publication),
        additionalInformation = AdditionalInformation(publication),
        hazards = Hazards(publication),
        conformance = Conformance(publication),
        legal = Legal(publication),
        accessibilitySummary = AccessibilitySummary(publication),
    )

    /**
     * Returns the list of display fields in their recommended order.
     */
    public val fields: List<Field> =
        listOf(
            waysOfReading,
            navigation,
            richContent,
            additionalInformation,
            hazards,
            conformance,
            legal,
            accessibilitySummary,
        )

    /**
     * Represents a collection of related accessibility claims which should be
     * displayed together in a section.
     */
    public sealed interface Field {

        /**
         * Indicates whether this display field should be rendered in the user
         * interface, because it contains useful information.
         *
         * A field with `shouldDisplay` set to `false` might have for only statement
         * "No information is available".
         */
        public val shouldDisplay: Boolean

        /**
         * Localized title for this display field, for example to use as a section
         * header.
         */
        public fun localizedTitle(context: Context): String

        /**
         * List of accessibility claims to display for this field.
         */
        public val statements: List<Statement>
    }

    /**
     * The ways of reading display field is a banner heading that groups
     * together the following information about how the content facilitates
     * access.
     *
     * https://w3c.github.io/publ-a11y/a11y-meta-display-guide/2.0/guidelines/#ways-of-reading
     *
     * @param visualAdjustments Indicates if users can modify the appearance of
     * the text and the page layout according to the possibilities offered by
     * the reading system.
     * @parma nonvisualReading Indicates whether all content required for
     * comprehension can be consumed in text and therefore is available to
     * reading systems with read aloud speech or dynamic braille capabilities.
     * @param nonvisualReadingAltText Indicates whether text alternatives are
     * provided for visuals.
     * @param prerecordedAudio Indicates the presence of prerecorded audio and
     * specifies if this audio is standalone (an audiobook), is an alternative
     * to the text (synchronized text with audio playback), or is complementary
     * audio (portions of audio, (e.g., reading of a poem).
     */
    public data class WaysOfReading(
        public val visualAdjustments: VisualAdjustments = VisualAdjustments.UNKNOWN,
        public val nonvisualReading: NonvisualReading = NonvisualReading.NO_METADATA,
        public val nonvisualReadingAltText: Boolean = false,
        public val prerecordedAudio: PrerecordedAudio = PrerecordedAudio.NO_METADATA,
    ) : Field {

        /** "Ways of reading" should be rendered even if there is no metadata. */
        override val shouldDisplay: Boolean = true

        override fun localizedTitle(context: Context): String =
            context.getString(R.string.readium_a11y_ways_of_reading_title)

        public enum class VisualAdjustments {
            /** Appearance can be modified */
            MODIFIABLE,

            /** Appearance cannot be modified */
            UNMODIFIABLE,

            /** No information about appearance modifiability is available */
            UNKNOWN,
        }

        public enum class NonvisualReading {
            /** Readable in read aloud or dynamic braille */
            READABLE,

            /** Not fully readable in read aloud or dynamic braille */
            NOT_FULLY,

            /** Not readable in read aloud or dynamic braille */
            UNREADABLE,

            /** No information about nonvisual reading is available */
            NO_METADATA,
        }

        public enum class PrerecordedAudio {
            /** Prerecorded audio synchronized with text */
            SYNCHRONIZED,

            /** Prerecorded audio only */
            AUDIO_ONLY,

            /** Prerecorded audio clips */
            AUDIO_COMPLEMENTARY,

            /** No information about prerecorded audio is available */
            NO_METADATA,
        }

        override val statements: List<Statement> get() = buildList {
            add(
                when (visualAdjustments) {
                    VisualAdjustments.MODIFIABLE -> S.WAYS_OF_READING_VISUAL_ADJUSTMENTS_MODIFIABLE
                    VisualAdjustments.UNMODIFIABLE -> S.WAYS_OF_READING_VISUAL_ADJUSTMENTS_UNMODIFIABLE
                    VisualAdjustments.UNKNOWN -> S.WAYS_OF_READING_VISUAL_ADJUSTMENTS_UNKNOWN
                }
            )

            add(
                when (nonvisualReading) {
                    NonvisualReading.READABLE -> S.WAYS_OF_READING_NONVISUAL_READING_READABLE
                    NonvisualReading.NOT_FULLY -> S.WAYS_OF_READING_NONVISUAL_READING_NOT_FULLY
                    NonvisualReading.UNREADABLE -> S.WAYS_OF_READING_NONVISUAL_READING_NONE
                    NonvisualReading.NO_METADATA -> S.WAYS_OF_READING_NONVISUAL_READING_NO_METADATA
                }
            )

            if (nonvisualReadingAltText) {
                add(S.WAYS_OF_READING_NONVISUAL_READING_ALT_TEXT)
            }

            add(
                when (prerecordedAudio) {
                    PrerecordedAudio.SYNCHRONIZED -> S.WAYS_OF_READING_PRERECORDED_AUDIO_SYNCHRONIZED
                    PrerecordedAudio.AUDIO_ONLY -> S.WAYS_OF_READING_PRERECORDED_AUDIO_ONLY
                    PrerecordedAudio.AUDIO_COMPLEMENTARY -> S.WAYS_OF_READING_PRERECORDED_AUDIO_COMPLEMENTARY
                    PrerecordedAudio.NO_METADATA -> S.WAYS_OF_READING_PRERECORDED_AUDIO_NO_METADATA
                }
            )
        }

        public companion object {
            public operator fun invoke(publication: Publication): WaysOfReading {
                val isFixedLayout = publication.metadata.presentation.layout == EpubLayout.FIXED
                val a11y = publication.metadata.accessibility ?: Accessibility()

                val allText = a11y.accessModes == setOf(AccessMode.TEXTUAL) ||
                    a11y.accessModesSufficient.contains(setOf(PrimaryAccessMode.TEXTUAL))

                val someText = a11y.accessModes.contains(AccessMode.TEXTUAL) ||
                    a11y.accessModesSufficient.contains { it.contains(PrimaryAccessMode.TEXTUAL) }

                val noText = !(a11y.accessModes.isEmpty() && a11y.accessModesSufficient.isEmpty()) &&
                    !a11y.accessModes.contains(AccessMode.TEXTUAL) &&
                    !a11y.accessModesSufficient.contains { it.contains(PrimaryAccessMode.TEXTUAL) }

                val hasTextAlt = a11y.features.containsAny(
                    Feature.LONG_DESCRIPTION,
                    Feature.ALTERNATIVE_TEXT,
                    Feature.DESCRIBED_MATH,
                    Feature.TRANSCRIPT
                )

                return WaysOfReading(
                    visualAdjustments = when {
                        a11y.features.contains(Feature.DISPLAY_TRANSFORMABILITY) -> VisualAdjustments.MODIFIABLE
                        isFixedLayout -> VisualAdjustments.UNMODIFIABLE
                        else -> VisualAdjustments.UNKNOWN
                    },
                    nonvisualReading = when {
                        allText -> NonvisualReading.READABLE
                        someText || hasTextAlt -> NonvisualReading.NOT_FULLY
                        noText -> NonvisualReading.UNREADABLE
                        else -> NonvisualReading.NO_METADATA
                    },
                    nonvisualReadingAltText = hasTextAlt,
                    prerecordedAudio = when {
                        a11y.features.contains(Feature.SYNCHRONIZED_AUDIO_TEXT) -> PrerecordedAudio.SYNCHRONIZED
                        a11y.accessModesSufficient.contains { it.contains(PrimaryAccessMode.AUDITORY) } -> PrerecordedAudio.AUDIO_ONLY
                        a11y.accessModes.contains(AccessMode.AUDITORY) -> PrerecordedAudio.AUDIO_COMPLEMENTARY
                        else -> PrerecordedAudio.NO_METADATA
                    }
                )
            }
        }
    }

    /**
     * Identifies the navigation features included in the publication.
     *
     * https://w3c.github.io/publ-a11y/a11y-meta-display-guide/2.0/guidelines/#navigation
     *
     * @param tableOfContents Table of contents to all chapters of the text via links.
     * @param index Index with links to referenced entries.
     * @param headings Elements such as headings, tables, etc for structured navigation.
     * @param page Page list to go to pages from the print source version.
     */
    public data class Navigation(
        val tableOfContents: Boolean = false,
        val index: Boolean = false,
        val headings: Boolean = false,
        val page: Boolean = false,
    ) : Field {

        /**
         * Indicates whether no information about navigation features is
         * available.
         */
        public val noMetadata: Boolean
            get() = !tableOfContents && !index && !headings && !page

        override val shouldDisplay: Boolean get() = !noMetadata

        override fun localizedTitle(context: Context): String =
            context.getString(R.string.readium_a11y_navigation_title)

        override val statements: List<Statement> get() = buildList {
            if (tableOfContents) add(S.NAVIGATION_TOC)
            if (index) add(S.NAVIGATION_INDEX)
            if (headings) add(S.NAVIGATION_STRUCTURAL)
            if (page) add(S.NAVIGATION_PAGE_NAVIGATION)
            if (isEmpty()) add(S.NAVIGATION_NO_METADATA)
        }

        public companion object {
            public operator fun invoke(publication: Publication): Navigation {
                val features = publication.metadata.accessibility?.features ?: emptySet()

                return Navigation(
                    tableOfContents = features.contains(Feature.TABLE_OF_CONTENTS),
                    index = features.contains(Feature.INDEX),
                    headings = features.contains(Feature.STRUCTURAL_NAVIGATION),
                    page = features.contains(Feature.PAGE_NAVIGATION)
                )
            }
        }
    }

    /**
     * Indicates the presence of math, chemical formulas, extended descriptions
     * for information rich images, e.g., charts, diagrams, figures, graphs, and
     * whether these are in an accessible format or available in an alternative
     * form, e.g., whether math and chemical formulas are navigable with
     * assistive technologies, or whether extended descriptions are available
     * for information-rich images. In addition, it indicates the presence of
     * videos and if closed captions, open captions, or transcripts for
     * prerecorded audio are available.
     *
     * https://w3c.github.io/publ-a11y/a11y-meta-display-guide/2.0/guidelines/#rich-content
     *
     * @param extendedAltTextDescriptions Information-rich images are described by extended descriptions.
     * @param mathFormula Text descriptions of math are provided.
     * @param mathFormulaAsMathML Math formulas in accessible format (MathML).
     * @param mathFormulaAsLaTeX Math formulas in accessible format (LaTeX).
     * @param chemicalFormulaAsMathML Chemical formulas in accessible format (MathML).
     * @param chemicalFormulaAsLaTeX Chemical formulas in accessible format (LaTeX).
     * @param closedCaptions Videos included in publications have closed captions.
     * @param openCaptions Videos included in publications have open captions.
     * @param transcript Transcript(s) provided.
     */
    public data class RichContent(
        public val extendedAltTextDescriptions: Boolean = false,
        public val mathFormula: Boolean = false,
        public val mathFormulaAsMathML: Boolean = false,
        public val mathFormulaAsLaTeX: Boolean = false,
        public val chemicalFormulaAsMathML: Boolean = false,
        public val chemicalFormulaAsLaTeX: Boolean = false,
        public val closedCaptions: Boolean = false,
        public val openCaptions: Boolean = false,
        public val transcript: Boolean = false,
    ) : Field {

        /**
         * Indicates whether no information about rich content is available.
         */
        public val noMetadata: Boolean
            get() = !extendedAltTextDescriptions && !mathFormula && !mathFormulaAsMathML &&
                !mathFormulaAsLaTeX && !chemicalFormulaAsMathML && !chemicalFormulaAsLaTeX &&
                !closedCaptions && !openCaptions && !transcript

        override val shouldDisplay: Boolean get() = !noMetadata

        override fun localizedTitle(context: Context): String =
            context.getString(R.string.readium_a11y_rich_content_title)

        override val statements: List<Statement> get() = buildList {
            if (extendedAltTextDescriptions) add(S.RICH_CONTENT_EXTENDED)
            if (mathFormula) add(S.RICH_CONTENT_ACCESSIBLE_MATH_DESCRIBED)
            if (mathFormulaAsMathML) add(S.RICH_CONTENT_ACCESSIBLE_MATH_AS_MATHML)
            if (mathFormulaAsLaTeX) add(S.RICH_CONTENT_ACCESSIBLE_MATH_AS_LATEX)
            if (chemicalFormulaAsMathML) add(S.RICH_CONTENT_ACCESSIBLE_CHEMISTRY_AS_MATHML)
            if (chemicalFormulaAsLaTeX) add(S.RICH_CONTENT_ACCESSIBLE_CHEMISTRY_AS_LATEX)
            if (closedCaptions) add(S.RICH_CONTENT_CLOSED_CAPTIONS)
            if (openCaptions) add(S.RICH_CONTENT_OPEN_CAPTIONS)
            if (transcript) add(S.RICH_CONTENT_TRANSCRIPT)
            if (isEmpty()) add(S.RICH_CONTENT_UNKNOWN)
        }

        public companion object {
            public operator fun invoke(publication: Publication): RichContent {
                val features = publication.metadata.accessibility?.features ?: emptySet()
                return RichContent(
                    extendedAltTextDescriptions = features.contains(Feature.LONG_DESCRIPTION),
                    mathFormula = features.contains(Feature.DESCRIBED_MATH),
                    mathFormulaAsMathML = features.contains(Feature.MATHML),
                    mathFormulaAsLaTeX = features.contains(Feature.LATEX),
                    chemicalFormulaAsMathML = features.contains(Feature.MATHML_CHEMISTRY),
                    chemicalFormulaAsLaTeX = features.contains(Feature.LATEX_CHEMISTRY),
                    closedCaptions = features.contains(Feature.CLOSED_CAPTIONS),
                    openCaptions = features.contains(Feature.OPEN_CAPTIONS),
                    transcript = features.contains(Feature.TRANSCRIPT)
                )
            }
        }
    }

    /**
     * This section lists additional metadata categories that can help users
     * better understand the accessibility characteristics of digital
     * publications. These are for metadata that do not fit into the other
     * categories or are rarely used in trade publishing.
     *
     * @param pageBreakMarkers Page breaks included.
     * @param aria ARIA roles included.
     * @param audioDescriptions Audio descriptions.
     * @param braille Braille.
     * @param rubyAnnotations Some ruby annotations.
     * @param fullRubyAnnotations Full ruby annotations
     * @param highAudioContrast High contrast between foreground and background audio
     * @param highDisplayContrast High contrast between foreground text and background.
     * @param largePrint Large print.
     * @param signLanguage Sign language.
     * @param tactileGraphics Tactile graphics included.
     * @param tactileObjects Tactile 3D objects.
     * @param textToSpeechHinting Text-to-speech hinting provided.
     */
    public data class AdditionalInformation(
        public val pageBreakMarkers: Boolean = false,
        public val aria: Boolean = false,
        public val audioDescriptions: Boolean = false,
        public val braille: Boolean = false,
        public val rubyAnnotations: Boolean = false,
        public val fullRubyAnnotations: Boolean = false,
        public val highAudioContrast: Boolean = false,
        public val highDisplayContrast: Boolean = false,
        public val largePrint: Boolean = false,
        public val signLanguage: Boolean = false,
        public val tactileGraphics: Boolean = false,
        public val tactileObjects: Boolean = false,
        public val textToSpeechHinting: Boolean = false,
    ) : Field {

        /**
         * Indicates whether no additional information is provided.
         */
        public val noMetadata: Boolean
            get() = !pageBreakMarkers && !aria && !audioDescriptions && !braille &&
                !rubyAnnotations && !fullRubyAnnotations && !highAudioContrast &&
                !highDisplayContrast && !largePrint && !signLanguage &&
                !tactileGraphics && !tactileObjects && !textToSpeechHinting

        override val shouldDisplay: Boolean get() = !noMetadata

        override fun localizedTitle(context: Context): String =
            context.getString(R.string.readium_a11y_additional_accessibility_information_title)

        override val statements: List<Statement> get() = buildList {
            if (pageBreakMarkers) add(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_PAGE_BREAKS)
            if (aria) add(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_ARIA)
            if (audioDescriptions) add(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_AUDIO_DESCRIPTIONS)
            if (braille) add(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_BRAILLE)
            if (rubyAnnotations) add(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_RUBY_ANNOTATIONS)
            if (fullRubyAnnotations) add(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_FULL_RUBY_ANNOTATIONS)
            if (highAudioContrast) add(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_HIGH_CONTRAST_BETWEEN_FOREGROUND_AND_BACKGROUND_AUDIO)
            if (highDisplayContrast) add(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_HIGH_CONTRAST_BETWEEN_TEXT_AND_BACKGROUND)
            if (largePrint) add(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_LARGE_PRINT)
            if (signLanguage) add(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_SIGN_LANGUAGE)
            if (tactileGraphics) add(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_TACTILE_GRAPHICS)
            if (tactileObjects) add(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_TACTILE_OBJECTS)
            if (textToSpeechHinting) add(S.ADDITIONAL_ACCESSIBILITY_INFORMATION_TEXT_TO_SPEECH_HINTING)
        }

        public companion object {
            public operator fun invoke(publication: Publication): AdditionalInformation {
                val features = publication.metadata.accessibility?.features ?: emptySet()
                return AdditionalInformation(
                    pageBreakMarkers = features.contains(Feature.PAGE_BREAK_MARKERS) || features.contains(Feature.PRINT_PAGE_NUMBERS),
                    aria = features.contains(Feature.ARIA),
                    audioDescriptions = features.contains(Feature.AUDIO_DESCRIPTION),
                    braille = features.contains(Feature.BRAILLE),
                    rubyAnnotations = features.contains(Feature.RUBY_ANNOTATIONS),
                    fullRubyAnnotations = features.contains(Feature.FULL_RUBY_ANNOTATIONS),
                    highAudioContrast = features.contains(Feature.HIGH_CONTRAST_AUDIO),
                    highDisplayContrast = features.contains(Feature.HIGH_CONTRAST_DISPLAY),
                    largePrint = features.contains(Feature.LARGE_PRINT),
                    signLanguage = features.contains(Feature.SIGN_LANGUAGE),
                    tactileGraphics = features.contains(Feature.TACTILE_GRAPHIC),
                    tactileObjects = features.contains(Feature.TACTILE_OBJECT),
                    textToSpeechHinting = features.contains(Feature.TTS_MARKUP)
                )
            }
        }
    }

    /**
     * Identifies any potential hazards (e.g., flashing elements, sounds,
     * and motion simulation) that could afflict physiologically sensitive
     * users.
     *
     * Unlike other accessibility properties, the presence of hazards can be
     * expressed either positively or negatively. This is because users
     * search for content that is safe for them as well as want to know when
     * content is potentially dangerous to them.
     *
     * https://w3c.github.io/publ-a11y/a11y-meta-display-guide/2.0/guidelines/#hazards
     *
     * @param flashing The publication contains flashing content which can
     * cause photosensitive seizures.
     * @param motion The publication contains motion simulations that can
     * cause motion sickness.
     * @param sound The publication contains sounds which can be uncomfortable.
     */
    public data class Hazards(
        public val flashing: Hazard = Hazard.NO_METADATA,
        public val motion: Hazard = Hazard.NO_METADATA,
        public val sound: Hazard = Hazard.NO_METADATA,
    ) : Field {

        public enum class Hazard {
            YES,
            NO,
            UNKNOWN,
            NO_METADATA,
        }

        /**
         * Indicates whether no information about hazards is available.
         */
        public val noMetadata: Boolean
            get() = flashing == Hazard.NO_METADATA &&
                motion == Hazard.NO_METADATA &&
                sound == Hazard.NO_METADATA

        /**
         * The publication contains no hazards.
         */
        public val noHazards: Boolean
            get() = flashing == Hazard.NO &&
                motion == Hazard.NO &&
                sound == Hazard.NO

        /**
         * The presence of hazards is unknown.
         */
        public val unknown: Boolean
            get() = flashing == Hazard.UNKNOWN &&
                motion == Hazard.UNKNOWN &&
                sound == Hazard.UNKNOWN

        override val shouldDisplay: Boolean get() = !noMetadata

        override fun localizedTitle(context: Context): String =
            context.getString(R.string.readium_a11y_hazards_title)

        override val statements: List<Statement> get() = buildList {
            when {
                noHazards -> add(S.HAZARDS_NONE)
                unknown -> add(S.HAZARDS_UNKNOWN)
                noMetadata -> add(S.HAZARDS_NO_METADATA)
                else -> {
                    if (flashing == Hazard.YES) add(S.HAZARDS_FLASHING)
                    if (motion == Hazard.YES) add(S.HAZARDS_MOTION)
                    if (sound == Hazard.YES) add(S.HAZARDS_SOUND)

                    if (flashing == Hazard.UNKNOWN) add(S.HAZARDS_FLASHING_UNKNOWN)
                    if (motion == Hazard.UNKNOWN) add(S.HAZARDS_MOTION_UNKNOWN)
                    if (sound == Hazard.UNKNOWN) add(S.HAZARDS_SOUND_UNKNOWN)

                    if (flashing == Hazard.NO) add(S.HAZARDS_FLASHING_NONE)
                    if (motion == Hazard.NO) add(S.HAZARDS_MOTION_NONE)
                    if (sound == Hazard.NO) add(S.HAZARDS_SOUND_NONE)
                }
            }
        }

        public companion object {
            public operator fun invoke(publication: Publication): Hazards {
                val hazards = publication.metadata.accessibility?.hazards ?: emptySet()

                val fallback = when {
                    hazards.contains(Accessibility.Hazard.NONE) -> Hazard.NO
                    hazards.contains(Accessibility.Hazard.UNKNOWN) -> Hazard.UNKNOWN
                    else -> Hazard.NO_METADATA
                }

                return Hazards(
                    flashing = when {
                        hazards.contains(Accessibility.Hazard.FLASHING) -> Hazard.YES
                        hazards.contains(Accessibility.Hazard.NO_FLASHING_HAZARD) -> Hazard.NO
                        hazards.contains(Accessibility.Hazard.UNKNOWN_FLASHING_HAZARD) -> Hazard.UNKNOWN
                        else -> fallback
                    },
                    motion = when {
                        hazards.contains(Accessibility.Hazard.MOTION_SIMULATION) -> Hazard.YES
                        hazards.contains(Accessibility.Hazard.NO_MOTION_SIMULATION_HAZARD) -> Hazard.NO
                        hazards.contains(Accessibility.Hazard.UNKNOWN_MOTION_SIMULATION_HAZARD) -> Hazard.UNKNOWN
                        else -> fallback
                    },
                    sound = when {
                        hazards.contains(Accessibility.Hazard.SOUND) -> Hazard.YES
                        hazards.contains(Accessibility.Hazard.NO_SOUND_HAZARD) -> Hazard.NO
                        hazards.contains(Accessibility.Hazard.UNKNOWN_SOUND_HAZARD) -> Hazard.UNKNOWN
                        else -> fallback
                    }
                )
            }
        }
    }

    /**
     * Identifies whether the digital publication claims to meet internationally
     * recognized conformance standards for accessibility.
     *
     * https://w3c.github.io/publ-a11y/a11y-meta-display-guide/2.0/guidelines/#conformance-group
     *
     * @param profiles Accessibility conformance profiles.
     */
    public data class Conformance(
        public val profiles: Set<Accessibility.Profile> = emptySet(),
    ) : Field {

        /** "Conformance" should be rendered even if there is no metadata. */
        override val shouldDisplay: Boolean get() = true

        override fun localizedTitle(context: Context): String =
            context.getString(R.string.readium_a11y_conformance_title)

        override val statements: List<Statement>
            get() = buildList {
                if (profiles.isEmpty()) {
                    add(S.CONFORMANCE_NO)
                    return@buildList
                }

                add(
                    when {
                        profiles.contains { it.isWCAGLevelAAA } -> S.CONFORMANCE_AAA
                        profiles.contains { it.isWCAGLevelAA } -> S.CONFORMANCE_AA
                        profiles.contains { it.isWCAGLevelA } -> S.CONFORMANCE_A
                        else -> S.CONFORMANCE_UNKNOWN_STANDARD
                    }
                )

                // FIXME: Waiting on W3C to offer localized strings with placeholders instead of concatenation. See https://github.com/w3c/publ-a11y/issues/688
            }

        public companion object {
            public operator fun invoke(publication: Publication): Conformance =
                Conformance(profiles = publication.metadata.accessibility?.conformsTo ?: emptySet())
        }
    }

    /**
     * In some jurisdictions publishers may be able to claim an exemption from
     * the provision of accessible publications, including the provision of
     * accessibility metadata. This should always be subject to clarification by
     * legal counsel for each jurisdiction.
     *
     * https://w3c.github.io/publ-a11y/a11y-meta-display-guide/2.0/guidelines/#legal-considerations
     *
     * @param exemption This publication claims an accessibility exemption in
     * some jurisdictions.
     */
    public data class Legal(
        public val exemption: Boolean = false,
    ) : Field {

        override val shouldDisplay: Boolean get() = exemption

        override fun localizedTitle(context: Context): String =
            context.getString(R.string.readium_a11y_legal_considerations_title)

        override val statements: List<Statement>
            get() = buildList {
                if (exemption) {
                    add(S.LEGAL_CONSIDERATIONS_EXEMPT)
                } else {
                    add(S.LEGAL_CONSIDERATIONS_NO_METADATA)
                }
            }

        public companion object {
            public operator fun invoke(publication: Publication): Legal =
                Legal(exemption = publication.metadata.accessibility?.exemptions?.isNotEmpty() ?: false)
        }
    }

    /**
     * The accessibility summary was intended (in EPUB Accessibility 1.0) to
     * describe in human-readable prose the accessibility features present in
     * the publication as well as any shortcomings. Starting with EPUB
     * Accessibility version 1.1 the accessibility summary became a human-
     * readable summary of the accessibility that complements, but does not
     * duplicate, the other discoverability metadata.
     *
     * https://w3c.github.io/publ-a11y/a11y-meta-display-guide/2.0/guidelines/#accessibility-summary
     */
    public data class AccessibilitySummary(
        public val summary: String? = null,
    ) : Field {

        override val shouldDisplay: Boolean get() = summary != null

        override fun localizedTitle(context: Context): String =
            context.getString(R.string.readium_a11y_accessibility_summary_title)

        override val statements: List<Statement>
            get() = buildList {
                if (summary != null) {
                    add(DynamicStatement(summary))
                } else {
                    add(S.ACCESSIBILITY_SUMMARY_NO_METADATA)
                }
            }

        public companion object {
            public operator fun invoke(publication: Publication): AccessibilitySummary =
                AccessibilitySummary(
                    summary = publication.metadata.accessibility?.summary
                )
        }
    }

    /**
     * Represents a single accessibility claim, such as "Appearance can be
     * modified".
     */
    public sealed interface Statement {

        /**
         * A localized representation for this display statement.
         *
         * For example:
         * - compact: Appearance can be modified
         * - descriptive: For example, "Appearance of the text and page layout can
         *   be modified according to the capabilities of the reading system (font
         *   family and font size, spaces between paragraphs, sentences, words, and
         *   letters, as well as color of background and text)
         *
         * Some statements contain HTTP links; so we use an ``NSAttributedString``.
         *
         * @param descriptive When true, will return the long descriptive statement.
         */
        public fun localizedString(context: Context, descriptive: Boolean): String
    }

    /**
     * Localized display statement generated from the official JSON translations.
     */
    internal class StaticStatement(
        val string: S,
    ) : Statement {

        override fun localizedString(context: Context, descriptive: Boolean): String =
            string.localizedString(context, descriptive = descriptive)
    }

    /**
     * Localized display statement computed during runtime.
     */
    internal class DynamicStatement(
        private val compactString: String,
        private val descriptiveString: String = compactString,
    ) : Statement {
        override fun localizedString(context: Context, descriptive: Boolean): String =
            if (descriptive) descriptiveString else compactString
    }
}

/**
 * Localized display string with compact and descriptive variant.
 *
 * See https://w3c.github.io/publ-a11y/a11y-meta-display-guide/2.0/draft/localizations/
 */
internal data class AccessibilityDisplayString(
    @StringRes private val compactId: Int,
    @StringRes private val descriptiveId: Int,
) {
    /**
     * Returns the localized string for this display string.
     *
     * @param descriptive When true, will return the long descriptive statement.
     */
    fun localizedString(context: Context, descriptive: Boolean): String =
        context.getString(if (descriptive) descriptiveId else compactId)
            .trim()

    companion object
}

// Syntactic sugar

private typealias S = AccessibilityDisplayString

internal fun MutableList<Statement>.add(string: AccessibilityDisplayString) {
    add(StaticStatement(string))
}
