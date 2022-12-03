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
    private val sources: List<String>,
    private var fontStyle: FontStyle? = null,
    private var fontWeight: FontWeight? = null,
) {

    internal fun toCss(urlNormalizer: (String) -> String): String {
        val descriptors = buildMap {
            set("font-family", """"$fontFamily"""")

            val urls = sources.map(urlNormalizer)
            val src = urls.joinToString(", ") { """url("$it")""" }
            set("src", src)

            fontStyle?.let { set("font-style", it.name.lowercase()) }
            fontWeight?.let { set("font-weight", it.name.lowercase()) }
        }

        val descriptorList = descriptors
            .map { entry -> "${entry.key}: ${entry.value};" }
            .joinToString(" ")

        return "@font-face { $descriptorList }"
    }
}

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
    private val sources: MutableList<String> = mutableListOf(),
    private var fontStyle: FontStyle? = null,
    private var fontWeight: FontWeight? = null,
) {

    /**
     * Add a source for the font face.
     */
    fun addSource(url: String) {
        this.sources.add(url)
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
 */
@ExperimentalReadiumApi
enum class FontWeight {
    NORMAL,
    BOLD;
}
