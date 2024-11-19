/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.css

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Url

/**
 * Build a declaration for [fontFamily] using [builderAction].
 *
 * @param alternates Specifies a list of alternative font families used as fallbacks when symbols
 * are missing from [fontFamily].
 */
@ExperimentalReadiumApi
internal fun buildFontFamilyDeclaration(
    fontFamily: String,
    alternates: List<String>,
    builderAction: (MutableFontFamilyDeclaration).() -> Unit,
) =
    MutableFontFamilyDeclaration(fontFamily, alternates).apply(builderAction).toFontFamilyDeclaration()

/**
 * A font family declaration.
 *
 * @param alternates Specifies a list of alternative font families used as fallbacks when symbols
 * are missing from [fontFamily].
 */
@ExperimentalReadiumApi
internal data class FontFamilyDeclaration(
    val fontFamily: String,
    val alternates: List<String>,
    val fontFaces: List<FontFaceDeclaration>,
)

/**
 * An immutable font face declaration.
 */
@ExperimentalReadiumApi
internal data class FontFaceDeclaration(
    val fontFamily: String,
    val sources: List<FontFaceSource>,
    var fontStyle: FontStyle? = null,
    var fontWeight: Either<FontWeight, ClosedRange<Int>>? = null,
) {

    fun links(urlNormalizer: (Url) -> Url): List<String> =
        sources
            .filter { it.preload }
            .map {
                """<link rel="preload" href="${urlNormalizer(it.href)}" as="font" crossorigin="" />"""
            }

    fun toCss(urlNormalizer: (Url) -> Url): String {
        val descriptors = buildMap {
            set("font-family", """"$fontFamily"""")

            val urls = sources.map { urlNormalizer(it.href) }
            val src = urls.joinToString(", ") { """url("$it")""" }
            set("src", src)

            fontStyle?.let { set("font-style", it.name.lowercase()) }

            fontWeight?.let {
                when (it) {
                    is Either.Left ->
                        set("font-weight", it.value.value)
                    is Either.Right ->
                        set("font-weight", "${it.value.start} ${it.value.endInclusive}")
                }
            }
        }

        val descriptorList = descriptors
            .map { entry -> "${entry.key}: ${entry.value};" }
            .joinToString(" ")

        return "@font-face { $descriptorList }"
    }
}

/**
 * Represents an individual font file.
 *
 * @param preload Indicates whether this source will be declared for preloading in the HTML using
 * `<link rel="preload">`.
 */
internal data class FontFaceSource(
    val href: Url,
    val preload: Boolean = false,
)

/**
 * A mutable font family declaration.
 */
@ExperimentalReadiumApi
public data class MutableFontFamilyDeclaration internal constructor(
    private val fontFamily: String,
    private val alternates: List<String>,
    private val fontFaces: MutableList<FontFaceDeclaration> = mutableListOf(),
) {

    public fun addFontFace(builderAction: MutableFontFaceDeclaration.() -> Unit) {
        val fontFace = MutableFontFaceDeclaration(fontFamily).apply(builderAction)
        fontFaces.add(fontFace.toFontFaceDeclaration())
    }

    internal fun toFontFamilyDeclaration(): FontFamilyDeclaration {
        check(fontFaces.isNotEmpty())
        return FontFamilyDeclaration(fontFamily, alternates, fontFaces)
    }
}

/**
 * A mutable font face declaration.
 */
@ExperimentalReadiumApi
public data class MutableFontFaceDeclaration internal constructor(
    private val fontFamily: String,
    private val sources: MutableList<FontFaceSource> = mutableListOf(),
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
        this.sources.add(FontFaceSource(href = href, preload = preload))
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
