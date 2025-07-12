/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.common

import kotlinx.collections.immutable.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Url

public data class FontFamilyDeclarations(
    val declarations: PersistentList<FontFamilyDeclaration>,
) {

    public companion object {

        public operator fun invoke(
            block: MutableFontFamilyDeclarations.() -> Unit,
        ): FontFamilyDeclarations =
            MutableFontFamilyDeclarations()
                .apply { block() }
                .toFontFamilyDeclarations()
    }
}

public class MutableFontFamilyDeclarations(
    private var declarations: PersistentList<FontFamilyDeclaration> = persistentListOf(),
) {
    /**
     * Adds a declaration for [fontFamily] using [builderAction].
     *
     * @param alternates Specifies a list of alternative font families used as fallbacks when
     * symbols are missing from [fontFamily].
     */
    @ExperimentalReadiumApi
    public fun addFontFamilyDeclaration(
        fontFamily: FontFamily,
        alternates: ImmutableList<FontFamily> = persistentListOf(),
        builderAction: (MutableFontFamilyDeclaration).() -> Unit,
    ) {
        declarations += MutableFontFamilyDeclaration(
            fontFamily = fontFamily.name,
            alternates = alternates.map { it.name }.toImmutableList()
        ).apply(builderAction)
            .toFontFamilyDeclaration()
    }

    public fun toFontFamilyDeclarations(): FontFamilyDeclarations =
        FontFamilyDeclarations(declarations)
}

/**
 * A font family declaration.
 *
 * @param alternates Specifies a list of alternative font families used as fallbacks when symbols
 * are missing from [fontFamily].
 */
public data class FontFamilyDeclaration(
    val fontFamily: String,
    val alternates: ImmutableList<String>,
    val fontFaces: ImmutableList<FontFaceDeclaration>,
) {

    public companion object {

        public operator fun invoke(
            fontFamily: String,
            alternates: ImmutableList<String>,
            block: MutableFontFamilyDeclaration.() -> Unit,
        ): FontFamilyDeclaration =
            MutableFontFamilyDeclaration(fontFamily, alternates)
                .apply { block() }
                .toFontFamilyDeclaration()
    }
}

/**
 * A mutable font family declaration.
 */
@ExperimentalReadiumApi
@ConsistentCopyVisibility
public data class MutableFontFamilyDeclaration internal constructor(
    private val fontFamily: String,
    private val alternates: ImmutableList<String>,
    private var fontFaces: PersistentList<FontFaceDeclaration> = persistentListOf(),
) {

    public fun addFontFace(builderAction: MutableFontFaceDeclaration.() -> Unit) {
        val fontFace = MutableFontFaceDeclaration(fontFamily).apply(builderAction)
        fontFaces += fontFace.toFontFaceDeclaration()
    }

    internal fun toFontFamilyDeclaration(): FontFamilyDeclaration {
        check(fontFaces.isNotEmpty())
        return FontFamilyDeclaration(fontFamily, alternates, fontFaces)
    }
}

/**
 * An immutable font face declaration.
 */
public data class FontFaceDeclaration(
    val fontFamily: String,
    val sources: ImmutableList<FontFaceSource>,
    val fontStyle: FontStyle? = null,
    val fontWeight: Either<FontWeight, ClosedRange<Int>>? = null,
)

/**
 * A mutable font face declaration.
 */
@ExperimentalReadiumApi
@ConsistentCopyVisibility
public data class MutableFontFaceDeclaration internal constructor(
    private val fontFamily: String,
    private var sources: PersistentList<FontFaceSource> = persistentListOf(),
    private var fontStyle: FontStyle? = null,
    private var fontWeight: Either<FontWeight, ClosedRange<Int>>? = null,
) {
    /**
     * Add a source for the font face.
     *
     * @param path Path to the font file.
     * @param preload Indicates whether this source will be declared for preloading in the HTML
     * using `<link rel="preload">`.
     */
    public fun addSource(path: String, preload: Boolean = false) {
        val url = requireNotNull(Url.fromDecodedPath(path)) {
            "Invalid font path: $path"
        }
        addSource(url, preload = preload)
    }

    /**
     * Add a source for the font face.
     *
     * @param preload Indicates whether this source will be declared for preloading in the HTML
     * using `<link rel="preload">`.
     */
    public fun addSource(href: Url, preload: Boolean = false) {
        this.sources += FontFaceSource(href = href, preload = preload)
    }

    /**
     * Set the font style of the font face.
     */
    public fun setFontStyle(fontStyle: FontStyle) {
        this.fontStyle = fontStyle
    }

    /**
     * Set the font weight of the font face.
     */
    public fun setFontWeight(fontWeight: FontWeight) {
        this.fontWeight = Either(fontWeight)
    }

    /**
     * Set the font weight range of a variable font face.
     */
    public fun setFontWeight(range: ClosedRange<Int>) {
        require(range.start >= 1)
        require(range.endInclusive <= 1000)
        this.fontWeight = Either(range)
    }

    internal fun toFontFaceDeclaration() =
        FontFaceDeclaration(fontFamily, sources, fontStyle, fontWeight)
}

/**
 * Represents an individual font file.
 *
 * @param preload Indicates whether this source will be declared for preloading in the HTML using
 * `<link rel="preload">`.
 */
public data class FontFaceSource(
    val href: Url,
    val preload: Boolean = false,
)

/**
 *  Styles that a font can be styled with.
 */
@ExperimentalReadiumApi
public enum class FontStyle {
    NORMAL,
    ITALIC,
}

/**
 * Weight (or boldness) of a font.
 *
 * See https://developer.mozilla.org/en-US/docs/Web/CSS/@font-face/font-weight#common_weight_name_mapping
 */
@ExperimentalReadiumApi
public enum class FontWeight(public val value: Int) {
    THIN(100),
    EXTRA_LIGHT(200),
    LIGHT(300),
    NORMAL(400),
    MEDIUM(500),
    SEMI_BOLD(600),
    BOLD(700),
    EXTRA_BOLD(800),
    BLACK(900),
}

/**
 * Typeface for a publication's text.
 *
 * For a list of vetted font families, see https://readium.org/readium-css/docs/CSS10-libre_fonts.
 */
@ExperimentalReadiumApi
@JvmInline
@Serializable
public value class FontFamily(public val name: String) {

    public companion object {
        // Generic font families
        // See https://www.w3.org/TR/css-fonts-4/#generic-font-families
        public val SERIF: FontFamily = FontFamily("serif")
        public val SANS_SERIF: FontFamily = FontFamily("sans-serif")
        public val CURSIVE: FontFamily = FontFamily("cursive")
        public val FANTASY: FontFamily = FontFamily("fantasy")
        public val MONOSPACE: FontFamily = FontFamily("monospace")

        // Accessibility fonts embedded with Readium
        public val ACCESSIBLE_DFA: FontFamily = FontFamily("AccessibleDfA")
        public val IA_WRITER_DUOSPACE: FontFamily = FontFamily("IA Writer Duospace")
        public val OPEN_DYSLEXIC: FontFamily = FontFamily("OpenDyslexic")
    }
}
