/*
 *  Copyright 2025 Readium Foundation. All rights reserved.
 *  Use of this source code is governed by the BSD-style license
 *  available in the top-level LICENSE file of the project.
 */

// DO NOT EDIT. File generated automatically from v2.0.c of the en-US JSON strings.

package org.readium.r2.shared.accessibility

import android.content.Context
import androidx.annotation.StringRes
import org.readium.r2.shared.R

/**
 * Returns the localized string for this display string.
 *
 * @param descriptive When true, will return the long descriptive statement.
 */
internal fun AccessibilityDisplayString.localizedString(context: Context, descriptive: Boolean): String =
    context.getString(resourceId(descriptive)).trim()

@StringRes
private fun AccessibilityDisplayString.resourceId(descriptive: Boolean): Int =
    when (key) {
        "ways_of_reading_nonvisual_reading_alt_text" -> if (descriptive) R.string.readium_a11y_ways_of_reading_nonvisual_reading_alt_text_descriptive else R.string.readium_a11y_ways_of_reading_nonvisual_reading_alt_text_compact
        "ways_of_reading_nonvisual_reading_no_metadata" -> if (descriptive) R.string.readium_a11y_ways_of_reading_nonvisual_reading_no_metadata_descriptive else R.string.readium_a11y_ways_of_reading_nonvisual_reading_no_metadata_compact
        "ways_of_reading_nonvisual_reading_none" -> if (descriptive) R.string.readium_a11y_ways_of_reading_nonvisual_reading_none_descriptive else R.string.readium_a11y_ways_of_reading_nonvisual_reading_none_compact
        "ways_of_reading_nonvisual_reading_not_fully" -> if (descriptive) R.string.readium_a11y_ways_of_reading_nonvisual_reading_not_fully_descriptive else R.string.readium_a11y_ways_of_reading_nonvisual_reading_not_fully_compact
        "ways_of_reading_nonvisual_reading_readable" -> if (descriptive) R.string.readium_a11y_ways_of_reading_nonvisual_reading_readable_descriptive else R.string.readium_a11y_ways_of_reading_nonvisual_reading_readable_compact
        "ways_of_reading_prerecorded_audio_complementary" -> if (descriptive) R.string.readium_a11y_ways_of_reading_prerecorded_audio_complementary_descriptive else R.string.readium_a11y_ways_of_reading_prerecorded_audio_complementary_compact
        "ways_of_reading_prerecorded_audio_no_metadata" -> if (descriptive) R.string.readium_a11y_ways_of_reading_prerecorded_audio_no_metadata_descriptive else R.string.readium_a11y_ways_of_reading_prerecorded_audio_no_metadata_compact
        "ways_of_reading_prerecorded_audio_only" -> if (descriptive) R.string.readium_a11y_ways_of_reading_prerecorded_audio_only_descriptive else R.string.readium_a11y_ways_of_reading_prerecorded_audio_only_compact
        "ways_of_reading_prerecorded_audio_synchronized" -> if (descriptive) R.string.readium_a11y_ways_of_reading_prerecorded_audio_synchronized_descriptive else R.string.readium_a11y_ways_of_reading_prerecorded_audio_synchronized_compact
        "ways_of_reading_visual_adjustments_modifiable" -> if (descriptive) R.string.readium_a11y_ways_of_reading_visual_adjustments_modifiable_descriptive else R.string.readium_a11y_ways_of_reading_visual_adjustments_modifiable_compact
        "ways_of_reading_visual_adjustments_unknown" -> if (descriptive) R.string.readium_a11y_ways_of_reading_visual_adjustments_unknown_descriptive else R.string.readium_a11y_ways_of_reading_visual_adjustments_unknown_compact
        "ways_of_reading_visual_adjustments_unmodifiable" -> if (descriptive) R.string.readium_a11y_ways_of_reading_visual_adjustments_unmodifiable_descriptive else R.string.readium_a11y_ways_of_reading_visual_adjustments_unmodifiable_compact
        "conformance_a" -> if (descriptive) R.string.readium_a11y_conformance_a_descriptive else R.string.readium_a11y_conformance_a_compact
        "conformance_aa" -> if (descriptive) R.string.readium_a11y_conformance_aa_descriptive else R.string.readium_a11y_conformance_aa_compact
        "conformance_aaa" -> if (descriptive) R.string.readium_a11y_conformance_aaa_descriptive else R.string.readium_a11y_conformance_aaa_compact
        "conformance_certifier" -> if (descriptive) R.string.readium_a11y_conformance_certifier_descriptive else R.string.readium_a11y_conformance_certifier_compact
        "conformance_certifier_credentials" -> if (descriptive) R.string.readium_a11y_conformance_certifier_credentials_descriptive else R.string.readium_a11y_conformance_certifier_credentials_compact
        "conformance_details_certification_info" -> if (descriptive) R.string.readium_a11y_conformance_details_certification_info_descriptive else R.string.readium_a11y_conformance_details_certification_info_compact
        "conformance_details_certifier_report" -> if (descriptive) R.string.readium_a11y_conformance_details_certifier_report_descriptive else R.string.readium_a11y_conformance_details_certifier_report_compact
        "conformance_details_claim" -> if (descriptive) R.string.readium_a11y_conformance_details_claim_descriptive else R.string.readium_a11y_conformance_details_claim_compact
        "conformance_details_epub_accessibility_1_0" -> if (descriptive) R.string.readium_a11y_conformance_details_epub_accessibility_1_0_descriptive else R.string.readium_a11y_conformance_details_epub_accessibility_1_0_compact
        "conformance_details_epub_accessibility_1_1" -> if (descriptive) R.string.readium_a11y_conformance_details_epub_accessibility_1_1_descriptive else R.string.readium_a11y_conformance_details_epub_accessibility_1_1_compact
        "conformance_details_level_a" -> if (descriptive) R.string.readium_a11y_conformance_details_level_a_descriptive else R.string.readium_a11y_conformance_details_level_a_compact
        "conformance_details_level_aa" -> if (descriptive) R.string.readium_a11y_conformance_details_level_aa_descriptive else R.string.readium_a11y_conformance_details_level_aa_compact
        "conformance_details_level_aaa" -> if (descriptive) R.string.readium_a11y_conformance_details_level_aaa_descriptive else R.string.readium_a11y_conformance_details_level_aaa_compact
        "conformance_details_wcag_2_0" -> if (descriptive) R.string.readium_a11y_conformance_details_wcag_2_0_descriptive else R.string.readium_a11y_conformance_details_wcag_2_0_compact
        "conformance_details_wcag_2_1" -> if (descriptive) R.string.readium_a11y_conformance_details_wcag_2_1_descriptive else R.string.readium_a11y_conformance_details_wcag_2_1_compact
        "conformance_details_wcag_2_2" -> if (descriptive) R.string.readium_a11y_conformance_details_wcag_2_2_descriptive else R.string.readium_a11y_conformance_details_wcag_2_2_compact
        "conformance_no" -> if (descriptive) R.string.readium_a11y_conformance_no_descriptive else R.string.readium_a11y_conformance_no_compact
        "conformance_unknown_standard" -> if (descriptive) R.string.readium_a11y_conformance_unknown_standard_descriptive else R.string.readium_a11y_conformance_unknown_standard_compact
        "navigation_index" -> if (descriptive) R.string.readium_a11y_navigation_index_descriptive else R.string.readium_a11y_navigation_index_compact
        "navigation_no_metadata" -> if (descriptive) R.string.readium_a11y_navigation_no_metadata_descriptive else R.string.readium_a11y_navigation_no_metadata_compact
        "navigation_page_navigation" -> if (descriptive) R.string.readium_a11y_navigation_page_navigation_descriptive else R.string.readium_a11y_navigation_page_navigation_compact
        "navigation_structural" -> if (descriptive) R.string.readium_a11y_navigation_structural_descriptive else R.string.readium_a11y_navigation_structural_compact
        "navigation_toc" -> if (descriptive) R.string.readium_a11y_navigation_toc_descriptive else R.string.readium_a11y_navigation_toc_compact
        "rich_content_accessible_chemistry_as_latex" -> if (descriptive) R.string.readium_a11y_rich_content_accessible_chemistry_as_latex_descriptive else R.string.readium_a11y_rich_content_accessible_chemistry_as_latex_compact
        "rich_content_accessible_chemistry_as_mathml" -> if (descriptive) R.string.readium_a11y_rich_content_accessible_chemistry_as_mathml_descriptive else R.string.readium_a11y_rich_content_accessible_chemistry_as_mathml_compact
        "rich_content_accessible_math_as_latex" -> if (descriptive) R.string.readium_a11y_rich_content_accessible_math_as_latex_descriptive else R.string.readium_a11y_rich_content_accessible_math_as_latex_compact
        "rich_content_accessible_math_as_mathml" -> if (descriptive) R.string.readium_a11y_rich_content_accessible_math_as_mathml_descriptive else R.string.readium_a11y_rich_content_accessible_math_as_mathml_compact
        "rich_content_accessible_math_described" -> if (descriptive) R.string.readium_a11y_rich_content_accessible_math_described_descriptive else R.string.readium_a11y_rich_content_accessible_math_described_compact
        "rich_content_closed_captions" -> if (descriptive) R.string.readium_a11y_rich_content_closed_captions_descriptive else R.string.readium_a11y_rich_content_closed_captions_compact
        "rich_content_extended" -> if (descriptive) R.string.readium_a11y_rich_content_extended_descriptive else R.string.readium_a11y_rich_content_extended_compact
        "rich_content_open_captions" -> if (descriptive) R.string.readium_a11y_rich_content_open_captions_descriptive else R.string.readium_a11y_rich_content_open_captions_compact
        "rich_content_transcript" -> if (descriptive) R.string.readium_a11y_rich_content_transcript_descriptive else R.string.readium_a11y_rich_content_transcript_compact
        "rich_content_unknown" -> if (descriptive) R.string.readium_a11y_rich_content_unknown_descriptive else R.string.readium_a11y_rich_content_unknown_compact
        "hazards_flashing" -> if (descriptive) R.string.readium_a11y_hazards_flashing_descriptive else R.string.readium_a11y_hazards_flashing_compact
        "hazards_flashing_none" -> if (descriptive) R.string.readium_a11y_hazards_flashing_none_descriptive else R.string.readium_a11y_hazards_flashing_none_compact
        "hazards_flashing_unknown" -> if (descriptive) R.string.readium_a11y_hazards_flashing_unknown_descriptive else R.string.readium_a11y_hazards_flashing_unknown_compact
        "hazards_motion" -> if (descriptive) R.string.readium_a11y_hazards_motion_descriptive else R.string.readium_a11y_hazards_motion_compact
        "hazards_motion_none" -> if (descriptive) R.string.readium_a11y_hazards_motion_none_descriptive else R.string.readium_a11y_hazards_motion_none_compact
        "hazards_motion_unknown" -> if (descriptive) R.string.readium_a11y_hazards_motion_unknown_descriptive else R.string.readium_a11y_hazards_motion_unknown_compact
        "hazards_no_metadata" -> if (descriptive) R.string.readium_a11y_hazards_no_metadata_descriptive else R.string.readium_a11y_hazards_no_metadata_compact
        "hazards_none" -> if (descriptive) R.string.readium_a11y_hazards_none_descriptive else R.string.readium_a11y_hazards_none_compact
        "hazards_sound" -> if (descriptive) R.string.readium_a11y_hazards_sound_descriptive else R.string.readium_a11y_hazards_sound_compact
        "hazards_sound_none" -> if (descriptive) R.string.readium_a11y_hazards_sound_none_descriptive else R.string.readium_a11y_hazards_sound_none_compact
        "hazards_sound_unknown" -> if (descriptive) R.string.readium_a11y_hazards_sound_unknown_descriptive else R.string.readium_a11y_hazards_sound_unknown_compact
        "hazards_unknown" -> if (descriptive) R.string.readium_a11y_hazards_unknown_descriptive else R.string.readium_a11y_hazards_unknown_compact
        "accessibility_summary_no_metadata" -> if (descriptive) R.string.readium_a11y_accessibility_summary_no_metadata_descriptive else R.string.readium_a11y_accessibility_summary_no_metadata_compact
        "accessibility_summary_publisher_contact" -> if (descriptive) R.string.readium_a11y_accessibility_summary_publisher_contact_descriptive else R.string.readium_a11y_accessibility_summary_publisher_contact_compact
        "legal_considerations_exempt" -> if (descriptive) R.string.readium_a11y_legal_considerations_exempt_descriptive else R.string.readium_a11y_legal_considerations_exempt_compact
        "legal_considerations_no_metadata" -> if (descriptive) R.string.readium_a11y_legal_considerations_no_metadata_descriptive else R.string.readium_a11y_legal_considerations_no_metadata_compact
        "additional_accessibility_information_aria" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_aria_descriptive else R.string.readium_a11y_additional_accessibility_information_aria_compact
        "additional_accessibility_information_audio_descriptions" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_audio_descriptions_descriptive else R.string.readium_a11y_additional_accessibility_information_audio_descriptions_compact
        "additional_accessibility_information_braille" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_braille_descriptive else R.string.readium_a11y_additional_accessibility_information_braille_compact
        "additional_accessibility_information_color_not_sole_means_of_conveying_information" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_color_not_sole_means_of_conveying_information_descriptive else R.string.readium_a11y_additional_accessibility_information_color_not_sole_means_of_conveying_information_compact
        "additional_accessibility_information_dyslexia_readability" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_dyslexia_readability_descriptive else R.string.readium_a11y_additional_accessibility_information_dyslexia_readability_compact
        "additional_accessibility_information_full_ruby_annotations" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_full_ruby_annotations_descriptive else R.string.readium_a11y_additional_accessibility_information_full_ruby_annotations_compact
        "additional_accessibility_information_high_contrast_between_foreground_and_background_audio" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_high_contrast_between_foreground_and_background_audio_descriptive else R.string.readium_a11y_additional_accessibility_information_high_contrast_between_foreground_and_background_audio_compact
        "additional_accessibility_information_high_contrast_between_text_and_background" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_high_contrast_between_text_and_background_descriptive else R.string.readium_a11y_additional_accessibility_information_high_contrast_between_text_and_background_compact
        "additional_accessibility_information_large_print" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_large_print_descriptive else R.string.readium_a11y_additional_accessibility_information_large_print_compact
        "additional_accessibility_information_page_breaks" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_page_breaks_descriptive else R.string.readium_a11y_additional_accessibility_information_page_breaks_compact
        "additional_accessibility_information_ruby_annotations" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_ruby_annotations_descriptive else R.string.readium_a11y_additional_accessibility_information_ruby_annotations_compact
        "additional_accessibility_information_sign_language" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_sign_language_descriptive else R.string.readium_a11y_additional_accessibility_information_sign_language_compact
        "additional_accessibility_information_tactile_graphics" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_tactile_graphics_descriptive else R.string.readium_a11y_additional_accessibility_information_tactile_graphics_compact
        "additional_accessibility_information_tactile_objects" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_tactile_objects_descriptive else R.string.readium_a11y_additional_accessibility_information_tactile_objects_compact
        "additional_accessibility_information_text_to_speech_hinting" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_text_to_speech_hinting_descriptive else R.string.readium_a11y_additional_accessibility_information_text_to_speech_hinting_compact
        "additional_accessibility_information_ultra_high_contrast_between_text_and_background" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_ultra_high_contrast_between_text_and_background_descriptive else R.string.readium_a11y_additional_accessibility_information_ultra_high_contrast_between_text_and_background_compact
        "additional_accessibility_information_visible_page_numbering" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_visible_page_numbering_descriptive else R.string.readium_a11y_additional_accessibility_information_visible_page_numbering_compact
        "additional_accessibility_information_without_background_sounds" -> if (descriptive) R.string.readium_a11y_additional_accessibility_information_without_background_sounds_descriptive else R.string.readium_a11y_additional_accessibility_information_without_background_sounds_compact
        else -> throw IllegalArgumentException("Unknown accessibility display string: $key")
    }
