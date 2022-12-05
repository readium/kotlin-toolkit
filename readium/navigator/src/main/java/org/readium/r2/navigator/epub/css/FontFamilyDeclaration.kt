/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.css

import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Build a declaration for [fontFamily] using [builderAction].
 */
@ExperimentalReadiumApi
fun buildFontFamilyDeclaration(
    fontFamily: String,
    builderAction: (MutableFontFamilyDeclaration).() -> Unit
) =
    MutableFontFamilyDeclaration(fontFamily).apply(builderAction).toFontFamilyDeclaration()

/**
 * A font family declaration.
 */
@ExperimentalReadiumApi
data class FontFamilyDeclaration internal constructor(
    internal val fontFamily: String,
    internal val fontFaces: List<FontFaceDeclaration>
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
data class FontFaceDeclaration internal constructor(
    private val fontFamily: String,
    private val sources: List<FontFaceSource>,
    private var fontStyle: FontStyle? = null,
    private var fontWeight: FontWeight? = null,
) {

    internal fun links(urlNormalizer: (String) -> String): List<String> =
        sources
            .filter { it.preload }
            .map {
                """<link rel="preload" href="${urlNormalizer(it.href)}" as="font" crossorigin="" />"""
            }

    internal fun toCss(urlNormalizer: (String) -> String): String {
        val descriptors = buildMap {
            set("font-family", """"$fontFamily"""")

            val urls = sources.map { urlNormalizer(it.href) }
            val src = urls.joinToString(", ") { """url("$it")""" }
            set("src", src)

            fontStyle?.let { set("font-style", it.name.lowercase()) }
            fontWeight?.let { set("font-weight", it.value) }
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
data class FontFaceSource(
    val href: String,
    val preload: Boolean = false
)

/**
 * A mutable font family declaration.
 */
@ExperimentalReadiumApi
data class MutableFontFamilyDeclaration internal constructor(
    private val fontFamily: String,
    private val fontFaces: MutableList<FontFaceDeclaration> = mutableListOf()
) {

    fun addFontFace(builderAction: MutableFontFaceDeclaration.() -> Unit) {
        val fontFace = MutableFontFaceDeclaration(fontFamily).apply(builderAction)
        fontFaces.add(fontFace.toFontFaceDeclaration())
    }

    internal fun toFontFamilyDeclaration(): FontFamilyDeclaration {
        check(fontFaces.isNotEmpty())
        return FontFamilyDeclaration(fontFamily, fontFaces)
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
    private var fontWeight: FontWeight? = null,
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
enum class FontWeight(internal val value: Int) {
    THIN(100),
    EXTRA_LIGHT(200),
    LIGHT(300),
    NORMAL(400),
    MEDIUM(500),
    SEMI_BOLD(600),
    BOLD(700),
    EXTRA_BOLD(800),
    BLACK(900);
}
