/*
 *  Copyright 2025 Readium Foundation. All rights reserved.
 *  Use of this source code is governed by the BSD-style license
 *  available in the top-level LICENSE file of the project.
 */

// DO NOT EDIT. File generated automatically from v2.0.c of the en-US JSON strings.

package org.readium.r2.shared.accessibility

/**
 * Returns the localized string for this display string.
 *
 * On iOS, only the en-US strings of the W3C display guide are bundled.
 *
 * @param descriptive When true, will return the long descriptive statement.
 */
internal fun AccessibilityDisplayString.localizedString(descriptive: Boolean): String =
    requireNotNull(
        accessibilityDisplayStrings[key + if (descriptive) "_descriptive" else "_compact"]
    ) { "Unknown accessibility display string: $key" }.trim()

/**
 * The en-US strings of the W3C accessibility metadata display guide, keyed by resource name
 * (without the `readium_a11y_` prefix).
 */
internal val accessibilityDisplayStrings: Map<String, String> = mapOf(
    "ways_of_reading_title" to
        "Ways of reading",
    "ways_of_reading_nonvisual_reading_alt_text_compact" to
        "Has alternative text",
    "ways_of_reading_nonvisual_reading_alt_text_descriptive" to
        "Has alternative text descriptions for images",
    "ways_of_reading_nonvisual_reading_no_metadata_compact" to
        "No information about nonvisual reading is available",
    "ways_of_reading_nonvisual_reading_no_metadata_descriptive" to
        "No information about nonvisual reading is available",
    "ways_of_reading_nonvisual_reading_none_compact" to
        "Not readable in read aloud or dynamic braille",
    "ways_of_reading_nonvisual_reading_none_descriptive" to
        "The content is not readable as read aloud speech or dynamic braille",
    "ways_of_reading_nonvisual_reading_not_fully_compact" to
        "Not fully readable in read aloud or dynamic braille",
    "ways_of_reading_nonvisual_reading_not_fully_descriptive" to
        "Not all of the content will be readable as read aloud speech or dynamic braille",
    "ways_of_reading_nonvisual_reading_readable_compact" to
        "Readable in read aloud or dynamic braille",
    "ways_of_reading_nonvisual_reading_readable_descriptive" to
        "All content can be read as read aloud speech or dynamic braille",
    "ways_of_reading_prerecorded_audio_complementary_compact" to
        "Prerecorded audio clips",
    "ways_of_reading_prerecorded_audio_complementary_descriptive" to
        "Prerecorded audio clips are embedded in the content",
    "ways_of_reading_prerecorded_audio_no_metadata_compact" to
        "No information about prerecorded audio is available",
    "ways_of_reading_prerecorded_audio_no_metadata_descriptive" to
        "No information about prerecorded audio is available",
    "ways_of_reading_prerecorded_audio_only_compact" to
        "Prerecorded audio only",
    "ways_of_reading_prerecorded_audio_only_descriptive" to
        "Audiobook with no text alternative",
    "ways_of_reading_prerecorded_audio_synchronized_compact" to
        "Prerecorded audio synchronized with text",
    "ways_of_reading_prerecorded_audio_synchronized_descriptive" to
        "All the content is available as prerecorded audio synchronized with text",
    "ways_of_reading_visual_adjustments_modifiable_compact" to
        "Appearance can be modified",
    "ways_of_reading_visual_adjustments_modifiable_descriptive" to
        "Appearance of the text and page layout can be modified according to the capabilities of the reading system (font family and font size, spaces between paragraphs, sentences, words, and letters, as well as color of background and text)",
    "ways_of_reading_visual_adjustments_unknown_compact" to
        "No information about appearance modifiability is available",
    "ways_of_reading_visual_adjustments_unknown_descriptive" to
        "No information about appearance modifiability is available",
    "ways_of_reading_visual_adjustments_unmodifiable_compact" to
        "Appearance cannot be modified",
    "ways_of_reading_visual_adjustments_unmodifiable_descriptive" to
        "Text and page layout cannot be modified as the reading experience is close to a print version, but reading systems can still provide zooming options",
    "conformance_title" to
        "Conformance",
    "conformance_details_title" to
        "Detailed conformance information",
    "conformance_a_compact" to
        "This publication meets minimum accessibility standards",
    "conformance_a_descriptive" to
        "The publication contains a conformance statement that it meets the EPUB Accessibility and WCAG 2 Level A standard",
    "conformance_aa_compact" to
        "This publication meets accepted accessibility standards",
    "conformance_aa_descriptive" to
        "The publication contains a conformance statement that it meets the EPUB Accessibility and WCAG 2 Level AA standard",
    "conformance_aaa_compact" to
        "This publication exceeds accepted accessibility standards",
    "conformance_aaa_descriptive" to
        "The publication contains a conformance statement that it meets the EPUB Accessibility and WCAG 2 Level AAA standard",
    "conformance_certifier_compact" to
        "The publication was certified by ",
    "conformance_certifier_descriptive" to
        "The publication was certified by ",
    "conformance_certifier_credentials_compact" to
        "The certifier's credential is ",
    "conformance_certifier_credentials_descriptive" to
        "The certifier's credential is ",
    "conformance_details_certification_info_compact" to
        "The publication was certified on ",
    "conformance_details_certification_info_descriptive" to
        "The publication was certified on ",
    "conformance_details_certifier_report_compact" to
        "For more information refer to the certifier's report",
    "conformance_details_certifier_report_descriptive" to
        "For more information refer to the certifier's report",
    "conformance_details_claim_compact" to
        "This publication claims to meet",
    "conformance_details_claim_descriptive" to
        "This publication claims to meet",
    "conformance_details_epub_accessibility_1_0_compact" to
        " EPUB Accessibility 1.0",
    "conformance_details_epub_accessibility_1_0_descriptive" to
        " EPUB Accessibility 1.0",
    "conformance_details_epub_accessibility_1_1_compact" to
        " EPUB Accessibility 1.1",
    "conformance_details_epub_accessibility_1_1_descriptive" to
        " EPUB Accessibility 1.1",
    "conformance_details_level_a_compact" to
        " Level A",
    "conformance_details_level_a_descriptive" to
        " Level A",
    "conformance_details_level_aa_compact" to
        " Level AA",
    "conformance_details_level_aa_descriptive" to
        " Level AA",
    "conformance_details_level_aaa_compact" to
        " Level AAA",
    "conformance_details_level_aaa_descriptive" to
        " Level AAA",
    "conformance_details_wcag_2_0_compact" to
        " WCAG 2.0",
    "conformance_details_wcag_2_0_descriptive" to
        " Web Content Accessibility Guidelines (WCAG) 2.0",
    "conformance_details_wcag_2_1_compact" to
        " WCAG 2.1",
    "conformance_details_wcag_2_1_descriptive" to
        " Web Content Accessibility Guidelines (WCAG) 2.1",
    "conformance_details_wcag_2_2_compact" to
        " WCAG 2.2",
    "conformance_details_wcag_2_2_descriptive" to
        " Web Content Accessibility Guidelines (WCAG) 2.2",
    "conformance_no_compact" to
        "No information is available",
    "conformance_no_descriptive" to
        "No information is available",
    "conformance_unknown_standard_compact" to
        "Conformance to accepted standards for accessibility of this publication cannot be determined",
    "conformance_unknown_standard_descriptive" to
        "Conformance to accepted standards for accessibility of this publication cannot be determined",
    "navigation_title" to
        "Navigation",
    "navigation_index_compact" to
        "Index",
    "navigation_index_descriptive" to
        "Index with links to referenced entries",
    "navigation_no_metadata_compact" to
        "No information is available",
    "navigation_no_metadata_descriptive" to
        "No information is available",
    "navigation_page_navigation_compact" to
        "Go to page",
    "navigation_page_navigation_descriptive" to
        "Page list to go to pages from the print source version",
    "navigation_structural_compact" to
        "Headings",
    "navigation_structural_descriptive" to
        "Elements such as headings, tables, etc for structured navigation",
    "navigation_toc_compact" to
        "Table of contents",
    "navigation_toc_descriptive" to
        "Table of contents to all chapters of the text via links",
    "rich_content_title" to
        "Rich content",
    "rich_content_accessible_chemistry_as_latex_compact" to
        "Chemical formulas in LaTeX",
    "rich_content_accessible_chemistry_as_latex_descriptive" to
        "Chemical formulas in accessible format (LaTeX)",
    "rich_content_accessible_chemistry_as_mathml_compact" to
        "Chemical formulas in MathML",
    "rich_content_accessible_chemistry_as_mathml_descriptive" to
        "Chemical formulas in accessible format (MathML)",
    "rich_content_accessible_math_as_latex_compact" to
        "Math as LaTeX",
    "rich_content_accessible_math_as_latex_descriptive" to
        "Math formulas in accessible format (LaTeX)",
    "rich_content_accessible_math_as_mathml_compact" to
        "Math as MathML",
    "rich_content_accessible_math_as_mathml_descriptive" to
        "Math formulas in accessible format (MathML)",
    "rich_content_accessible_math_described_compact" to
        "Text descriptions of math are provided",
    "rich_content_accessible_math_described_descriptive" to
        "Text descriptions of math are provided",
    "rich_content_closed_captions_compact" to
        "Videos have closed captions",
    "rich_content_closed_captions_descriptive" to
        "Videos included in publications have closed captions",
    "rich_content_extended_compact" to
        "Information-rich images are described by extended descriptions",
    "rich_content_extended_descriptive" to
        "Information-rich images are described by extended descriptions",
    "rich_content_open_captions_compact" to
        "Videos have open captions",
    "rich_content_open_captions_descriptive" to
        "Videos included in publications have open captions",
    "rich_content_transcript_compact" to
        "Transcript(s) provided",
    "rich_content_transcript_descriptive" to
        "Transcript(s) provided",
    "rich_content_unknown_compact" to
        "No information is available",
    "rich_content_unknown_descriptive" to
        "No information is available",
    "hazards_title" to
        "Hazards",
    "hazards_flashing_compact" to
        "Flashing content",
    "hazards_flashing_descriptive" to
        "The publication contains flashing content that can cause photosensitive seizures",
    "hazards_flashing_none_compact" to
        "No flashing hazards",
    "hazards_flashing_none_descriptive" to
        "The publication does not contain flashing content that can cause photosensitive seizures",
    "hazards_flashing_unknown_compact" to
        "Flashing hazards not known",
    "hazards_flashing_unknown_descriptive" to
        "The presence of flashing content that can cause photosensitive seizures could not be determined",
    "hazards_motion_compact" to
        "Motion simulation",
    "hazards_motion_descriptive" to
        "The publication contains motion simulations that can cause motion sickness",
    "hazards_motion_none_compact" to
        "No motion simulation hazards",
    "hazards_motion_none_descriptive" to
        "The publication does not contain motion simulations that can cause motion sickness",
    "hazards_motion_unknown_compact" to
        "Motion simulation hazards not known",
    "hazards_motion_unknown_descriptive" to
        "The presence of motion simulations that can cause motion sickness could not be determined",
    "hazards_no_metadata_compact" to
        "No information is available",
    "hazards_no_metadata_descriptive" to
        "No information is available",
    "hazards_none_compact" to
        "No hazards",
    "hazards_none_descriptive" to
        "The publication contains no hazards",
    "hazards_sound_compact" to
        "Sounds",
    "hazards_sound_descriptive" to
        "The publication contains sounds that can cause sensitivity issues",
    "hazards_sound_none_compact" to
        "No sound hazards",
    "hazards_sound_none_descriptive" to
        "The publication does not contain sounds that can cause sensitivity issues",
    "hazards_sound_unknown_compact" to
        "Sound hazards not known",
    "hazards_sound_unknown_descriptive" to
        "The presence of sounds that can cause sensitivity issues could not be determined",
    "hazards_unknown_compact" to
        "The presence of hazards is unknown",
    "hazards_unknown_descriptive" to
        "The presence of hazards is unknown",
    "accessibility_summary_title" to
        "Accessibility summary",
    "accessibility_summary_no_metadata_compact" to
        "No information is available",
    "accessibility_summary_no_metadata_descriptive" to
        "No information is available",
    "accessibility_summary_publisher_contact_compact" to
        "For more information about the accessibility of this product, please contact the publisher: ",
    "accessibility_summary_publisher_contact_descriptive" to
        "For more information about the accessibility of this product, please contact the publisher: ",
    "legal_considerations_title" to
        "Legal considerations",
    "legal_considerations_exempt_compact" to
        "Claims an accessibility exemption in some jurisdictions",
    "legal_considerations_exempt_descriptive" to
        "This publication claims an accessibility exemption in some jurisdictions",
    "legal_considerations_no_metadata_compact" to
        "No information is available",
    "legal_considerations_no_metadata_descriptive" to
        "No information is available",
    "additional_accessibility_information_title" to
        "Additional accessibility information",
    "additional_accessibility_information_aria_compact" to
        "ARIA roles included",
    "additional_accessibility_information_aria_descriptive" to
        "Content is enhanced with ARIA roles to optimize organization and facilitate navigation",
    "additional_accessibility_information_audio_descriptions_compact" to
        "Audio descriptions",
    "additional_accessibility_information_audio_descriptions_descriptive" to
        "Audio descriptions",
    "additional_accessibility_information_braille_compact" to
        "Braille",
    "additional_accessibility_information_braille_descriptive" to
        "Braille",
    "additional_accessibility_information_color_not_sole_means_of_conveying_information_compact" to
        "Color is not the sole means of conveying information",
    "additional_accessibility_information_color_not_sole_means_of_conveying_information_descriptive" to
        "Color is not the sole means of conveying information",
    "additional_accessibility_information_dyslexia_readability_compact" to
        "Dyslexia readability",
    "additional_accessibility_information_dyslexia_readability_descriptive" to
        "Dyslexia readability",
    "additional_accessibility_information_full_ruby_annotations_compact" to
        "Full ruby annotations",
    "additional_accessibility_information_full_ruby_annotations_descriptive" to
        "Full ruby annotations",
    "additional_accessibility_information_high_contrast_between_foreground_and_background_audio_compact" to
        "High contrast between foreground and background audio",
    "additional_accessibility_information_high_contrast_between_foreground_and_background_audio_descriptive" to
        "High contrast between foreground and background audio",
    "additional_accessibility_information_high_contrast_between_text_and_background_compact" to
        "High contrast between foreground text and background",
    "additional_accessibility_information_high_contrast_between_text_and_background_descriptive" to
        "High contrast between foreground text and background",
    "additional_accessibility_information_large_print_compact" to
        "Large print",
    "additional_accessibility_information_large_print_descriptive" to
        "Large print",
    "additional_accessibility_information_page_breaks_compact" to
        "Page breaks included",
    "additional_accessibility_information_page_breaks_descriptive" to
        "Page breaks included from the original print source",
    "additional_accessibility_information_ruby_annotations_compact" to
        "Some Ruby annotations",
    "additional_accessibility_information_ruby_annotations_descriptive" to
        "Some Ruby annotations",
    "additional_accessibility_information_sign_language_compact" to
        "Sign language",
    "additional_accessibility_information_sign_language_descriptive" to
        "Sign language",
    "additional_accessibility_information_tactile_graphics_compact" to
        "Tactile graphics included",
    "additional_accessibility_information_tactile_graphics_descriptive" to
        "Tactile graphics have been integrated to facilitate access to visual elements for blind people",
    "additional_accessibility_information_tactile_objects_compact" to
        "Tactile 3D objects",
    "additional_accessibility_information_tactile_objects_descriptive" to
        "Tactile 3D objects",
    "additional_accessibility_information_text_to_speech_hinting_compact" to
        "Text-to-speech hinting provided",
    "additional_accessibility_information_text_to_speech_hinting_descriptive" to
        "Text-to-speech hinting provided",
    "additional_accessibility_information_ultra_high_contrast_between_text_and_background_compact" to
        "Ultra high contrast between text and background",
    "additional_accessibility_information_ultra_high_contrast_between_text_and_background_descriptive" to
        "Ultra high contrast between text and background",
    "additional_accessibility_information_visible_page_numbering_compact" to
        "Visible page numbering",
    "additional_accessibility_information_visible_page_numbering_descriptive" to
        "Visible page numbering",
    "additional_accessibility_information_without_background_sounds_compact" to
        "Without background sounds",
    "additional_accessibility_information_without_background_sounds_descriptive" to
        "Without background sounds",
)
