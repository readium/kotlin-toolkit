/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import android.content.Context
import androidx.annotation.StringRes
import org.readium.r2.shared.R
import org.readium.r2.shared.extensions.contains
import org.readium.r2.shared.extensions.containsAny
import org.readium.r2.shared.publication.Accessibility.AccessMode
import org.readium.r2.shared.publication.Accessibility.PrimaryAccessMode
import org.readium.r2.shared.publication.Accessibility.Feature
import org.readium.r2.shared.publication.Accessibility.Hazard
import org.readium.r2.shared.publication.AccessibilityMetadataDisplayGuide.Statement
import org.readium.r2.shared.publication.AccessibilityMetadataDisplayGuide.StaticStatement
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
//    public val navigation: Navigation,
//    public val richContent: RichContent,
//    public val additionalInformation: AdditionalInformation,
//    public val hazards: Hazards,
//    public val conformance: Conformance,
//    public val legal: Legal,
//    public val accessibilitySummary: AccessibilitySummary,
) {

    /**
     * Creates a new display guide for the given `publication` metadata.
     */
    public constructor(publication: Publication) : this(
        waysOfReading = WaysOfReading(publication),
//        navigation = Navigation(publication),
//        richContent = RichContent(publication),
//        additionalInformation = AdditionalInformation(publication),
//        hazards = Hazards(publication),
//        conformance = Conformance(publication),
//        legal = Legal(publication),
//        accessibilitySummary = AccessibilitySummary(publication),
    )

    /**
     * Returns the list of display fields in their recommended order.
     */
    public val fields: List<Field> =
        listOf(
            waysOfReading,
//            navigation,
//            richContent,
//            additionalInformation,
//            hazards,
//            conformance,
//            legal,
//            accessibilitySummary,
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
        public val prerecordedAudio: PrerecordedAudio = PrerecordedAudio.NO_METADATA
    ): Field {

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
            add(when (visualAdjustments) {
                VisualAdjustments.MODIFIABLE -> S.WAYS_OF_READING_VISUAL_ADJUSTMENTS_MODIFIABLE
                VisualAdjustments.UNMODIFIABLE -> S.WAYS_OF_READING_VISUAL_ADJUSTMENTS_UNMODIFIABLE
                VisualAdjustments.UNKNOWN -> S.WAYS_OF_READING_VISUAL_ADJUSTMENTS_UNKNOWN
            })

            add(when (nonvisualReading) {
                NonvisualReading.READABLE -> S.WAYS_OF_READING_NONVISUAL_READING_READABLE
                NonvisualReading.NOT_FULLY -> S.WAYS_OF_READING_NONVISUAL_READING_NOT_FULLY
                NonvisualReading.UNREADABLE -> S.WAYS_OF_READING_NONVISUAL_READING_NONE
                NonvisualReading.NO_METADATA -> S.WAYS_OF_READING_NONVISUAL_READING_NO_METADATA
            })

            if (nonvisualReadingAltText) {
                add(S.WAYS_OF_READING_NONVISUAL_READING_ALT_TEXT)
            }

            add(when (prerecordedAudio) {
                PrerecordedAudio.SYNCHRONIZED -> S.WAYS_OF_READING_PRERECORDED_AUDIO_SYNCHRONIZED
                PrerecordedAudio.AUDIO_ONLY -> S.WAYS_OF_READING_PRERECORDED_AUDIO_ONLY
                PrerecordedAudio.AUDIO_COMPLEMENTARY -> S.WAYS_OF_READING_PRERECORDED_AUDIO_COMPLEMENTARY
                PrerecordedAudio.NO_METADATA -> S.WAYS_OF_READING_PRERECORDED_AUDIO_NO_METADATA
            })
        }

        public companion object {
            public operator fun invoke(publication: Publication): WaysOfReading {
                val isFixedLayout = publication.metadata.presentation.layout == EpubLayout.FIXED
                val a11y = publication.metadata.accessibility ?: Accessibility()

                val allText = a11y.accessModes == setOf(AccessMode.TEXTUAL)
                    || a11y.accessModesSufficient.contains(setOf(PrimaryAccessMode.TEXTUAL))

                val someText = a11y.accessModes.contains(AccessMode.TEXTUAL)
                    || a11y.accessModesSufficient.contains { it.contains(PrimaryAccessMode.TEXTUAL) }

                val noText = !(a11y.accessModes.isEmpty() && a11y.accessModesSufficient.isEmpty())
                    && !a11y.accessModes.contains(AccessMode.TEXTUAL)
                    && !a11y.accessModesSufficient.contains { it.contains(PrimaryAccessMode.TEXTUAL) }

                val hasTextAlt = a11y.features.containsAny(
                    Feature.LONG_DESCRIPTION,
                    Feature.ALTERNATIVE_TEXT,
                    Feature.DESCRIBED_MATH,
                    Feature.TRANSCRIPT
                )

                return WaysOfReading(
                    visualAdjustments =  when {
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
