package org.readium.r2.shared

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val FONT_SIZE_REF: String = "fontSize"

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val FONT_FAMILY_REF: String = "fontFamily"

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val FONT_OVERRIDE_REF: String = "fontOverride"

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val APPEARANCE_REF: String = "appearance"

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val SCROLL_REF: String = "scroll"

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val PUBLISHER_DEFAULT_REF: String = "advancedSettings"

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val TEXT_ALIGNMENT_REF: String = "textAlign"

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val COLUMN_COUNT_REF: String = "colCount"

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val WORD_SPACING_REF: String = "wordSpacing"

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val LETTER_SPACING_REF: String = "letterSpacing"

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val PAGE_MARGINS_REF: String = "pageMargins"

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val LINE_HEIGHT_REF: String = "lineHeight"

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val FONT_SIZE_NAME: String = ""

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val FONT_FAMILY_NAME: String = ""

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val FONT_OVERRIDE_NAME: String = ""

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val APPEARANCE_NAME: String = ""

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val SCROLL_NAME: String = ""

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val PUBLISHER_DEFAULT_NAME: String = ""

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val TEXT_ALIGNMENT_NAME: String = ""

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val COLUMN_COUNT_NAME: String = ""

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val WORD_SPACING_NAME: String = ""

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val LETTER_SPACING_NAME: String = ""

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val PAGE_MARGINS_NAME: String = ""

@Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
public const val LINE_HEIGHT_NAME: String = ""

// List of strings that can identify the name of a CSS custom property
// Also used for storing UserSettings in UserDefaults
@Deprecated(
    "Migrate to the new Settings API (see migration guide)",
    level = DeprecationLevel.WARNING
)
public enum class ReadiumCSSName(public val ref: String)
