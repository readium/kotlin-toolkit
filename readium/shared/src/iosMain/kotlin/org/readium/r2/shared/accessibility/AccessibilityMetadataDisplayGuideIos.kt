/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.accessibility

import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.Field
import org.readium.r2.shared.accessibility.AccessibilityMetadataDisplayGuide.Statement

/**
 * Localized title for this display field, for example to use as a section header.
 *
 * On iOS, only the en-US strings of the W3C display guide are bundled.
 */
public fun Field.localizedTitle(): String =
    requireNotNull(accessibilityDisplayStrings[titleKey])

private val Field.titleKey: String get() = when (this) {
    is AccessibilityMetadataDisplayGuide.WaysOfReading -> "ways_of_reading_title"
    is AccessibilityMetadataDisplayGuide.Navigation -> "navigation_title"
    is AccessibilityMetadataDisplayGuide.RichContent -> "rich_content_title"
    is AccessibilityMetadataDisplayGuide.AdditionalInformation -> "additional_accessibility_information_title"
    is AccessibilityMetadataDisplayGuide.Hazards -> "hazards_title"
    is AccessibilityMetadataDisplayGuide.Conformance -> "conformance_title"
    is AccessibilityMetadataDisplayGuide.Legal -> "legal_considerations_title"
    is AccessibilityMetadataDisplayGuide.AccessibilitySummary -> "accessibility_summary_title"
}

/**
 * A localized representation for this display statement.
 *
 * On iOS, only the en-US strings of the W3C display guide are bundled.
 *
 * @param descriptive When true, will return the long descriptive statement.
 */
public fun Statement.localizedString(descriptive: Boolean): String =
    when (this) {
        is AccessibilityMetadataDisplayGuide.StaticStatement ->
            string.localizedString(descriptive = descriptive)
        is AccessibilityMetadataDisplayGuide.DynamicStatement ->
            if (descriptive) descriptiveString else compactString
    }
