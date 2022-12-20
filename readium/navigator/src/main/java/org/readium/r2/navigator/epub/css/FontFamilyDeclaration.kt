/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.css

import org.readium.r2.shared.ExperimentalReadiumApi

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
    builderAction: (MutableFontFamilyDeclaration).() -> Unit
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
    val fontFaces: List<FontFaceDeclaration>
)

/**
 * Build a font face declaration for [fontFamily].
 */
@ExperimentalReadiumApi
internal fun buildFontFaceDeclaration(
    fontFamily: String,
    builderAction: (MutableFontFaceDeclaration).() -> Unit
) =
    MutableFontFaceDeclaration(fontFamily).apply(builderAction).toFontFaceDeclaration()

/**
 * An immutable font face declaration.
 */
@ExperimentalReadiumApi
internal data class FontFaceDeclaration(
    val fontFamily: String,
    val sources: List<FontFaceSource>,
    var fontStyle: FontStyle? = null,
    var fontWeight: FontWeight? = null,
) {

    fun links(urlNormalizer: (String) -> String): List<String> =
        sources
            .filter { it.preload }
            .map {
                """<link rel="preload" href="${urlNormalizer(it.href)}" as="font" crossorigin="" />"""
            }

    fun toCss(urlNormalizer: (String) -> String): String {
        val descriptors = buildMap {
            set("font-family", """"$fontFamily"""")

            val urls = sources.map { urlNormalizer(it.href) }
            val src = urls.joinToString(", ") { """url("$it")""" }
            set("src", src)

            fontStyle?.let { set("font-style", it.name.lowercase()) }

            fontWeight?.let {
                when (it) {
                    is FontWeight.Range ->
                        set("font-weight", "${it.range.start} ${it.range.endInclusive}")
                    is FontWeight.Value ->
                        set("font-weight", it.value)
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
    val href: String,
    val preload: Boolean = false
)

/**
 * A mutable font family declaration.
 */
@ExperimentalReadiumApi
data class MutableFontFamilyDeclaration internal constructor(
    private val fontFamily: String,
    private val alternates: List<String>,
    private val fontFaces: MutableList<FontFaceDeclaration> = mutableListOf()
) {

    fun addFontFace(builderAction: MutableFontFaceDeclaration.() -> Unit) {
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
data class MutableFontFaceDeclaration internal constructor(
    private val fontFamily: String,
    private val sources: MutableList<FontFaceSource> = mutableListOf(),
    private var fontStyle: FontStyle? = null,
    private var fontWeight: FontWeight? = null
) {

    /**
     * Add a source for the font face.
     *
     * @param preload Indicates whether this source will be declared for preloading in the HTML
     * using `<link rel="preload">`.
     */
    fun addSource(href: String, preload: Boolean = false) {
        this.sources.add(FontFaceSource(href = href, preload = preload))
    }

    /**
     * Set the font style of the font face.
     */
    fun setFontStyle(fontStyle: FontStyle) {
        this.fontStyle = fontStyle
    }

    /**
     * Set the font weight of the font face.
     */
    fun setFontWeight(fontWeight: FontWeight) {
        this.fontWeight = fontWeight
    }

    /**
     * Set the font weight range of a variable font face.
     */
    fun setFontWeightRange(range: ClosedRange<Int> = 1..1000) {
        this.fontWeight = FontWeight.Range(range)
    }

    internal fun toFontFaceDeclaration() =
        FontFaceDeclaration(fontFamily, sources, fontStyle, fontWeight)
}

/**
 *  Styles that a font can be styled with.
 */
@ExperimentalReadiumApi
enum class FontStyle {
    NORMAL,
    ITALIC;
}

/**
 * Weight (or boldness) of a font.
 *
 * See https://developer.mozilla.org/en-US/docs/Web/CSS/@font-face/font-weight#common_weight_name_mapping
 */
@ExperimentalReadiumApi
sealed class FontWeight {
    data class Range(val range: ClosedRange<Int> = 1..1000) : FontWeight()
    data class Value(val value: Int) : FontWeight()

    companion object {
        val THIN = Value(100)
        val EXTRA_LIGHT = Value(200)
        val LIGHT = Value(300)
        val NORMAL = Value(400)
        val MEDIUM = Value(500)
        val SEMI_BOLD = Value(600)
        val BOLD = Value(700)
        val EXTRA_BOLD = Value(800)
        val BLACK = Value(900)
    }
}
