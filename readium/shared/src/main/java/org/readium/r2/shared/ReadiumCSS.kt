@file:Suppress("DEPRECATION")

package org.readium.r2.shared

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val FONT_SIZE_REF = "fontSize"
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val FONT_FAMILY_REF = "fontFamily"
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val FONT_OVERRIDE_REF = "fontOverride"
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val APPEARANCE_REF = "appearance"
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val SCROLL_REF = "scroll"
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val PUBLISHER_DEFAULT_REF = "advancedSettings"
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val TEXT_ALIGNMENT_REF = "textAlign"
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val COLUMN_COUNT_REF = "colCount"
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val WORD_SPACING_REF = "wordSpacing"
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val LETTER_SPACING_REF = "letterSpacing"
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val PAGE_MARGINS_REF = "pageMargins"
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val LINE_HEIGHT_REF = "lineHeight"

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val FONT_SIZE_NAME = ""
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val FONT_FAMILY_NAME = ""
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val FONT_OVERRIDE_NAME = ""
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val APPEARANCE_NAME = ""
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val SCROLL_NAME = ""
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val PUBLISHER_DEFAULT_NAME = ""
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val TEXT_ALIGNMENT_NAME = ""
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val COLUMN_COUNT_NAME = ""
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val WORD_SPACING_NAME = ""
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val LETTER_SPACING_NAME = ""
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val PAGE_MARGINS_NAME = ""
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
const val LINE_HEIGHT_NAME = ""

// List of strings that can identify the name of a CSS custom property
// Also used for storing UserSettings in UserDefaults
@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
enum class ReadiumCSSName(val ref: String) {
    fontSize("--USER__fontSize"),
    fontFamily("--USER__fontFamily"),
    fontOverride("--USER__fontOverride"),
    appearance("--USER__appearance"),
    scroll("--USER__scroll"),
    publisherDefault("--USER__advancedSettings"),
    textAlignment("--USER__textAlign"),
    columnCount("--USER__colCount"),
    wordSpacing("--USER__wordSpacing"),
    letterSpacing("--USER__letterSpacing"),
    pageMargins("--USER__pageMargins"),
    lineHeight("--USER__lineHeight"),
    paraIndent("--USER__paraIndent"),
    hyphens("--USER__bodyHyphens"),
    ligatures("--USER__ligatures");
}
