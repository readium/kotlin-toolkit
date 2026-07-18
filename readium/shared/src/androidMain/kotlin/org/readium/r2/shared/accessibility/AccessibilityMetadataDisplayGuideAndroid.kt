/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.accessibility

import android.content.Context
import androidx.annotation.StringRes
import org.readium.r2.shared.R
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.Field
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.Statement

/**
 * Localized title for this display field, for example to use as a section header.
 */
public fun Field.localizedTitle(context: Context): String =
    context.getString(titleResId)

@get:StringRes
private val Field.titleResId: Int get() = when (this) {
    is AccessibilityMetadataDisplayGuide.WaysOfReading -> R.string.readium_a11y_ways_of_reading_title
    is AccessibilityMetadataDisplayGuide.Navigation -> R.string.readium_a11y_navigation_title
    is AccessibilityMetadataDisplayGuide.RichContent -> R.string.readium_a11y_rich_content_title
    is AccessibilityMetadataDisplayGuide.AdditionalInformation -> R.string.readium_a11y_additional_accessibility_information_title
    is AccessibilityMetadataDisplayGuide.Hazards -> R.string.readium_a11y_hazards_title
    is AccessibilityMetadataDisplayGuide.Conformance -> R.string.readium_a11y_conformance_title
    is AccessibilityMetadataDisplayGuide.Legal -> R.string.readium_a11y_legal_considerations_title
    is AccessibilityMetadataDisplayGuide.AccessibilitySummary -> R.string.readium_a11y_accessibility_summary_title
}

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
 * @param descriptive When true, will return the long descriptive statement.
 */
public fun Statement.localizedString(context: Context, descriptive: Boolean): String =
    when (this) {
        is AccessibilityMetadataDisplayGuide.StaticStatement ->
            string.localizedString(context, descriptive = descriptive)
        is AccessibilityMetadataDisplayGuide.DynamicStatement ->
            if (descriptive) descriptiveString else compactString
    }
