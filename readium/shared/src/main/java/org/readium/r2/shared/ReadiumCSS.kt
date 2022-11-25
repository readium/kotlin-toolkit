@file:Suppress("DEPRECATION")

package org.readium.r2.shared

@Deprecated("Migrate to the new Settings API (see migration guide)")
const val FONT_SIZE_REF = "fontSize"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val FONT_FAMILY_REF = "fontFamily"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val FONT_OVERRIDE_REF = "fontOverride"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val APPEARANCE_REF = "appearance"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val SCROLL_REF = "scroll"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val PUBLISHER_DEFAULT_REF = "advancedSettings"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val TEXT_ALIGNMENT_REF = "textAlign"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val COLUMN_COUNT_REF = "colCount"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val WORD_SPACING_REF = "wordSpacing"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val LETTER_SPACING_REF = "letterSpacing"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val PAGE_MARGINS_REF = "pageMargins"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val LINE_HEIGHT_REF = "lineHeight"

@Deprecated("Migrate to the new Settings API (see migration guide)")
const val FONT_SIZE_NAME = "--USER__$FONT_SIZE_REF"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val FONT_FAMILY_NAME = "--USER__$FONT_FAMILY_REF"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val FONT_OVERRIDE_NAME = "--USER__$FONT_OVERRIDE_REF"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val APPEARANCE_NAME = "--USER__$APPEARANCE_REF"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val SCROLL_NAME = "--USER__$SCROLL_REF"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val PUBLISHER_DEFAULT_NAME = "--USER__$PUBLISHER_DEFAULT_REF"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val TEXT_ALIGNMENT_NAME = "--USER__$TEXT_ALIGNMENT_REF"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val COLUMN_COUNT_NAME = "--USER__$COLUMN_COUNT_REF"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val WORD_SPACING_NAME = "--USER__$WORD_SPACING_REF"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val LETTER_SPACING_NAME = "--USER__$LETTER_SPACING_REF"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val PAGE_MARGINS_NAME = "--USER__$PAGE_MARGINS_REF"
@Deprecated("Migrate to the new Settings API (see migration guide)")
const val LINE_HEIGHT_NAME = "--USER__$LINE_HEIGHT_REF"

// List of strings that can identify the name of a CSS custom property
// Also used for storing UserSettings in UserDefaults
@Deprecated("Migrate to the new Settings API (see migration guide)")
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

    companion object {
        fun ref(name: String): ReadiumCSSName = valueOf(name)
    }
}
