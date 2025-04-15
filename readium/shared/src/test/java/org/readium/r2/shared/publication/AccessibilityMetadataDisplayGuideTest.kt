package org.readium.r2.shared.publication

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.R
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.publication.Accessibility.AccessMode
import org.readium.r2.shared.publication.Accessibility.Feature
import org.readium.r2.shared.publication.Accessibility.PrimaryAccessMode
import org.readium.r2.shared.publication.AccessibilityMetadataDisplayGuide.StaticStatement
import org.readium.r2.shared.publication.AccessibilityMetadataDisplayGuide.WaysOfReading
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
        fun test(layout: EpubLayout, a11y: Accessibility?, expected: WaysOfReading.VisualAdjustments) {
            val publication = publication(layout = layout, accessibility = a11y)
            val sut = WaysOfReading(publication)
            assertEquals(expected, sut.visualAdjustments)
        }

        val displayTransformability = Accessibility(features = setOf(Feature.DISPLAY_TRANSFORMABILITY))

        test(EpubLayout.REFLOWABLE, null, WaysOfReading.VisualAdjustments.UNKNOWN)
        test(EpubLayout.REFLOWABLE, displayTransformability, WaysOfReading.VisualAdjustments.MODIFIABLE)
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
        test(Accessibility(accessModes = emptySet(), accessModesSufficient = emptySet()), WaysOfReading.NonvisualReading.NO_METADATA)

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
            Accessibility(accessModesSufficient = setOf(setOf(PrimaryAccessMode.TEXTUAL, PrimaryAccessMode.AUDITORY))),
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
            ), WaysOfReading.NonvisualReading.UNREADABLE
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
                accessModesSufficient = setOf(setOf(PrimaryAccessMode.AUDITORY), setOf(
                    PrimaryAccessMode.TEXTUAL))
            ),
            WaysOfReading.PrerecordedAudio.AUDIO_ONLY
        )
        test(
            Accessibility(
                accessModes = setOf(AccessMode.TEXTUAL, AccessMode.AUDITORY),
                accessModesSufficient = setOf(setOf(PrimaryAccessMode.AUDITORY), setOf(
                    PrimaryAccessMode.TEXTUAL))
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

    @OptIn(InternalReadiumApi::class)
    private fun publication(
        layout: EpubLayout? = null,
        accessibility: Accessibility?
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